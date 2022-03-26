package com.example.biomapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.biomapper.databinding.FragmentMainMapBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * The map that is initially displayed upon launching the app.
 * Relies on the Base Map fragment for most of its functionality.
 * Includes a button to open the Action Menu.
 * TODO: Includes a map key/legend as an image that relates the map color values to the corresponding data values
 */
public class MainMap extends Fragment
{
    private FragmentMainMapBinding binding;
    private MainActivity mainActivity;
    private FragmentManager fragmentManager;
    private SharedPreferences sharedPreferences;

    private static final int CHM_CODE = 0;
    private static final int DEM_CODE = 1;
    private static final int AGB_CODE = 2;
    private int[] chmDataValArray = {0, 9, 18, 27, 36, 45};
    private int[] demDataValArray = {0, 300, 600, 900, 1200, 1500};
    private int[] agbDataValArray = {0, 90, 180, 270, 360, 450};

    /**
     * Called when the map container is created.
     * Adds the Base Map to this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Initialize reference to Main Activity and the fragment manager.
        mainActivity = (MainActivity) getActivity();
        fragmentManager = mainActivity.getSupportFragmentManager();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );


        // Add the Base Map.
        //fragmentManager.beginTransaction().add( R.id.base_map, mainActivity.baseMap ).commit();
    }



    /**
     * Called after onCreate(). Creates the view of the fragment.
     * Adds functionality to the Action Menu button.
     */
    @Nullable
    @Override
    public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState )
    {
        // Inflate the layout for this fragment.
        View view = inflater.inflate( R.layout.fragment_main_map, container, false );

        // Return the view.
        return view;
    }



    /**
     * Called after the view has been created.
     * Adds a toolbar and the functionality for its back button.
     */
    @Override
    public void onViewCreated( @NonNull View view, @Nullable Bundle savedInstanceState )
    {
        // Add functionality to action Menu Button.
        FloatingActionButton actionMenuButton = mainActivity.findViewById( R.id.actionMenuButton );
        actionMenuButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick( View view )
                    {
                        openActionMenu();
                    }
                }
        );

        setColorBarValues();
    }



    /**
     * Updates the Main Map whenever the user navigates back from the Action Menu.
     */
    public void updateMap()
    {
        mainActivity.baseMap.updateMap();

        setColorBarValues();
    }



    /**
     * Sets the data values shown within the color bar based on the current data type.
     */
    private void setColorBarValues()
    {
        // Check the Preferences to see if the color bar should be shown or not.
        if( sharedPreferences.getBoolean( getString( R.string.show_color_bar ), false ) )
        {
            // Make the color bar visible.
            mainActivity.findViewById( R.id.color_bar ).setVisibility(View.VISIBLE);

            // Determine the data type values and units based on the currently selected data type.
            int[] dataValArray;
            String units = "";

            int dataTypeCode = mainActivity.baseMap.dataTypeCode;
            if( dataTypeCode == CHM_CODE )
            {
                dataValArray = chmDataValArray;
                units = "m";
            }
            else if( dataTypeCode == DEM_CODE )
            {
                dataValArray = demDataValArray;
                units = "m";
            }
            else if( dataTypeCode == AGB_CODE )
            {
                dataValArray = agbDataValArray;
                units = "Mg/ha";
            }
            else
            {
                dataValArray = new int[] { -1, -1, -1, -1, -1 };
            }

            // Set the values of the Text Views that are within the color bar.
            ((TextView) mainActivity.findViewById( R.id.color_bar_min )).setText( dataValArray[0] + units );
            ((TextView) mainActivity.findViewById( R.id.color_bar_1 )).setText( dataValArray[1] + units );
            ((TextView) mainActivity.findViewById( R.id.color_bar_2 )).setText( dataValArray[2] + units );
            ((TextView) mainActivity.findViewById( R.id.color_bar_3 )).setText( dataValArray[3] + units );
            ((TextView) mainActivity.findViewById( R.id.color_bar_4 )).setText( dataValArray[4] + units );
            ((TextView) mainActivity.findViewById( R.id.color_bar_max )).setText( dataValArray[5] + units );

            // Reset the color bar's layout, making it resize to only be as wide as necessary.
            mainActivity.findViewById( R.id.color_bar_max ).requestLayout();
        }
        else
        {
            // Make the color bar invisible.
            mainActivity.findViewById(R.id.color_bar).setVisibility(View.INVISIBLE);
        }
    }



    /**
     * Add or show the Action Menu and hide the Main Map.
     * This is done instead of replacing the Main Map with the menu so that
     * the Base Map isn't destroyed each time the Action Menu is opened.
     */
    private void openActionMenu()
    {
        if( mainActivity.baseMap.roiMarker != null )
        {
            mainActivity.baseMap.roiMarker.remove();
            mainActivity.baseMap.roiMarker = null;
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // If the Action Menu exists, show it.
        if( fragmentManager.findFragmentByTag( "action_menu" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("action_menu") );
        }
        // Else the Action Menu fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new ActionMenu(), "action_menu" );
        }

        // Hide the Main Map.
        if( fragmentManager.findFragmentByTag( "main_map" ) != null)
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag( "main_map" ) );
        }

        fragmentTransaction.commit();
    }

} // End of Main Map class.