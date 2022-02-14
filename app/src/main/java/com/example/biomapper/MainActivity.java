package com.example.biomapper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import android.os.Bundle;

/**
 * The main, and only, activity.
 * The component that is first created when the app is launched.
 * At any point it will be displaying the Main Map, the Action Menu, etc.
 */
public class MainActivity extends AppCompatActivity
{
    // References to fragments that need to be accessed by other fragments.
    public Preferences preferences;
    public BaseMap baseMap;


    /**
     * Called when the activity is created.
     * Initializes references to fragments and displays the Main Map.
     */
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Initialize the Preferences object so that it can be accessed in other fragments.
        // This must be done before initializing the Base Map!
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true);
        preferences = new Preferences();

        // Initialize the Base Map so that it can be accessed in other fragments.
        baseMap = new BaseMap();

        // Apply the view to the activity's layout.
        setContentView( R.layout.activity_main );

        // Create the fragment transaction object.
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        // Add the Main Map fragment to the main activity container.
        fragmentTransaction.add( R.id.fragment_container, new MainMap(), "main_map" );
        fragmentTransaction.commit();
    }

}