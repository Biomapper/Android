package com.example.biomapper;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Allows the user to add or remove filters for any of the three data types.
 */
public class DownloadManager extends Fragment
{
    // Declare references used to access views, other fragments, and shared preferences.
    private static MainActivity mainActivity;
    private static FragmentManager fragmentManager;
    private static SharedPreferences sharedPreferences;

    // References to UI Views.
    TextView downloadDescription;
    EditText radiusEditText;
    TextView sizeEstimate;
    ImageButton beginDownloadButton;
    ImageButton cancelDownloadButton;



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
     * Also sets the up to toolbar, data type spinner, and the accept/clear filter button.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState )
    {
        // Initialize references to view components.
        downloadDescription = mainActivity.findViewById( R.id.download_description );
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

        // Add functionality to the begin download button.
        beginDownloadButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // TODO Attempt to download region based on ROI and given radius
            }
        });

        // Add functionality to the cancel download button.
        cancelDownloadButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openActionMenu();
            }
        });

    } // End of onViewCreated function.



    /**
     * Downloads a single tile based on the given parameters.
     */
    public void downloadTile( boolean filterSet, String dataTypeUrlString, int x, int y, int zoom )
    {
        // Modify the y tile coordinate to convert from TMS to XYZ tiles.
        // This is necessary because Google Maps uses XYZ standard tiles
        // but stored data tiles are of the TMS standard.
        y = ( 1 << zoom ) - y - 1;

        // Build the URL of the map tile based on its zoom, x coordinate, and y coordinate
        String urlString = mainActivity.baseMap.getTileUrlString( filterSet, dataTypeUrlString, zoom, x, y );

        URL url = null;
        try
        {
            url = new URL( urlString );
        }
        catch( MalformedURLException e )
        {
            throw new AssertionError(e);
        }

        // Check if a tile exists at the URL
        if( checkUrlExists( url ) )
        {
            // Get bitmap image from URL
            Bitmap mapTile = getBitmapFromUrl( url );

            // Save the image to internal storage
            String fileLoc = saveToInternalStorage( mapTile, dataTypeUrlString, zoom, x, y );

            // Create the directory where the tile should be stored.
            // - - - This is currently only saving to a single folder! - - -
            String dir = String.format( "%s-%d-%d/%d.png", dataTypeUrlString, zoom, x, y );

                // - - - BROKEN SECTION - - -
                if( checkFileExists( dir ) )
                {
                    // - - - Log statements are to be removed before release - - -
                    Log.e( TAG, "File Exists!" );
                }
                else
                {
                    // - - - Log statements are to be removed before release - - -
                    Log.e( TAG, "File Does Not Exist!" );
                }

            // - - - Log statements are to be removed before release - - -
            Log.e( TAG, "Code passed the fileExists() function" );
        }
        else
        {
            // - - - Log statements to be removed before release - - -
            Log.e(TAG, "No data found at URL.");
        }

    } // End of Download Tile function.



    /**
     * Checks if the given URL links to a valid resource.
     */
    private boolean checkUrlExists( URL url )
    {
        int responseCode = -1;
        try
        {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("HEAD");
            responseCode = urlConnection.getResponseCode();
        }
        catch( Exception e )
        {
            // - - - Log statements are to be removed before release - - -
            Log.e(TAG, "Error Checking URL.");
            e.printStackTrace();
        }

        return HttpURLConnection.HTTP_OK == responseCode;
    }



    /**
     * Retrieves and returns a bitmap image from the given URL.
     */
    private Bitmap getBitmapFromUrl( URL url )
    {
        try
        {
            Bitmap image = BitmapFactory.decodeStream( url.openConnection().getInputStream() );
            // - - - Log statements are to be removed before release - - -
            Log.e(TAG, "Bitmap Retrieved From URL.");
            return image;
        }
        catch( Exception e )
        {
            // - - - Log statements are to be removed before release - - -
            Log.e(TAG, "Error Getting Bitmap from URL.");
        }

        return null;
    }



    /**
     * Saves the given bitmap image to internal storage based on the given metadata.
     * Returns the path to where the image was saved.
     */
    private String saveToInternalStorage( Bitmap bitmapImage, String dataType, int zoom, int x, int y )
    {
        String imageDir = String.format( "%s-%d-%d", dataType, zoom, x );
        String imageName = String.format( "%d.png", y );

        ContextWrapper contextWrapper = new ContextWrapper( getContext() );

        // Create path to /data/data/app_name/app_data/<data_type>/<zoom>/<x>
        File directory = contextWrapper.getDir( imageDir, Context.MODE_PRIVATE );

        // Create image at imageDir (<y>.png).
        File imageFile = new File( directory, imageName );

        FileOutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream( imageFile );

            // Use the compress method on the BitMap object to write image to the OutputStream.
            bitmapImage.compress( Bitmap.CompressFormat.PNG, 100, outputStream );
        }
        catch( Exception e )
        {
            // - - - Log statements are to be removed before release - - -
            Log.e(TAG, "Error Adding Image to Internal Storage.");
            e.printStackTrace();
        }
        finally
        {
            try
            {
                outputStream.flush();
                outputStream.close();
            }
            catch( Exception e )
            {
                // - - - Log statements are to be removed before release - - -
                Log.e(TAG, "Error Closing Output Stream.");
                e.printStackTrace();
            }
        }

        // - - - Log statements are to be removed before release - - -
        Log.w(TAG, "File Created Successfully.");
        return directory.getAbsolutePath();
    }



    /**
     * Checks if a file of the given name exists in internal storage.
     */
    public boolean checkFileExists(String fileName )
    {
        // - - - Log statements are to be removed before release - - -
        Log.e(TAG, "Checking if File Exists.");
        File file = getContext().getFileStreamPath( fileName );
        return file.exists();
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
            fragmentTransaction.remove( fragmentManager.findFragmentByTag( "download_manager" ) );
        }

        fragmentTransaction.commit();
    }


} // End of Filter Manager.
