package com.example.biomapper;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A page that displays information to the user about the app and how to use it.
 */
public class AboutPage extends Fragment
{
    // Declare references used to access views, other fragments, and shared preferences.
    private static MainActivity mainActivity;
    private static FragmentManager fragmentManager;



    /**
     * Called when the fragment is being created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );

        // Initialize references to Main Activity and the fragment manager.
        mainActivity = (MainActivity) getActivity();
        fragmentManager = mainActivity.getSupportFragmentManager();
    }



    /**
     * Called after onCreate, creates the fragment's view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate( R.layout.fragment_about_page, container, false );
        return view;
    }



    /**
     * Called after the view has been created.
     * Initializes references to UI view components amd sets up the toolbar.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState )
    {
        // Create the toolbar.
        Toolbar toolbar = (Toolbar) view.findViewById( R.id.about_page_toolbar );

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
    }



    /**
     * Returns the app to the Action Menu.
     * Adds or shows the Action Menu and removes the About Page.
     */
    private void openActionMenu()
    {
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
        if( fragmentManager.findFragmentByTag( "about_page" ) != null )
        {
            fragmentTransaction.remove( fragmentManager.findFragmentByTag( "about_page" ) );
        }

        fragmentTransaction.commit();
    }


} // End of Filter Manager.

