package com.example.biomapper;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import java.net.MalformedURLException;
import java.net.URL;


/**
 * The base map without any additional content.
 * Provides a dynamic (navigable) map that displays custom map tiles.
 */
public class BaseMap extends Fragment
{
    // Permission Variables.
    private static final String[] LOCATION_PERMS =
        { Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION };
    private static final int REQUEST_CODE = 700;

    // Primary objects for creating the map and connecting to the main activity.
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private MainActivity mainActivity;
    private TileOverlay tileOverlay;
    private TileProvider tileProvider;

    // Restrict the zoom level to 5 and 13.
    // - - - Once a complete set of tiles is added to the server, change to 5 to 13 - - -
    private static final int MIN_ZOOM = 5;
    private static final int MAX_ZOOM = 10;

    // Specifies the size of the map tiles.
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;

    // Values used for setting the map data type.
    private static final int CHM_CODE = 100;
    private static final int DEM_CODE = 101;
    private static final int AGB_CODE = 102;
    private static String dataTypeValue;
    public static int dataTypeCode;

    // Strings used for building map tile URLs.
    // - - - TODO Update tile server IP address - - -
    private static final String BASE_ROOT_STRING = "http://3.16.10.92/base-tiles/";
    private static final String FILTER_ROOT_STRING = "http://3.16.10.92:3000/base-tiles/";

    private static final String CHM_STRING = "chm/";
    private static final String DEM_STRING = "dem/";
    private static final String AGB_STRING = "agb/";
    private static String dataTypeUrlString;

    private static final String BASE_TILE_STRING = "%d/%d/%d.png";
    private static final String FILTER_TILE_STRING = "%d/%d/%d/%d/%d";

    // Values for determining which tile provider to use.
    private static final int CONNECTED_MODE = 200;
    private static final int OFFLINE_MODE = 201;
    public static int connectivityMode;

    // Values for determining if data filter/modified tiles should be used.
    private static final int NO_FILTER_VALUE = -1;
    public static boolean filterSet;
    public static boolean filterChanged;
    public static int minFilterValue;
    public static int maxFilterValue;



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

        // Initialize reference to the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );

        // Set the map data type value according to the preferences.
        dataTypeValue = sharedPreferences
                .getString( getString( R.string.set_data_type ),"-1");
        setDataType( dataTypeValue );

        // Set the data filter according to the preferences.
        if( dataTypeCode == CHM_CODE &&
                sharedPreferences.getBoolean( getString( R.string.chm_filter_set ), false ) )
        {
            setFilterValues( sharedPreferences, getString(R.string.chm_filter_min), getString(R.string.chm_filter_max) );
        }
        else if( dataTypeCode == DEM_CODE &&
                sharedPreferences.getBoolean( getString( R.string.dem_filter_set ), false ) )
        {
            setFilterValues( sharedPreferences, getString(R.string.dem_filter_min), getString(R.string.dem_filter_max) );
        }
        else if( dataTypeCode == AGB_CODE &&
                sharedPreferences.getBoolean( getString( R.string.agb_filter_set ), false ) )
        {
            setFilterValues( sharedPreferences, getString(R.string.agb_filter_min), getString(R.string.agb_filter_max) );
        }
        else
        {
            filterSet = false;
            minFilterValue = NO_FILTER_VALUE;
            maxFilterValue = NO_FILTER_VALUE;
        }
        filterChanged = false;

        // Set the connectivity mode according to the preferences.
        if( sharedPreferences.getBoolean( getString( R.string.enable_offline_mode ), false ) )
        {
            connectivityMode = OFFLINE_MODE;
        }
        else
        {
            connectivityMode = CONNECTED_MODE;
        }
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
                getChildFragmentManager().findFragmentById( R.id.base_map_fragment );

        // Wait until the map is available to apply changes to it.
        mapFragment.getMapAsync(
            new OnMapReadyCallback()
            {
                /**
                 * This callback is triggered when the map is ready to be used.
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
     * This is where we can add markers or lines, add listeners, or move the camera.
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
     * Called when the map is re-opened.
     * Updates map components if any changes were made since the map was last opened.
     */
    public void updateMap()
    {
        // Boolean for determining if the tiles need to be reloaded/updated.
        boolean shouldUpdateTiles = false;

        // Initialize reference to the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );

        // If the map data type has been changed, begin using tiles of that type.
        if( dataTypeValue != sharedPreferences.getString( getString( R.string.set_data_type ),"-1") )
        {
            // Update the map data type accordingly.
            dataTypeValue = sharedPreferences.getString( getString( R.string.set_data_type ),"-1");
            setDataType( dataTypeValue );

            // Reload tiles so they're of the new data type.
            shouldUpdateTiles = true;
        }

        // Apply any changes that might've been made to the filter.
        if( dataTypeCode == CHM_CODE && applyFilterChanges(sharedPreferences, getString(R.string.chm_filter_set),
                getString(R.string.chm_filter_min), getString(R.string.chm_filter_max) ) )
        {
            shouldUpdateTiles = true;
        }
        else if( dataTypeCode == DEM_CODE && applyFilterChanges( sharedPreferences, getString( R.string.dem_filter_set ),
                getString( R.string.dem_filter_min ), getString( R.string.dem_filter_max ) ) )
        {
            shouldUpdateTiles = true;
        }
        else if( dataTypeCode == AGB_CODE && applyFilterChanges( sharedPreferences, getString( R.string.agb_filter_set ),
                getString( R.string.agb_filter_min ), getString( R.string.agb_filter_max ) ) )
        {
            shouldUpdateTiles = true;
        }


        // If currently using online tiles but offline mode has been enabled,
        // begin using offline tiles instead.
        if( connectivityMode == CONNECTED_MODE && sharedPreferences.getBoolean( getString( R.string.enable_offline_mode ), false ) )
        {
            // Update the connectivity mode according to the preferences.
            connectivityMode = OFFLINE_MODE;

            // Reload tiles so the offline ones are used.
            shouldUpdateTiles = true;
        }
        // Else if currently using offline tiles but offline mode has been disabled,
        // begin using online tiles instead.
        else if( connectivityMode == OFFLINE_MODE && !sharedPreferences.getBoolean( getString( R.string.enable_offline_mode ), false ) )
        {
            // Update the connectivity mode according to the preferences.
            connectivityMode = CONNECTED_MODE;

            // Reload tiles so the offline ones are used.
            shouldUpdateTiles = true;
        }

        // If new data type selected, data is filtered, or offline mode changed,
        // the outdated tiles need to be removed and reloaded.
        if( shouldUpdateTiles )
        {
            removeTileOverlay();
            addTileOverlay();
        }

    } // End of Update Map function.



    /**
     * Attempts to apply any changes made to the filter.
     * Returns whether changes were applied or not.
     */
    private boolean applyFilterChanges( SharedPreferences sharedPreferences, String setKey, String minKey, String maxKey )
    {
        // Check if filter set state has been changed.
        if( filterSet != sharedPreferences.getBoolean( setKey, false ) )
        {
            // Update the filter set state.
            filterSet = sharedPreferences.getBoolean( setKey, false );

            // Check if the filter has been set.
            if( filterSet )
            {
                minFilterValue = sharedPreferences.getInt( minKey, NO_FILTER_VALUE );
                maxFilterValue = sharedPreferences.getInt( maxKey, NO_FILTER_VALUE );
            }
            // Else the filter has been removed.
            else
            {
                minFilterValue = NO_FILTER_VALUE;
                maxFilterValue = NO_FILTER_VALUE;
            }

            // Return that changes to the filter were made.
            return true;
        }
        // Else check if filter values have been changed.
        else if(
                minFilterValue != sharedPreferences.getInt( minKey, NO_FILTER_VALUE) ||
                        maxFilterValue != sharedPreferences.getInt( maxKey, NO_FILTER_VALUE )
        )
        {
            // Update filter values.
            minFilterValue = sharedPreferences.getInt( minKey, NO_FILTER_VALUE);
            maxFilterValue = sharedPreferences.getInt( maxKey, NO_FILTER_VALUE);

            // Return that changes to the filter were made.
            return true;
        }

        // Else no changes have been made to the filter.
        return false;
    }



    /**
     * Sets the local filter values based on the Shared Preferences.
     */
    private void setFilterValues( SharedPreferences sharedPreferences, String minKey, String maxKey )
    {
        filterSet = true;
        minFilterValue = sharedPreferences.getInt( minKey, NO_FILTER_VALUE);
        maxFilterValue = sharedPreferences.getInt( maxKey, NO_FILTER_VALUE);
    }



    /**
     * Sets the map data type according to the Preferences.
     */
    public void setDataType( String value )
    {
        if( value.equals("canopy_height_model") )
        {
            dataTypeCode = CHM_CODE;
            dataTypeUrlString = CHM_STRING;
        }
        else if( value.equals("digital_elevation_model") )
        {
            dataTypeCode = DEM_CODE;
            dataTypeUrlString = DEM_STRING;
        }
        else if( value.equals("above_ground_biomass") )
        {
            dataTypeCode = AGB_CODE;
            dataTypeUrlString = AGB_STRING;
        }
    }



    /**
     * Returns a complete URL for a tile for the given data type,
     * filter state, zoom level, x coordinate, and y coordinate.
     */
    public static String getTileUrlString( boolean filterSet, String dataTypeUrlString, int zoom, int x, int y )
    {
        if( filterSet )
        {
            return String.format( FILTER_ROOT_STRING + dataTypeUrlString + FILTER_TILE_STRING,
                    zoom, x, y, minFilterValue, maxFilterValue );
        }
        return String.format( BASE_ROOT_STRING + dataTypeUrlString + BASE_TILE_STRING, zoom, x, y );
    }



    /**
     * Creates the appropriate tile provider based on the connectivity mode
     * before adding a tile overlay to the map.
     */
    public void addTileOverlay()
    {
        // Determine which tile provider should be used.
        if( connectivityMode == CONNECTED_MODE )
        {
            tileProvider = new ConnectedTileProvider();
        }
        else if( connectivityMode == OFFLINE_MODE )
        {
            tileProvider = new OfflineTileProvider();
        }

        // Add a tile overlay to the map.
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
            // Modify the y tile coordinate to convert from TMS to XYZ tiles.
            // This is necessary because Google Maps uses XYZ standard tiles
            // but stored data tiles are of the TMS standard.
            // - - - This should be able to be removed after we can tile to XYZ coordinates - - -
            y = ( 1 << zoom ) - y - 1;

            // Build the URL of the map tile based on its zoom, x coordinate, and y coordinate
            String urlStr = getTileUrlString( filterSet, dataTypeUrlString, zoom, x, y );

            try
            {
                return new URL( urlStr );
            }
            catch(MalformedURLException e)
            {
                throw new AssertionError(e);
            }
        }
    } // End of Connected Tile Provider class.



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

            return NO_TILE;
        }
    } // End of Offline Tile Provider class.



} // End of Base Map class.


