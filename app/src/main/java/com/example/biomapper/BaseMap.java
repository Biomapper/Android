package com.example.biomapper;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * The base map without any additional content.
 * Provides a dynamic (navigable) map that displays custom map tiles.
 */
public class BaseMap extends Fragment
{
    // Permission Variables
    private static final String[] LOCATION_PERMS =
        { Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION };
    private static final int REQUEST_CODE = 700;

    // Primary objects for creating the map and connecting to the main activity
    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    MainActivity mainActivity;
    TileOverlay tileOverlay;
    TileProvider tileProvider;

    // Restrict the zoom level to 5 and 13
    private static final int MIN_ZOOM = 5;
    private static final int MAX_ZOOM = 13;

    // Specifies the size of the map tiles
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;

    // Strings used for building map tile URLs
    private static final String BASE_URL_STRING = "https://ceias.nau.edu/capstone/projects/CS/2022/BioSphere/";
        // originally https://ceias.nau.edu/capstone/projects/CS/2022/BioSphere/gabonChmData/%d/%d/%d.png;
    private static final String CHM_STRING = "gabonChmData/";
    private static final String DEM_STRING = "DEM/";
    private static final String AGB_STRING = "AGB/";
    private static final String TILE_STRING = "%d/%d/%d.png";
    public static String dataTypeValue;
    public static String dataTypeUrlString;

    // Values for determining which tile provider to use
    private static final int CONNECTED_MODE = 100;
    private static final int DOWNLOAD_MODE = 101;
    private static final int OFFLINE_MODE = 102;
    public static int connectivityMode;



    /**
     * Called when the map is created.
     * Ensures that the map data type, connectivity mode, etc. is as specified in the Preferences.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Initialize reference to the Main Activity.
        mainActivity = (MainActivity) getActivity();

        // Set the map data type value according to the Preferences.
        dataTypeValue = PreferenceManager.
                getDefaultSharedPreferences( getContext() ).
                getString( getString( R.string.set_data_type ),"-1");
        dataTypeUrlString = getDataUrlStringFromValue(dataTypeValue);

        // - - - log statements are to be removed before final release - - -
        Log.e(TAG,String.format("Initial Data Type: %s", dataTypeUrlString) );

        // TODO Set the connectivity mode according to the preferences.
        connectivityMode = DOWNLOAD_MODE; //CONNECTED_MODE; // - - - manually set for now - - -
    }



    /**
     * Called after onCreate(). Creates the fragment's view.
     * Readies the map once is becomes available.
     */
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState )
    {
        // Inflate the view.
        View view = inflater.inflate( R.layout.fragment_base_map, container, false );

        // Initialize the map fragment.
        mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById( R.id.base_map );

        // Wait until the map is available to apply changes to it.
        mapFragment.getMapAsync(
            new OnMapReadyCallback()
            {
                /**
                 * From Google:
                 * Manipulates the map once available.
                 * This callback is triggered when the map is ready to be used.
                 * This is where we can add markers  or lines, add listeners or move the camera.
                 * If Google Play services is not installed on the device, the user will be prompted to install
                 * it inside the SupportMapFragment. This method will only be triggered once the user has
                 * installed Google Play services and returned to the app.
                 */
                @Override
                public void onMapReady( @NonNull GoogleMap googleMap )
                {
                    setUpMap( googleMap );
                }
            }
        );

        // Return the view.
        return view;
    }



    /**
     * Sets up the map after it becomes available and the view has been created.
     * Adds the desired functionality to the map.
     */
    public void setUpMap( @NonNull GoogleMap googleMap )
    {
        // Initialize map variable.
        mMap = googleMap;

        // Set the map type so that Google's typical map tiles are not displayed.
        // - - - This will be uncommented once we have enough real data - - -
        //mMap.setMapType( GoogleMap.MAP_TYPE_NONE );

        // Set the zoom limitations.
        mMap.setMinZoomPreference(MIN_ZOOM);
        mMap.setMaxZoomPreference(MAX_ZOOM);

        // Get location permission.
        if( PackageManager.PERMISSION_GRANTED !=
                getContext().checkCallingOrSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) )
        {
            ActivityCompat.requestPermissions( (MainActivity) getActivity(), LOCATION_PERMS, REQUEST_CODE );
        }
        // Activate the use of the "set my location" button.
        // - - - needs to be set to true eventually - - -
        mMap.setMyLocationEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Create the standard options to be used by map markers.
        MarkerOptions clickedLocationMarkerOptions = new MarkerOptions()
                .icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_GREEN ) );
        MarkerOptions regionOfInterestMarkerOptions = new MarkerOptions()
                .icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_AZURE ) );

        // Add data tiles to the map.
        addTileOverlay();

        // Add a marker in Libreville.
        LatLng libreville = new LatLng( 0.4162, 9.4673 );
        mMap.addMarker( regionOfInterestMarkerOptions.position(libreville).title("Marker in Libreville") );

        // Move camera to Libreville marker.
        CameraUpdate upd = CameraUpdateFactory.newLatLngZoom( libreville, 6 );
        mMap.moveCamera( upd );
    }



    /**
     * Checks that the tile server supports the requested x, y and zoom.
     */
    private boolean checkTileExists( int x, int y, int zoom )
    {
        return ( zoom >= MIN_ZOOM && zoom <= MAX_ZOOM );
    }



    /**
     * Checks if the given URL is valid and links to a resource.
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
        }

        return HttpURLConnection.HTTP_OK == responseCode;
    }



    /**
     * Checks if a file of the given name exists in internal storage.
     */
    public boolean fileExists( String fileName )
    {
        // - - - Log statements are to be removed before release - - -
        Log.e(TAG, "Checking if File Exists.");
        File file = getContext().getFileStreamPath( fileName );
        return file.exists();
    }



    /**
     * Retrieves and returns a bitmap image from the given URL.
     */
    private Bitmap getBitmapFromUrl( String urlString )
    {
        try
        {
            URL url = new URL( urlString );
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
     * Returns the data type URL string component associated
     * with the given data type list preference value.
     */
    public String getDataUrlStringFromValue( String value )
    {
        if( value.equals("canopy_height_model") )
        {
            return CHM_STRING;
        }
        else if( value.equals("digital_elevation_model") )
        {
            return DEM_STRING;
        }
        else if( value.equals("above_ground_biomass") )
        {
            return AGB_STRING;
        }
        return "-1";
    }



    /**
     * Returns a complete URL for a tile of the current data type and
     * for the given zoom level, x coordinate, and y coordinate.
     */
    public String getTileUrlString( int zoom, int x, int y )
    {
        return String.format(BASE_URL_STRING + dataTypeUrlString + TILE_STRING, zoom, x, y);
    }



    /**
     * Creates the appropriate tile provider based on the connectivity mode
     * before adding adding a tile overlay to the map.
     */
    public void addTileOverlay()
    {
        // Determine which tile provider should be used.
        if( connectivityMode == CONNECTED_MODE )
        {
            tileProvider = new ConnectedTileProvider();
        }
        else if( connectivityMode == DOWNLOAD_MODE )
        {
            tileProvider = new DownloadTileProvider();
        }
        else if( connectivityMode == OFFLINE_MODE )
        {
            tileProvider = new OfflineTileProvider();
        }
        else
        {
            throw new IllegalArgumentException();
        }

        // Add a til overlay to the map.
        tileOverlay = mMap.addTileOverlay( new TileOverlayOptions()
                .tileProvider( tileProvider ) );
    }



    /**
     * Removes the current tile overlay from the map.
     */
    public void removeTileOverlay()
    {
        // Make the overlay invisible
        tileOverlay.setVisible(false);

        // Remove the tile overlay from the map.
        tileOverlay.remove();
    }



    /**
     * Custom tile provider class to be used when online.
     */
    private class ConnectedTileProvider extends UrlTileProvider
    {
        public ConnectedTileProvider()
        {
            super( TILE_WIDTH, TILE_HEIGHT );
        }

        /**
         * Gets the URL for the desired tile.
         */
        @Override
        public synchronized URL getTileUrl( int x, int y, int zoom )
        {
            if( !checkTileExists( x, y, zoom ) )
            {
                return null;
            }

            // Modify the y tile coordinate to convert from TMS to XYZ tiles.
            // This is necessary because Google Maps uses XYZ standard tiles
            // but stored data tiles are of the TMS standard.
            // - - - This should be able to be removed after we can tile to XYZ coordinates - - -
            y = ( 1 << zoom ) - y - 1;

            // Build the URL of the map tile based on its zoom, x coordinate, and y coordinate
            String urlStr = getTileUrlString( zoom, x, y );

            try
            {
                return new URL( urlStr );
            }
            catch(MalformedURLException e)
            {
                throw new AssertionError(e);
            }
        }
    }



    /**
     * Custom tile provider class to be used when downloading tiles.
     */
    private class DownloadTileProvider extends UrlTileProvider
    {
        public DownloadTileProvider()
        {
            super( TILE_WIDTH, TILE_HEIGHT );
        }

        /**
         * Gets the URL for the desired tile and downloads the tile
         * it links to if there is one.
         */
        @Override
        public synchronized URL getTileUrl( int x, int y, int zoom )
        {
            String dataType = "CHM";

            if( !checkTileExists( x, y, zoom ) )
            {
                return null;
            }

            // Modify the y tile coordinate to convert from TMS to XYZ tiles.
            // This is necessary because Google Maps uses XYZ standard tiles
            // but stored data tiles are of the TMS standard.
            y = ( 1 << zoom ) - y - 1;

            // Build the URL of the map tile based on its zoom, x coordinate, and y coordinate
            String urlString = getTileUrlString( zoom, x, y );

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
                Bitmap mapTile = getBitmapFromUrl( urlString );

                // Save the image to internal storage
                String fileLoc = saveToInternalStorage( mapTile, dataType, zoom, x, y );

                // Create the directory where the tile should be stored
                String dir = String.format( "%s-%d-%d/%d.png", dataType, zoom, x, y );

                /*
                // - - - BROKEN SECTION - - -
                if( fileExists( dir ) )
                {
                    // - - - Log statements are to be removed before release - - -
                    Log.e( TAG, "File Exists!" );
                }
                else
                {
                    // - - - Log statements are to be removed before release - - -
                    Log.e( TAG, "File Does Not Exist!" );
                } */

                // - - - Log statements are to be removed before release - - -
                Log.e( TAG, "Code passed the fileExists() function" );

                // return URL to get the tile for displaying on map
                return url;
            }
            else
            {
                // - - - Log statements to be removed before release - - -
                Log.e(TAG, "No data found at URL.");
            }

            return null;
        }

    }



    /**
     * TODO: Tile provider for loading tiles that are stored on the device.
     * Custom tile provider class to be used when offline.
     */
    private class OfflineTileProvider implements TileProvider
    {
        /**
         * - - - Function to be completed later - - -
         * Gets the desired tile from internal storage.
         */
        @Nullable
        @Override
        public Tile getTile( int x, int y, int zoom )
        {
            if( !checkTileExists( x, y, zoom ) )
            {
                return NO_TILE;
            }

            return NO_TILE;
        }
    }


}


