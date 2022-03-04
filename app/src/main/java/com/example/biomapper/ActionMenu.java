package com.example.biomapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
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
     * Called after onCreate().
     * Creates the fragment's view and adds the Preferences.
     */
    @Override
    public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState )
    {
        // Inflate the layout for this fragment.
        View view = inflater.inflate( R.layout.fragment_action_menu, container, false );

        // Adds the Preferences layout to the view.
        if( savedInstanceState == null )
        {
            fragmentManager.beginTransaction()
                .replace( R.id.action_menu_container, mainActivity.preferences )
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
                @Override
                public void onClick( View view )
                {
                    mainActivity.baseMap.updateMap();

                    openMainMap();
                }
            }
        );

    } // End of onViewCreated function.



    /**
     * Show the already existing Base Map, show or add the Main Map, and hide the Action Menu.
     * This is done so the Base Map isn't re-created every time the user returns to the Main Map.
     */
    private void openMainMap()
    {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Show the Base Map.
        if( mainActivity.baseMap != null )
        {
            fragmentTransaction.show( mainActivity.baseMap );
        }

        // If the Main Map exists, show it.
        if( fragmentManager.findFragmentByTag( "main_map" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag( "main_map" ) );
        }
        // Else the Main Map fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new MainMap(), "main_map" );
        }

        // If the Action Menu exists, hide it.
        if( fragmentManager.findFragmentByTag( "action_menu" ) != null )
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag( "action_menu" ) );
        }

        fragmentTransaction.commit();
    }

} // End of ActionMenu class.