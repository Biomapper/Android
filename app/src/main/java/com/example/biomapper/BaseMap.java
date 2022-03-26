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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private SupportMapFragment mapFragment;
    private MainActivity mainActivity;
    private SharedPreferences sharedPreferences;
    private GoogleMap googleMap;
    private TileOverlay tileOverlay;
    private TileProvider tileProvider;

    // Restrict the zoom level to 5 and 13.
    // - - - Once a complete set of tiles is added to the server, change to 5 to 13 - - -
    public static final int MIN_ZOOM = 5;
    public static final int MAX_ZOOM = 10;
    private static final int INITIAL_ZOOM = 6;

    // Specifies the size of the map tiles.
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;

    // Values used for setting the map data type.
    private static final int CHM_CODE = 0;
    private static final int DEM_CODE = 1;
    private static final int AGB_CODE = 2;
    private static String dataTypeValue;
    public static int dataTypeCode;

    // Strings used for building map tile URLs.
    private static final String BASE_ROOT_STRING = "http://13.59.201.133/map-tiles/";
    private static final String FILTER_ROOT_STRING = "http://13.59.201.133:3000/base-tiles/";

    private static final String CHM_STRING = "chm/";
    private static final String DEM_STRING = "dem/";
    private static final String AGB_STRING = "agb/";
    private static String dataTypeUrlString;

    // Values for determining which tile provider to use.
    private static final int CONNECTED_MODE = 200;
    private static final int OFFLINE_MODE = 201;
    public static int connectivityMode;

    // Values for determining if data filter/modified tiles should be used.
    private static final int NO_FILTER_VALUE = -1;
    public static boolean filterSet;
    public static boolean filterChanged;
    private static int[] filterValues;
    public static int[] mappedFilterValues;
    private static final int[] ABS_MAX_ARRAY = { 45, 1500, 129 };

    // Values for markers and the region of interest.
    private static MarkerOptions clickLocMarkerOptions;
    private static MarkerOptions roiMarkerOptions;
    private static Marker clickLocMarker;
    public static Marker roiMarker;
    public static float tempRoiLat;
    public static float tempRoiLon;
    public static boolean isSelectingRoi;



    /**
     * Called when the map is created.
     * Ensures that the map data type, connectivity mode, etc. is as specified in the Preferences.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Initialize reference to the Main Activity and shared preferences.
        mainActivity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );

        // Set the map data type value according to the preferences.
        dataTypeValue = sharedPreferences.getString( getString( R.string.set_data_type ),"-1");
        setDataType( dataTypeValue );

        // Set the data filter according to the preferences.
        if( dataTypeCode == CHM_CODE &&
                sharedPreferences.getBoolean( getString( R.string.chm_filter_set ), false ) )
        {
            filterSet = true;
            setFilterValues( getString(R.string.chm_filter_min), getString(R.string.chm_filter_max) );
            mappedFilterValues = calculateMappedFilterValues( getString(R.string.chm_filter_min),
                    getString(R.string.chm_filter_max), ABS_MAX_ARRAY[dataTypeCode] );
        }
        else if( dataTypeCode == DEM_CODE &&
                sharedPreferences.getBoolean( getString( R.string.dem_filter_set ), false ) )
        {
            filterSet = true;
            setFilterValues( getString(R.string.dem_filter_min), getString(R.string.dem_filter_max) );
            mappedFilterValues = calculateMappedFilterValues( getString(R.string.dem_filter_min),
                    getString(R.string.dem_filter_max), ABS_MAX_ARRAY[dataTypeCode] );
        }
        else if( dataTypeCode == AGB_CODE &&
                sharedPreferences.getBoolean( getString( R.string.agb_filter_set ), false ) )
        {
            filterSet = true;
            setFilterValues( getString(R.string.agb_filter_min), getString(R.string.agb_filter_max) );
            mappedFilterValues = calculateMappedFilterValues( getString(R.string.agb_filter_min),
                    getString(R.string.agb_filter_max), ABS_MAX_ARRAY[dataTypeCode] );
        }
        else
        {
            filterSet = false;
            filterValues = new int[] { NO_FILTER_VALUE, NO_FILTER_VALUE };
            mappedFilterValues = new int[] { NO_FILTER_VALUE, NO_FILTER_VALUE };
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

        // Check if download map tiles have changed. Only applies if in offline mode.
        if( sharedPreferences.getBoolean( getString( R.string.downloaded_tiles_changed ), false )
                && connectivityMode == OFFLINE_MODE )
        {
            // Acknowledged the set of downloaded tiles have changed.
            sharedPreferences.edit().putBoolean( getString( R.string.downloaded_tiles_changed ), false ).commit();
        }

        // Set the values used for markers.
        clickLocMarker = null;
        roiMarker = null;
        tempRoiLat = -1;
        tempRoiLon = -1;
        isSelectingRoi = false;
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
                public void onMapReady( @NonNull GoogleMap availableMap )
                {
                    setUpMap( availableMap );
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
    public void setUpMap( @NonNull GoogleMap availableMap )
    {
        // Initialize map variable.
        googleMap = availableMap;

        // Customise the styling of the base map using a JSON object defined
        // in a raw resource file.
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        try
        {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getContext(), R.raw.style_json));

            if( !success )
            {
                Log.e(TAG, "Style parsing failed.");
            }
        }
        catch( Resources.NotFoundException e )
        {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        // Guarantee that the compass is enabled.
        googleMap.getUiSettings().setCompassEnabled(true);

        // Disable 3D maps and buildings.
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        googleMap.setBuildingsEnabled(false);

        // Set the zoom limitations.
        googleMap.setMinZoomPreference(MIN_ZOOM);
        googleMap.setMaxZoomPreference(MAX_ZOOM);

        // Make it so clicking on a marker only shows its title.
        googleMap.setOnMarkerClickListener(
                new GoogleMap.OnMarkerClickListener()
                {
                    @Override
                    public boolean onMarkerClick( Marker marker )
                    {
                        marker.showInfoWindow();
                        //mMap.moveCamera( CameraUpdateFactory.newLatLng( marker.getPosition() ) );
                        return true;
                    }
                }
        );
        // Create the standard options to be used by map markers.
        clickLocMarkerOptions = new MarkerOptions()
                .icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_GREEN ) );
        roiMarkerOptions = new MarkerOptions()
                .icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_AZURE ) );
        // Make it so clicking on the map adds the appropriate marker.
        googleMap.setOnMapClickListener(
                new GoogleMap.OnMapClickListener()
                {
                    @Override
                    public void onMapClick( LatLng point )
                    {
                        if( isSelectingRoi )
                        {
                            if( roiMarker != null )
                            {
                                roiMarker.remove();
                                roiMarker = null;
                            }
                            tempRoiLat = (float) point.latitude;
                            tempRoiLon = (float) point.longitude;
                            roiMarker = googleMap.addMarker( roiMarkerOptions.position( point )
                                    .title( String.format( "%.4f, %.4f", point.latitude, point.longitude ) ) );
                        }
                        else
                        {
                            if( clickLocMarker != null )
                            {
                                clickLocMarker.remove();
                                clickLocMarker = null;
                            }
                            else
                            {
                                clickLocMarker = googleMap.addMarker( clickLocMarkerOptions.position( point )
                                        .title( String.format( "%.4f, %.4f", point.latitude, point.longitude ) ) );
                            }
                        }
                    }
                }
        );
        // Add a ROI marker if one has been set.
        roiMarker = addRoiMarker();

        // Get location permission.
        if( PackageManager.PERMISSION_GRANTED !=
                getContext().checkCallingOrSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) )
        {
            ActivityCompat.requestPermissions( mainActivity, LOCATION_PERMS, REQUEST_CODE );
        }
        // Activate the use of the "set my location" button.
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Check if the default center location is the device location.
        if( sharedPreferences.getString( getString( R.string.set_default_center_loc ),
            "device_location").equals("device_location")
            && PackageManager.PERMISSION_GRANTED ==
            getContext().checkCallingOrSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) )
        {
            mainActivity.fusedLocationClient.getLastLocation()
                    .addOnSuccessListener( mainActivity,
                            new OnSuccessListener<Location>()
                            {
                                @Override
                                public void onSuccess( Location location )
                                {
                                    // Got last known location. In some rare situations this can be null.
                                    if( location != null )
                                    {
                                        googleMap.moveCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                        new LatLng(
                                                                location.getLatitude(),
                                                                location.getLongitude()
                                                        ), INITIAL_ZOOM
                                                )
                                        );
                                    }
                                }

                            }
                    );
        }
        // Else the default center location is the ROI if it exists.
        else if( roiMarker != null )
        {
            googleMap.moveCamera( CameraUpdateFactory.newLatLngZoom( roiMarker.getPosition(), INITIAL_ZOOM ) );
        }
        // Else center on a point in Africa.
        else
        {
            googleMap.moveCamera( CameraUpdateFactory.newLatLngZoom( new LatLng( 0, 20 ), INITIAL_ZOOM ) );
        }

        // Add data tiles to the map.
        addTileOverlay();
    }



    /**
     * Called when the map is re-opened.
     * Updates map components if any changes were made since the map was last opened.
     */
    public void updateMap()
    {
        // Add a ROI marker if one has been set.
        roiMarker = addRoiMarker();

        // Boolean for determining if the tiles need to be reloaded/updated.
        boolean shouldUpdateTiles = false;

        // If the map data type has been changed, begin using tiles of that type.
        if( !dataTypeValue.equals( sharedPreferences.getString( getString( R.string.set_data_type ), CHM_STRING) ) )
        {
            // Update the map data type accordingly.
            dataTypeValue = sharedPreferences.getString( getString( R.string.set_data_type ), CHM_STRING );
            setDataType( dataTypeValue );

            // Reload tiles so they're of the new data type.
            Log.e(TAG, "Data type changed.");
            shouldUpdateTiles = true;
        }

        // Apply any changes that might've been made to the filter.
        if(
            ( dataTypeCode == CHM_CODE && applyFilterChanges( getString(R.string.chm_filter_set),
                    getString(R.string.chm_filter_min), getString(R.string.chm_filter_max) ) )
            ||
            ( dataTypeCode == DEM_CODE && applyFilterChanges( getString( R.string.dem_filter_set ),
                    getString( R.string.dem_filter_min ), getString( R.string.dem_filter_max ) ) )
            ||
            ( dataTypeCode == AGB_CODE && applyFilterChanges( getString( R.string.agb_filter_set ),
                    getString( R.string.agb_filter_min ), getString( R.string.agb_filter_max ) ) )
          )
        {
            Log.e(TAG, "Changes made to the filter.");
            shouldUpdateTiles = true;
        }

        // If currently using online tiles but offline mode has been enabled,
        // begin using offline tiles instead.
        if( connectivityMode == CONNECTED_MODE &&
                sharedPreferences.getBoolean( getString( R.string.enable_offline_mode ), false ) )
        {
            // Update the connectivity mode according to the preferences.
            connectivityMode = OFFLINE_MODE;

            // Reload tiles so the offline ones are used.
            Log.e(TAG, "Offline mode enabled.");
            shouldUpdateTiles = true;
        }
        // Else if currently using offline tiles but offline mode has been disabled,
        // begin using online tiles instead.
        else if( connectivityMode == OFFLINE_MODE &&
                !sharedPreferences.getBoolean( getString( R.string.enable_offline_mode ), false ) )
        {
            // Update the connectivity mode according to the preferences.
            connectivityMode = CONNECTED_MODE;

            // Reload tiles so the offline ones are used.
            Log.e(TAG, "Online mode enabled.");
            shouldUpdateTiles = true;
        }

        // Check if download map tiles have changed. Only applies if in offline mode.
        if( sharedPreferences.getBoolean( getString( R.string.downloaded_tiles_changed ), false )
            && sharedPreferences.getBoolean( getString( R.string.enable_offline_mode ), false ) )
        {
            // Acknowledged the set of downloaded tiles have changed.
            sharedPreferences.edit().putBoolean( getString( R.string.downloaded_tiles_changed ), false ).commit();

            Log.e(TAG, "Downloaded tiles have changed.");
            shouldUpdateTiles = true;
        }

        // If new data type selected, data is filtered, offline mode changed,
        // or a new set of tiles downloaded, the outdated tiles need to be removed and reloaded.
        if( shouldUpdateTiles )
        {
            // - - - remove logs - - -
            Log.e(TAG, "Tile Overlay updated!");
            removeTileOverlay();
            addTileOverlay();
        }

    } // End of Update Map function.



    /**
     * Attempts to apply any changes made to the filter.
     * Returns whether changes were applied or not.
     */
    private boolean applyFilterChanges( String setKey, String minKey, String maxKey )
    {
        // Check if filter set state has been changed.
        if( filterSet != sharedPreferences.getBoolean( setKey, false ) )
        {
            // Update the filter set state.
            filterSet = sharedPreferences.getBoolean( setKey, false );

            // Check if the filter has been set.
            if( filterSet )
            {
                setFilterValues( minKey, maxKey );
                mappedFilterValues = calculateMappedFilterValues( minKey, maxKey, ABS_MAX_ARRAY[dataTypeCode] );
            }
            // Else the filter has been removed.
            else
            {
                filterValues = new int[] { NO_FILTER_VALUE, NO_FILTER_VALUE };
                mappedFilterValues = new int[] { NO_FILTER_VALUE, NO_FILTER_VALUE };
            }

            // Return that changes to the filter were made.
            return true;
        }
        // Else check if filter values have been changed.
        else if(
            filterValues[0] != sharedPreferences.getInt( minKey, NO_FILTER_VALUE) ||
            filterValues[1] != sharedPreferences.getInt( maxKey, NO_FILTER_VALUE )
        )
        {
            // Update filter values.
            setFilterValues( minKey, maxKey );
            mappedFilterValues = calculateMappedFilterValues( minKey, maxKey, ABS_MAX_ARRAY[dataTypeCode] );

            // Return that changes to the filter were made.
            return true;
        }

        // Else no changes have been made to the filter.
        return false;
    }



    /**
     * Sets the local filter values based on the Shared Preferences.
     */
    private void setFilterValues( String minKey, String maxKey )
    {
        filterValues = new int[]
        {
            sharedPreferences.getInt( minKey, NO_FILTER_VALUE),
            sharedPreferences.getInt( maxKey, NO_FILTER_VALUE)
        };
    }



    /**
     * Scale the filter values so they're between 0 an 1200.
     */
    public int[] calculateMappedFilterValues( String minKey, String maxKey, double absMax )
    {
        return new int[]
            {
                (int) Math.floor( sharedPreferences.getInt( minKey, NO_FILTER_VALUE) / absMax * 1200 ),
                (int) Math.floor( sharedPreferences.getInt( maxKey, NO_FILTER_VALUE) / absMax * 1200 )
            };
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
     * Returns a complete URL for a base tile for the given data type,
     * zoom level, x coordinate, and y coordinate.
     */
    public static String getBaseTileUrlString( String dataTypeUrlString, int zoom, int x, int y )
    {
        return BASE_ROOT_STRING + dataTypeUrlString + zoom + "/" + x + "/" + y + ".png";
    }



    /**
     * Returns a complete URL for a filtered tile for the given data type,
     * zoom level, x coordinate, y coordinate, and filter values.
     */
    public static String getFilteredTileUrlString( String dataTypeUrlString,
        int zoom, int x, int y, int minMappedFilterVal, int maxMappedFilterVal )
    {
        return FILTER_ROOT_STRING + dataTypeUrlString + zoom + "/" + x + "/" + y + "/" + minMappedFilterVal + "/" + maxMappedFilterVal;
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
        tileOverlay = googleMap.addTileOverlay( new TileOverlayOptions()
                .tileProvider( tileProvider ) );
    }



    /**
     * Removes the current tile overlay from the map.
     */
    public void removeTileOverlay()
    {
        // Remove the tile overlay from the map.
        tileOverlay.remove();
    }



    /**
     * Add a region of interest marker to the map if it's appropriate.
     */
    private Marker addRoiMarker()
    {
        // Check if a saved ROI point exists and therefore should be added.
        if( sharedPreferences.getBoolean( getString( R.string.roi_set ), false ) )
        {
            LatLng roiLatLon = new LatLng(
                    sharedPreferences.getFloat( getString( R.string.roi_lat ), 0),
                    sharedPreferences.getFloat( getString( R.string.roi_lng ), 22)
            );

            return googleMap.addMarker(
                    roiMarkerOptions.position( roiLatLon )
                            .title( String.format( "%.4f, %.4f", roiLatLon.latitude, roiLatLon.longitude ) )
            );
        }
        return null;
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

            // Build the URL of the map tile based on its data type,
            // zoom, x coordinate, y coordinate, and if a filter has been set.
            String urlString = filterSet ?
                    getFilteredTileUrlString( dataTypeUrlString,
                            zoom, x, y, mappedFilterValues[0], mappedFilterValues[1] ) :
                    getBaseTileUrlString( dataTypeUrlString, zoom, x, y ); ;

            try
            {
                return new URL( urlString );
            }
            catch(MalformedURLException e)
            {
                throw new AssertionError(e);
            }
        }
    } // End of Connected Tile Provider class.



    /**
     * Custom tile provider class to be used when offline.
     */
    private class OfflineTileProvider implements TileProvider
    {
        /**
         * Gets the desired tile from internal storage.
         */
        @Nullable
        @Override
        public Tile getTile( int x, int y, int zoom )
        {
            // Modify the y tile coordinate to convert from TMS to XYZ tiles.
            // This is necessary because Google Maps uses XYZ standard tiles
            // but stored data tiles are of the TMS standard.
            // - - - This should be able to be removed after we can tile to XYZ coordinates - - -
            y = ( 1 << zoom ) - y - 1;

            String folderPath = getContext().getFilesDir() + "/" + dataTypeUrlString + zoom + "/" + x + "/" + y + ".png";

            FileInputStream fileInputStream = null;
            try
            {
                fileInputStream = new FileInputStream( folderPath );
            }
            catch( FileNotFoundException e )
            {
                e.printStackTrace();
                return NO_TILE;
            }

            Bitmap bitmap = BitmapFactory.decodeStream( fileInputStream );
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapData = stream.toByteArray();

            return new Tile( TILE_WIDTH, TILE_HEIGHT, bitmapData );
        }

    } // End of Offline Tile Provider class.

} // End of Base Map class.


