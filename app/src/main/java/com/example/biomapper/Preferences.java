package com.example.biomapper;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;
import android.util.Log;

/**
 * The the list of settings and possible actions.
 */
public class Preferences extends PreferenceFragmentCompat
{
    private static final String TAG = "PreferenceFragment";

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
                    // TODO Open data filtering fragment

                    // - - - Logs are to be removed before release - - -
                    Log.e(TAG, "Data filter button pressed!");

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
                    // TODO Open a map that lets the user select a region of interest

                    // - - - Logs are to be removed before release - - -
                    Log.e(TAG, "Set roi button pressed!");

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
                    // TODO Download tiles that cover the roi to internal storage

                    // - - - Logs are to be removed before release - - -
                    Log.e(TAG, "Download roi button pressed!");

                    return true;
                }
            }
        );

        // Add functionality to the button that deletes all
        // downloaded tiles located in internal storage.
        Preference deleteRoiButton = findPreference( getString( R.string.delete_downloaded_roi ) );
        deleteRoiButton.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    // TODO Delete the local stores located in internal storage

                    // - - - Logs are to be removed before release - - -
                    Log.e(TAG, "Delete roi button pressed!");

                    return true;
                }
            }
        );

    }


}