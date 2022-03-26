package com.example.biomapper;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;
import android.util.Log;

/**
 * The the list of settings and possible actions.
 */
public class Preferences extends PreferenceFragmentCompat
{
    MainActivity mainActivity;
    private FragmentManager fragmentManager;

    /**
     * Called when creating the Preferences.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        // Initialize reference to Main Activity and the fragment manager.
        mainActivity = (MainActivity) getActivity();
        fragmentManager = mainActivity.getSupportFragmentManager();
    }



    /**
     * Called once the Preferences have been created.
     * Adds functionality to the buttons.
     */
    @Override
    public void onCreatePreferences( Bundle savedInstanceState, String rootKey )
    {
        // Specify the preference screen to be used.
        setPreferencesFromResource( R.xml.root_preferences, rootKey );

        // Add functionality to the button that opens the data filtering fragment.
        Preference filterDataButton = findPreference( getString( R.string.filter_data ) );
        filterDataButton.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    openFilterManager();
                    return true;
                }
            }
        );


        // Add functionality to the button that lets the user select the region of interest.
        Preference setRoiButton = findPreference( getString( R.string.set_roi ) );
        setRoiButton.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    openRoiManager();
                    return true;
                }
            }
        );

        // Add functionality to the button that downloads tiles that
        // cover the region of interest to internal storage.
        Preference downloadRoiButton = findPreference( getString( R.string.download_roi ) );
        downloadRoiButton.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    // Download tiles that cover the roi to internal storage
                    openDownloadManager();
                    return true;
                }
            }
        );

        // Add functionality to the button that opens the About Page.
        Preference aboutPageButton = findPreference( getString( R.string.open_about_page ) );
        aboutPageButton.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener()
                {
                    @Override
                    public boolean onPreferenceClick(Preference preference)
                    {
                        openAboutPage();
                        return true;
                    }
                }
        );

    } // End of OnCreatePreferences function.



    /**
     * Opens the Filter Manager fragment.
     */
    private void openFilterManager()
    {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // If the Filter Data fragment exists, show it.
        if( fragmentManager.findFragmentByTag( "data_filter" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("data_filter") );
        }
        // Else the Filter Data fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new FilterManager(), "data_filter" );
        }

        // Hide the Action Menu.
        if( fragmentManager.findFragmentByTag("action_menu") != null )
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag("action_menu") );
        }

        fragmentTransaction.commit();
    }



    /**
     * Opens the Region of Interest Manager fragment. Also makes the Base Map visible.
     */
    private void openRoiManager()
    {
        mainActivity.baseMap.isSelectingRoi = true;
        mainActivity.baseMap.updateMap();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // If the Base Map fragment exists, show it.
        if( fragmentManager.findFragmentByTag( "base_map" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("base_map") );
        }

        // If the Roi Selection fragment exists, show it.
        if( fragmentManager.findFragmentByTag( "roi_manager" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("roi_manager") );
        }
        // Else the Roi Selection fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new RoiManager(), "roi_manager" );
        }

        // If the Action Menu exists, hide it.
        if( fragmentManager.findFragmentByTag("action_menu") != null )
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag("action_menu") );
        }

        fragmentTransaction.commit();
    }



    /**
     * Opens the Download Manager fragment.
     */
    private void openDownloadManager()
    {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // If the Roi Selection fragment exists, show it.
        if( fragmentManager.findFragmentByTag( "download_manager" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("download_manager") );
        }
        // Else the Roi Selection fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new DownloadManager(), "download_manager" );
        }

        // If the Action Menu exists, hide it.
        if( fragmentManager.findFragmentByTag("action_menu") != null )
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag("action_menu") );
        }

        fragmentTransaction.commit();
    }



    /**
     * Opens the About Page fragment.
     */
    private void openAboutPage()
    {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // If the Roi Selection fragment exists, show it.
        if( fragmentManager.findFragmentByTag( "about_page" ) != null )
        {
            fragmentTransaction.show( fragmentManager.findFragmentByTag("about_page") );
        }
        // Else the Roi Selection fragment does not exist. Add it to fragment manager.
        else
        {
            fragmentTransaction.add( R.id.main_activity_container, new AboutPage(), "about_page" );
        }

        // If the Action Menu exists, hide it.
        if( fragmentManager.findFragmentByTag("action_menu") != null )
        {
            fragmentTransaction.hide( fragmentManager.findFragmentByTag("action_menu") );
        }

        fragmentTransaction.commit();
    }

} // End of Preferences class.