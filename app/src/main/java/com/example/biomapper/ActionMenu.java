package com.example.biomapper;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * The menu containing all settings and possible actions.
 * Relies on the Preferences fragment for most of its functionality.
 * Has a back button for returning to the Main Map.
 */
public class ActionMenu extends Fragment
{
    private MainActivity mainActivity;
    private FragmentManager fragmentManager;


    /**
     * Called when the menu is created.
     * Initializes needed references.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Initialize references to Main Activity and the fragment manager.
        mainActivity = (MainActivity) getActivity();
        fragmentManager = mainActivity.getSupportFragmentManager();
    }



    /**
     * Called after onCreate(). Creates the fragment's view.
     * Applies the appropriate theme and adds the Preferences.
     */
    @Override
    public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState )
    {
        // Apply the custom theme for ensuring the desired colors are used.
        getContext().getTheme().applyStyle( R.style.Theme_Preferences, true );

        // Inflate the layout for this fragment.
        View view = inflater.inflate( R.layout.fragment_action_menu, container, false );

        // Adds the Preferences layout to the view.
        if( savedInstanceState == null )
        {
            fragmentManager.beginTransaction()
                .replace( R.id.actionMenuContent, mainActivity.preferences )
                .commit();
        }

        return view;
    }



    /**
     * Called after the view has been created.
     * Adds a toolbar and the functionality for its back button.
     */
    @Override
    public void onViewCreated( @NonNull View view, @Nullable Bundle savedInstanceState )
    {
        // Create the toolbar.
        Toolbar toolbar = (Toolbar) view.findViewById( R.id.actionMenuToolbar );

        // Add a back button to the toolbar and make it functional.
        toolbar.setNavigationIcon( getContext().getDrawable(R.drawable.toolbar_back_icon) );
        toolbar.setNavigationOnClickListener(
            new View.OnClickListener()
            {
                /**
                 * Returns the app to the Main Map, applying any changes made
                 * to the preferences.
                 * Adds or shows the Main Map and hides the Action Menu.
                 * This is done instead of replacing the menu with the map so that
                 * the menu isn't destroyed upon returning to the map.
                 */
                @Override
                public void onClick( View view )
                {
                    // Boolean for determining if the tiles need to be reloaded/updated.
                    boolean shouldUpdateTiles = false;

                    // Change the theme back to the base theme.
                    getContext().getTheme().applyStyle( R.style.Theme_Biomapper, true );

                    // If the map data type has been changed, update the tiles to reflect this.
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );
                    BaseMap baseMap = mainActivity.baseMap;
                    if(
                            baseMap.dataTypeValue != sharedPreferences.getString( getString( R.string.set_data_type ),"-1")
                    )
                    {
                        // Update the map data type accordingly.
                        baseMap.dataTypeValue = sharedPreferences.getString( getString( R.string.set_data_type ),"-1");
                        baseMap.dataTypeUrlString = baseMap.getDataUrlStringFromValue( baseMap.dataTypeValue );

                        // Reload tiles so they're of the new data type.
                        shouldUpdateTiles = true;

                        // - - - log statements are to be removed before release - - -
                        Log.e(TAG,String.format("New Data Type: %s", baseMap.dataTypeUrlString) );
                    }

                    // TODO If offline mode has been enabled, begin using offline tiles instead
                    if( false )
                    {
                        // Update the connectivity mode according to the preferences.
                        baseMap.connectivityMode = -1;

                        // Reload tiles so the offline ones are used.
                        shouldUpdateTiles = true;
                    }

                    // If connectivity changed, data is filtered, or set to offline mode,
                    // the outdated tiles need to be removed and reloaded.
                    if( shouldUpdateTiles )
                    {
                        baseMap.removeTileOverlay();
                        baseMap.addTileOverlay();
                    }

                    // If the Main Map exists, show it.
                    if( fragmentManager.findFragmentByTag( "main_map" ) != null )
                    {
                        fragmentManager.beginTransaction().show( fragmentManager.findFragmentByTag( "main_map" ) ).commit();
                    }
                    // Else the Main Map fragment does not exist. Add it to fragment manager.
                    else
                    {
                        fragmentManager.beginTransaction().add( R.id.fragment_container, new MainMap(), "main_map" ).commit();
                    }
                    // Hide the Action Menu.
                    if( fragmentManager.findFragmentByTag( "action_menu" ) != null )
                    {
                        fragmentManager.beginTransaction().hide( fragmentManager.findFragmentByTag( "action_menu" ) ).commit();
                    }
                }
            }
        );

    } // End of onViewCreated function.

} // End of ActionMenu class.