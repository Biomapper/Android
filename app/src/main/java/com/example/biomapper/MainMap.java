package com.example.biomapper;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.biomapper.databinding.FragmentMainMapBinding;

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

        // Add the Base Map.
        fragmentManager.beginTransaction().add( R.id.base_map, mainActivity.baseMap ).commit();
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
        // Bind the Main Map layout to this object.
        binding = FragmentMainMapBinding.inflate( inflater, container, false );

        // Add functionality to action Menu Button.
        binding.actionMenuButton.setOnClickListener(
            new View.OnClickListener()
            {
                /**
                 * Add or show the Action Menu and hide the Main Map.
                 * This is done instead of replacing the map with the menu so that
                 * the map isn't destroyed upon opening the Action Menu.
                 */
                @Override
                public void onClick( View view )
                {
                    // If the Action Menu exists, show it.
                    if( fragmentManager.findFragmentByTag( "action_menu" ) != null )
                    {
                       fragmentManager.beginTransaction().show( fragmentManager.findFragmentByTag("action_menu") ).commit();
                    }
                    // Else the Action Menu fragment does not exist. Add it to fragment manager.
                    else
                    {
                        fragmentManager.beginTransaction().add( R.id.fragment_container, new ActionMenu(), "action_menu" ).commit();
                    }
                    // Hide the Main Map.
                    if(fragmentManager.findFragmentByTag("main_map") != null)
                    {
                        fragmentManager.beginTransaction().hide( fragmentManager.findFragmentByTag("main_map") ).commit();
                    }
                }
            }
        );

        // Return the view.
        return binding.getRoot();
    }

}