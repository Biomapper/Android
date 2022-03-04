package com.example.biomapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    }



    /**
     * Add or show the Action Menu and hide the Main Map.
     * This is done instead of replacing the Main Map with the menu so that
     * the Base Map isn't destroyed each time the Action Menu is opened.
     */
    private void openActionMenu()
    {
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

}