package com.example.biomapper;

import static android.content.ContentValues.TAG;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Allows the user to download map tiles for offline use.
 */
public class DownloadManager extends Fragment
{
    // Declare references used to access views, other fragments, and shared preferences.
    private static MainActivity mainActivity;
    private static FragmentManager fragmentManager;
    private static SharedPreferences sharedPreferences;

    // References to UI Views.
    TextView downloadDescription;
    CheckBox chmCheckBox;
    CheckBox demCheckBox;
    CheckBox agbCheckBox;
    EditText radiusEditText;
    TextView sizeEstimate;
    ImageButton beginDownloadButton;
    ImageButton cancelDownloadButton;

    private static final String BASE_ROOT_STRING = "http://13.59.201.133/map-tiles/";
    private static final String FILTER_ROOT_STRING = "http://13.59.201.133:3000/base-tiles/";
    private static final String CHM_STRING = "chm/";
    private static final String DEM_STRING = "dem/";
    private static final String AGB_STRING = "agb/";
    private static final int CHM_CODE = 0;
    private static final int DEM_CODE = 1;
    private static final int AGB_CODE = 2;

    // Booleans the specify which data types should be downloaded.
    private static boolean chmSelected;
    private static boolean demSelected;
    private static boolean agbSelected;

    // Radius that specifies the area to be downloaded.
    private static int radius;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 100;

    // Array of maximum values for the data types.
    private static final int[] ABS_MAX_ARRAY = { 45, 1500, 129 };

    // Array of tiles that are to be downloaded.
    private final static int MAX_NUM_OF_TILES = 100;
    int[][] tileValArray;
    int numOfCoveredTiles;

    String[] tileUrlStrArray;
    int numOfTileUrls;

    // Array of values that determine the number of kilometers
    // covered by a single pixel for each  zoom level.
    /*private static double[] PIXEL_SIZES =
            { 156, 78, 39, 20, 10, 4.9, 2.4, 1.2, 0.611,
            0.305, 0.152, 0.076, 0.038, 0.019, 0.0096, 0.0048 };*/


    /**
     * Called when the fragment is being created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );

        // Initialize references to Main Activity, fragment manager, and shared preferences.
        mainActivity = (MainActivity) getActivity();
        fragmentManager = mainActivity.getSupportFragmentManager();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );

        chmSelected = false;
        demSelected = false;
        agbSelected = false;
        radius = 0;

        tileValArray = new int[MAX_NUM_OF_TILES][3];
        numOfCoveredTiles = 0;

        tileUrlStrArray = new String[MAX_NUM_OF_TILES*3];
        numOfTileUrls = 0;
    }



    /**
     * Called after onCreate, creates the fragment's view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate( R.layout.fragment_download_manager, container, false );
        return view;
    }



    /**
     * Called after the view has been created.
     * Initializes references to UI view components.
     * Also sets the up to toolbar, data type selection, and the accept/delete download buttons.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState )
    {
        // Initialize references to view components.
        downloadDescription = mainActivity.findViewById( R.id.download_description );
        chmCheckBox = mainActivity.findViewById( R.id.chm_checkbox );
        demCheckBox = mainActivity.findViewById( R.id.dem_checkbox );
        agbCheckBox = mainActivity.findViewById( R.id.agb_checkbox );
        radiusEditText = mainActivity.findViewById( R.id.radius_edit_text );
        sizeEstimate = mainActivity.findViewById( R.id.size_estimate );
        beginDownloadButton = mainActivity.findViewById( R.id.begin_download );
        cancelDownloadButton = mainActivity.findViewById( R.id.cancel_download );

        // Create the toolbar.
        Toolbar toolbar = (Toolbar) view.findViewById( R.id.download_manager_toolbar );

        // Add a back button to the toolbar and make it functional.
        toolbar.setNavigationIcon( getContext().getDrawable(R.drawable.toolbar_back_icon) );
        toolbar.setNavigationOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick( View view )
                    {
                        openActionMenu();
                    }
                }
        );

        chmCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
                    {
                        // Determine if the data type has been checked or not.
                        chmSelected = chmCheckBox.isChecked();
                    }
                }
        );

        demCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
                    {
                        // Determine if the data type has been checked or not.
                        demSelected = demCheckBox.isChecked();
                    }
                }
        );

        agbCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
                    {
                        // Determine if the data type has been checked or not.
                        agbSelected = agbCheckBox.isChecked();
                    }
                }
        );

        // Add functionality to the begin download button.
        beginDownloadButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        tryDownloadingTiles();
                    }
                }
        );

        // Add functionality to the cancel download button.
        cancelDownloadButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        // Record that changes are being made to the set of downloaded tiles.
                        sharedPreferences.edit().putBoolean( getString( R.string.downloaded_tiles_changed ), true ).commit();

                        // TODO Prompt the user to select which data types they want to delete.
                        // TODO Delete the local tiles for the selected data type(s).
                        Log.e(TAG, "Deleting data..." );

                        // Set that the given data type will now be downloaded.
                        sharedPreferences.edit().putBoolean( getString( R.string.chm_downloaded ), false ).commit();
                        sharedPreferences.edit().putBoolean( getString( R.string.dem_downloaded ), false ).commit();
                        sharedPreferences.edit().putBoolean( getString( R.string.agb_downloaded ), false ).commit();
                        deleteDirectory( getContext().getFilesDir() + "/" + "chm" );
                        deleteDirectory( getContext().getFilesDir() + "/" + "dem" );
                        deleteDirectory( getContext().getFilesDir() + "/" + "agb" );
                    }
                }
        );

    } // End of onViewCreated function.



    /**
     *
     */
    private void tryDownloadingTiles()
    {
        // Get the radius from the EditText.
        String minValString = radiusEditText.getText().toString();
        if( !minValString.equals("") )
        {
            radius = Integer.parseInt( minValString );
        }

        // Check that a region of interest has been set.
        if( sharedPreferences.getBoolean( getString( R.string.roi_set ), false ) )
        {
            // Check that at least one data type has been selected.
            if( chmSelected || demSelected || agbSelected )
            {
                // Check that the radius is in the valid range.
                if( radius >= MIN_RADIUS && radius <= MAX_RADIUS )
                {
                    // Record that changes are being made to the set of downloaded tiles.
                    sharedPreferences.edit().putBoolean( getString( R.string.downloaded_tiles_changed ), true ).commit();

                    numOfTileUrls = 0;
                    numOfCoveredTiles = setCoveredTiles( radius );

                    int numOfDataTypes = 0;

                    int tileUrlIndex = 0;
                    if( chmSelected )
                    {
                        tileUrlIndex = addDateTypeUrls( tileUrlIndex, CHM_CODE, CHM_STRING,
                                getString( R.string.chm_downloaded ), getString( R.string.chm_filter_set ),
                                getString( R.string.chm_filter_min ), getString( R.string.chm_filter_max ) );
                        numOfDataTypes++;
                    }
                    if( demSelected )
                    {
                        tileUrlIndex = addDateTypeUrls( tileUrlIndex, DEM_CODE, DEM_STRING,
                                getString( R.string.dem_downloaded ), getString( R.string.dem_filter_set ),
                                getString( R.string.dem_filter_min ), getString( R.string.dem_filter_max ) );
                        numOfDataTypes++;
                    }
                    if( agbSelected )
                    {
                        tileUrlIndex = addDateTypeUrls( tileUrlIndex, AGB_CODE, AGB_STRING,
                                getString( R.string.agb_downloaded ), getString( R.string.agb_filter_set ),
                                getString( R.string.agb_filter_min ), getString( R.string.agb_filter_max ) );
                        numOfDataTypes++;
                    }
                    numOfTileUrls = numOfCoveredTiles * numOfDataTypes;

                    TileDownloader tileDownloader = new TileDownloader();
                    tileDownloader.execute();
                }
                else
                {
                    // Notify the user that the radius is not in the valid range.
                    Log.e(TAG, "Radius must be between 0 and 100");
                }
            }
            else
            {
                // Notify the user that at least one data type must be selected.
                Log.e(TAG, "At least one data type must be selected");
            }
        }
        else
        {
            // Notify the user that a ROI must be set.
            Log.e(TAG, "A region on interest must be set before downloading.");
        }
    }



    /**
     * Adds the URLs of the specified data type for all the covered tiles
     * to the array of tile URLs to be downloaded.
     */
    public int addDateTypeUrls( int tileUrlIndex, int dataTypeCode, String dataTypeString,
                                String dataDownloadedKey, String filterSetKey,
                                String minKey, String maxKey )
    {
        // If any tiles exist on the local device for the given data type, delete them.
        if( sharedPreferences.getBoolean( dataDownloadedKey, false ) )
        {
            deleteDirectory( getContext().getFilesDir() + "/" + dataTypeString );
        }

        // Set that the given data type will now be downloaded.
        sharedPreferences.edit().putBoolean( dataDownloadedKey, true ).commit();

        // Check if the given data type has a filter applied.
        boolean filterSet = sharedPreferences.getBoolean( filterSetKey, false );
        int[] mappedFilterValues = {};
        if( filterSet )
        {
            // Get the mapped filter values.
            mappedFilterValues = mainActivity.baseMap.calculateMappedFilterValues( minKey, maxKey, ABS_MAX_ARRAY[dataTypeCode] );
        }

        // Add a URL for every covered tile to the list.
        int tileValIndex = 0;
        for( tileValIndex = 0; tileValIndex < numOfCoveredTiles; tileValIndex++ )
        {
            if( filterSet )
            {
                // Add the filtered tile URL to the array of URLs for downloading.
                tileUrlStrArray[tileUrlIndex] = mainActivity.baseMap.getFilteredTileUrlString( dataTypeString,
                        tileValArray[tileValIndex][0], tileValArray[tileValIndex][1], tileValArray[tileValIndex][2],
                        mappedFilterValues[0], mappedFilterValues[1] );
            }
            else
            {
                // Add the regular tile URL to the array of URLs for downloading.
                tileUrlStrArray[tileUrlIndex] = mainActivity.baseMap.getBaseTileUrlString( dataTypeString,
                        tileValArray[tileValIndex][0], tileValArray[tileValIndex][1], tileValArray[tileValIndex][2] );
            }
            tileUrlIndex++;
        }

        return tileUrlIndex;
    }



    /**
     * Estimates the amount of storage space required for downloading.
     * Depends on the number of data types downloaded and the specified radius.
     * The estimate is in megabytes.
     */
    public double estimateDownloadSize( int numOfDataTypes, int radius )
    {
        // Estimate the size for a single data type based on the radius.
        double runningEstimate = radius * 0.05;

        // Multiply this estimate by the number of data types downloaded.
        return runningEstimate * numOfDataTypes;
    }



    /**
     * Determine which tiles are covered by the radius an return the number of these tiles.
     */
    public int setCoveredTiles(int radius )
    {
        final int MIN_ZOOM = mainActivity.baseMap.MIN_ZOOM;
        final int MAX_ZOOM = mainActivity.baseMap.MAX_ZOOM;

        // Latitude: 1 deg = 110.574 km
        final double KM_PER_LAT_DEG = 110.574;
        // Longitude: 1 deg = 111.320 * cos(latitude) km
        final double KM_PER_LNG_DEG = 111.320;
        final double DEG_TO_RADIANS = 0.017453292519943295; //= Math.PI / 180;

        final double CENTER_LAT = (double) sharedPreferences.getFloat( getString(R.string.roi_lat), 0 );
        final double CENTER_LNG = (double) sharedPreferences.getFloat( getString(R.string.roi_lng), 22 );;
        //Log.e(TAG,"Center: " + CENTER_LAT + ", " + CENTER_LNG );

        final double LAT_DEG_RADIUS = radius / KM_PER_LAT_DEG;
        final double LNG_DEG_RADIUS = radius / KM_PER_LNG_DEG * Math.cos( CENTER_LAT * DEG_TO_RADIANS );

        final double NORTH_LAT = CENTER_LAT + LAT_DEG_RADIUS;
        //Log.e(TAG,"North: " + NORTH_LAT + ", " + CENTER_LNG );
        final double SOUTH_LAT = CENTER_LAT - LAT_DEG_RADIUS;
        //Log.e(TAG,"South: " + SOUTH_LAT + ", " + CENTER_LNG );
        final double EAST_LNG = CENTER_LNG + LNG_DEG_RADIUS;
        //Log.e(TAG,"East: " + CENTER_LAT + ", " + EAST_LNG);
        final double WEST_LNG = CENTER_LNG - LNG_DEG_RADIUS;
        //Log.e(TAG,"West: " + CENTER_LAT + ", " + WEST_LNG);

        int northY;
        int southY;
        int eastX;
        int westX;

        int zoomLevel;
        int xCoor = -1;
        int yCoor = -1;

        int tileIndex = 0;
        for( zoomLevel = MIN_ZOOM; zoomLevel <= MAX_ZOOM; zoomLevel++ )
        {
            northY = getTileY( NORTH_LAT, zoomLevel );
            southY = getTileY( SOUTH_LAT, zoomLevel );
            eastX = getTileX( EAST_LNG, zoomLevel );
            westX = getTileX( WEST_LNG, zoomLevel );

            // Transverse down rows of tiles (north to south, descending y).
            for( yCoor = northY; yCoor >= southY; yCoor-- )
            {
                // Traverse across columns of tiles (west to east, ascending x)
                for( xCoor = westX; xCoor <= eastX; xCoor++ )
                {
                    //Log.e(TAG, "zoom: " + zoomLevel + ", x: " + xCoor + ", y: " + yCoor );
                    tileValArray[tileIndex] = new int[] { zoomLevel, xCoor, yCoor };
                    tileIndex++;
                }
            }
        }

        return tileIndex;
    }



    /**
     * Gets the X tile coordinate for a longitude and zoom level.
     */
    private static int getTileX( final double lon, final int zoom )
    {
        int xCoor = (int) Math.floor( (lon + 180) / 360 * (1<<zoom) );

        if( xCoor < 0 )
            xCoor = 0;
        if( xCoor >= (1<<zoom) )
            xCoor = ((1<<zoom)-1);

        return xCoor;
    }



    /**
     * Gets the Y tile coordinate for a latitude and zoom level.
     */
    private static int getTileY( final double lat, final int zoom )
    {
        int yCoor = (int) Math.floor(
                (
                        1 - Math.log(
                                Math.tan( Math.toRadians(lat) )
                                + 1 / Math.cos( Math.toRadians(lat) )
                        ) / Math.PI
                ) / 2 * (1<<zoom)
        );

        if( yCoor < 0 )
            yCoor=0;
        if( yCoor >= (1<<zoom) )
            yCoor=((1<<zoom)-1);

        // Modify the y tile coordinate to convert from XYZ to TMS tiles.
        // This is necessary because this script creates XYS coordinates but our
        // tiles use TMS coordinates.
        // - - - This should be able to be removed after we can tile to XYZ coordinates - - -
        //yCoor = ( 1 << zoom ) - yCoor - 1;
        yCoor = ( 1 << zoom ) + (-1*yCoor) - 1;

        return yCoor;
    }



    /**
     * Tries to delete the specified file.
     */
    public boolean deleteFile(String filePath)
    {
        File file = new File(filePath);
        if( file.isFile() && file.exists() )
        {
            return file.delete();
        }
        return false;
    }



    /**
     * Deletes the specified directory.
     */
    public boolean deleteDirectory( String filePath )
    {
        boolean flag = false;

        // If filePath does not end with a file separator, the file separator is automatically added
        if( !filePath.endsWith( File.separator ) )
        {
            filePath = filePath + File.separator;
        }

        // Create a file object from the given path.
        File dirFile = new File( filePath );
        // Check that the file exists or is a directory.
        if( !dirFile.exists() || !dirFile.isDirectory() )
        {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();

        // Iterate through all files in the deleted folder (including subdirectories)
        for( int index = 0; index < files.length; index++ ) {
            if( files[index].isFile() )
            {
                //  Delete subfiles
                flag = deleteFile( files[index].getAbsolutePath() );
                if (!flag) break;
            }
            else
            {
                //  Delete subdirectory
                flag = deleteDirectory( files[index].getAbsolutePath() );
                if (!flag) break;
            }
        }
        if(!flag)
        {
            return false;
        }

        //  Delete the current empty directory
        return dirFile.delete();
    }



    /**
     * Returns the app to the Action Menu.
     * Shows the Action Menu and removes the Data Filter.
     */
    private void openActionMenu()
    {
        // Close the keyboard, if it's currently open.
        try
        {
            InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService( getContext().INPUT_METHOD_SERVICE );
            imm.hideSoftInputFromWindow( mainActivity.getCurrentFocus().getWindowToken(), 0 );
        }
        catch( Exception e )
        {
            // Keyboard was not already open!
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // If the Action Menu fragment exists, show it.
        if( fragmentManager.findFragmentByTag( "action_menu" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("action_menu") );
        }
        // Else the Action Menu fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new ActionMenu(), "action_menu" );
        }

        // Remove the Data Filter fragment.
        if( fragmentManager.findFragmentByTag( "download_manager" ) != null )
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag( "download_manager" ) );
        }

        fragmentTransaction.commit();
    }



    /**
     * Class that provides asynchronous downloading of map tiles.
     */
    public class TileDownloader extends AsyncTask<String, Void, Bitmap[]>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Bitmap[] doInBackground( String... optionalArgs )
        {
            Log.e(TAG, "Downloading map tiles... Please wait.");
            Bitmap[] tileArray = new Bitmap[numOfTileUrls];

            int index;
            for(index = 0; index < numOfTileUrls; index++ )
            {
                try
                {
                    // Create URL for current tile.
                    URL tileUrl = new URL( tileUrlStrArray[index] );
                    // Connect to the URL.
                    HttpURLConnection urlConnection = (HttpURLConnection) tileUrl.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.connect();
                    // Get the map tile at that URL.
                    InputStream inputStream = urlConnection.getInputStream();
                    tileArray[index] = BitmapFactory.decodeStream( inputStream );
                    // Close the connection.
                    inputStream.close();
                }
                catch( MalformedURLException e )
                {
                    e.printStackTrace();
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }

            return tileArray;
        }

        @Override
        protected void onPostExecute( Bitmap[] tileArray )
        {
            Log.e(TAG, "Saving map tiles to internal storage... Please wait.");
            super.onPostExecute( tileArray );

            int index;
            for(index = 0; index < numOfTileUrls; index++ )
            {
                // Check if the current map tile is filtered.
                if( tileUrlStrArray[index].contains( FILTER_ROOT_STRING ) )
                {
                    String folderNameString = tileUrlStrArray[index].replace( FILTER_ROOT_STRING, "" );
                    String[] pathNames = folderNameString.split( "/" );
                    String folderName = "/" + pathNames[0] + "/" + pathNames[1] + "/" + pathNames[2] + "/";
                    String fileName = pathNames[3] + ".png";
                    saveImagesLocally( tileArray[index], folderName, fileName );
                }
                // Else the current map tile is unfiltered.
                else
                {
                    String folderNameString = tileUrlStrArray[index].replace( BASE_ROOT_STRING, "" );
                    String[] pathNames = folderNameString.split( "/" );
                    String folderName = "/" + pathNames[0] + "/" + pathNames[1] + "/" + pathNames[2] + "/";
                    String fileName = pathNames[3];
                    saveImagesLocally( tileArray[index], folderName, fileName );
                }
            }
            Log.e(TAG, "Downloading and saving complete.");
        }

        /**
         * Save Bitmap to local directory.
         */
        public boolean saveImagesLocally( Bitmap bitmapImage, String folderName, String imgName )
        {
            String folderPath = getContext().getFilesDir() + folderName;
            File file = new File(folderPath);
            if( !file.exists() && !file.mkdirs() )
            {
                // The folder being saved to does not exist.
                return false;
            }
            else
            {
                File saveFile = new File(folderPath, imgName);
                try
                {
                    FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                    bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, saveImgOut);
                    saveImgOut.flush();
                    saveImgOut.close();
                    //Log.e(TAG, "Save Bitmap:" + folderPath + imgName);
                    return true;
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
    } // End of Tile Downloader class.



} // End of Download Manager.
