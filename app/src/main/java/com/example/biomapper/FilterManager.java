package com.example.biomapper;

import static android.content.ContentValues.TAG;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Allows the user to add or remove filters for any of the three data types.
 */
public class FilterManager extends Fragment
{
    // Initialize support constants.
    private static final int NO_FILTER_VAL = -1;
    private static final String EMPTY_STRING = "";
    private static final int CHM_INDEX = 0;
    private static final int DEM_INDEX = 1;
    private static final int AGB_INDEX = 2;
    private static final int[] ABS_MIN_ARRAY = { 0, 0, 0 }; // { CHM, DEM, AGB }
    private static final int[] ABS_MAX_ARRAY = { 45, 1500, 129 };

    // Declare references used to access views, other fragments, and shared preferences.
    private static MainActivity mainActivity;
    private static FragmentManager fragmentManager;
    private static SharedPreferences sharedPreferences;

    // Declare references to view components.
    private Spinner dataTypeSpinner;
    private TextView dataTypeRange;
    private EditText minValEditText;
    private EditText maxValEditText;
    private ImageButton applyFilterButton;
    private ImageButton clearFilterButton;



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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );
    }



    /**
     * Called after onCreate, creates the fragment's view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment.
        View view = inflater.inflate( R.layout.fragment_filter_manager, container, false );
        return view;
    }



    /**
     * Called after the view has been created.
     * Initializes references to UI view components.
     * Also sets the up to toolbar, data type spinner, and the accept/clear filter button.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState )
    {
        // Initialize references to View components.
        dataTypeSpinner = mainActivity.findViewById( R.id.dataTypeSpinner );
        dataTypeRange = mainActivity.findViewById( R.id.dataTypeRange );
        minValEditText = mainActivity.findViewById( R.id.minEditText );
        maxValEditText = mainActivity.findViewById( R.id.maxEditText );
        applyFilterButton = mainActivity.findViewById( R.id.apply_filter );
        clearFilterButton = mainActivity.findViewById( R.id.clear_filter );

        // Create the toolbar.
        Toolbar toolbar = (Toolbar) view.findViewById( R.id.dataFilterToolbar );

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


        // Set data type spinner initial value to the current map data type.
        ListPreference dataTypeListPref = mainActivity.preferences.findPreference( getString( R.string.set_data_type ) );
        String initialDataTypeValue = dataTypeListPref.getValue();
        int initialDataTypeIndex = dataTypeListPref.findIndexOfValue( initialDataTypeValue );
        dataTypeSpinner.setSelection( initialDataTypeIndex );

        // Add functionality to the spinner so that the range and filter values change based on data type.
        dataTypeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener()
                {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
                    {
                        if( position == CHM_INDEX )
                        {
                            // Set the text informing the user of the range for the given data type.
                            dataTypeRange.setText( getString( R.string.chm_range ) );

                            displayFilterValues( getString( R.string.chm_filter_set ),
                                    getString( R.string.chm_filter_min ), getString( R.string.chm_filter_max ) );
                        }
                        else if( position == DEM_INDEX )
                        {
                            // Set the text informing the user of the range for the given data type.
                            dataTypeRange.setText( getString( R.string.dem_range ) );

                            displayFilterValues( getString( R.string.dem_filter_set ),
                                    getString( R.string.dem_filter_min ), getString( R.string.dem_filter_max ) );
                        }
                        else if( position == AGB_INDEX )
                        {
                            // Set the text informing the user of the range for the given data type.
                            dataTypeRange.setText( getString( R.string.agb_range ) );

                            displayFilterValues( getString( R.string.agb_filter_set ),
                                    getString( R.string.agb_filter_min ), getString( R.string.agb_filter_max ) );
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView)
                    {
                        // do nothing
                    }
                }
        );

        // Add functionality to the apply filter button.
        applyFilterButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                applyFilter();
            }
        });

        // Add functionality to the clear filter button.
        clearFilterButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                clearFilter();
            }
        });

    } // End of onViewCreated function.



    /**
     * Applies a filter to the current data type.
     */
    public void applyFilter()
    {
        int minFilterVal = NO_FILTER_VAL;
        int maxFilterVal = NO_FILTER_VAL;

        // ---------- Get values from UI components ----------
        // Get the index of the current data type value.
        int dataTypeIndex = dataTypeSpinner.getSelectedItemPosition();
        // Get the min filter value from the EditText.
        String minValString = minValEditText.getText().toString();
        if( !minValString.equals("") )
        {
            minFilterVal = Integer.parseInt( minValString );
        }
        // Get the max filter value from the EditText.
        String maxValString = maxValEditText.getText().toString();
        if( !maxValString.equals("") )
        {
            maxFilterVal = Integer.parseInt( maxValString );
        }


        // ---------- Check the retrieved values for errors. ----------
        boolean validRangeGiven = true;

        // Check if both filter values are not set.
        if( minFilterVal == NO_FILTER_VAL && maxFilterVal == NO_FILTER_VAL )
        {
            validRangeGiven = false;

            // State that at least one value must be given.
            Log.e(TAG, "Blank filter values given!");
        }
        // Else at least one filter value has been set.
        else
        {
            // Make any unset filter value an absolute min or max accordingly.
            if( minFilterVal == NO_FILTER_VAL )
            {
                minFilterVal = ABS_MIN_ARRAY[ dataTypeIndex ];
            }
            else if( maxFilterVal == NO_FILTER_VAL )
            {
                maxFilterVal = ABS_MAX_ARRAY[ dataTypeIndex ];
            }

            // Check if the min filter val is greater than the max filter val.
            if( minFilterVal > maxFilterVal )
            {
                validRangeGiven = false;

                // State that min cannot be greater than max.
                Log.e(TAG, "Min is greater than max!");
            }
            // Check if the range specified by the filter values agrees with the data type.
            else if( maxFilterVal > ABS_MAX_ARRAY[dataTypeIndex] ||
                    minFilterVal < ABS_MIN_ARRAY[dataTypeIndex] )
            {
                validRangeGiven = false;

                // State that the range is invalid for the given data type.
                Log.e(TAG, "Invalid range!");
            }
            // Check if the range is the same as without ny filter
            else if( maxFilterVal == ABS_MAX_ARRAY[dataTypeIndex] &&
                    minFilterVal == ABS_MIN_ARRAY[dataTypeIndex] )
            {
                validRangeGiven = false;

                // State that the provided range is the same as without filter.
                Log.e(TAG, "Range same as without filter!");
            }
            // Check if the filter values are the same as current filter values.

            else if( checkIfSameFilterValues( dataTypeIndex, minFilterVal, maxFilterVal ) )
            {
                validRangeGiven = false;

                // State that the provided range is the same as the current filter.
                Log.e(TAG, "Range same as current filter!");
            }
        }

        // ---------- Try applying the filter if range was valid. ----------
        // Check if filter should be applied.
        if( validRangeGiven )
        {
            setFilterValues( dataTypeIndex, minFilterVal, maxFilterVal, true );
            setEditTexts( String.valueOf( minFilterVal ), String.valueOf( maxFilterVal ) );
        }
        // Else don't apply any filter and populate the EditText boxes with already existing
        // filter values. If the filters aren't set, the EditTexts will be cleared.
        else
        {
            if( dataTypeIndex == CHM_INDEX )
            {
                displayFilterValues( getString( R.string.chm_filter_set ),
                        getString( R.string.chm_filter_min ), getString( R.string.chm_filter_max ) );
            }
            else if( dataTypeIndex == DEM_INDEX )
            {
                displayFilterValues( getString( R.string.dem_filter_set ),
                        getString( R.string.dem_filter_min ), getString( R.string.dem_filter_max ) );
            }
            else if( dataTypeIndex == AGB_INDEX )
            {
                displayFilterValues( getString( R.string.agb_filter_set ),
                        getString( R.string.agb_filter_min ), getString( R.string.agb_filter_max ) );
            }
        }
    }



    /**
     * Checks to see if the given filter is already applied.
     */
    private boolean checkIfSameFilterValues( int dataTypeIndex, int minFilterVal, int maxFilterVal )
    {
        return
            ( dataTypeIndex == CHM_INDEX &&
                sharedPreferences.getBoolean( getString( R.string.chm_filter_set ), false  ) &&
                minFilterVal == sharedPreferences.getInt( getString( R.string.chm_filter_min ), NO_FILTER_VAL ) &&
                maxFilterVal == sharedPreferences.getInt( getString( R.string.chm_filter_max ), NO_FILTER_VAL ) ) ||
            ( dataTypeIndex == DEM_INDEX &&
                sharedPreferences.getBoolean( getString( R.string.dem_filter_set ), false  ) &&
                minFilterVal == sharedPreferences.getInt( getString( R.string.dem_filter_min ), NO_FILTER_VAL ) &&
                maxFilterVal == sharedPreferences.getInt( getString( R.string.dem_filter_max ), NO_FILTER_VAL ) ) ||
            ( dataTypeIndex == AGB_INDEX &&
                sharedPreferences.getBoolean( getString( R.string.agb_filter_set ), false  ) &&
                minFilterVal == sharedPreferences.getInt( getString( R.string.agb_filter_min ), NO_FILTER_VAL ) &&
                maxFilterVal == sharedPreferences.getInt( getString( R.string.agb_filter_max ), NO_FILTER_VAL ) );
    }



    /**
     * Clears the data filter of the current data type.
     */
    private void clearFilter()
    {
        // Get the index of the current data type value.
        int dataTypeIndex = dataTypeSpinner.getSelectedItemPosition();

        // Remove the filter values for the given data type.
        setFilterValues( dataTypeIndex, NO_FILTER_VAL, NO_FILTER_VAL, false );

        // Clear the EditTexts that showed the filter values.
        setEditTexts( EMPTY_STRING, EMPTY_STRING );
    }



    /**
     * Sets the filter values for the given data type.
     * Used for both applying and clearing filters.
     */
    private void setFilterValues( int dataTypeIndex, int minVal, int maxVal, boolean isApplyingFilter )
    {
        if( dataTypeIndex == CHM_INDEX )
        {
            sharedPreferences.edit().putBoolean( getString( R.string.chm_filter_set ), isApplyingFilter ).commit();
            sharedPreferences.edit().putInt( getString( R.string.chm_filter_min ), minVal ).commit();
            sharedPreferences.edit().putInt( getString( R.string.chm_filter_max ), maxVal ).commit();
        }
        else if( dataTypeIndex == DEM_INDEX )
        {
            sharedPreferences.edit().putBoolean( getString( R.string.dem_filter_set ), isApplyingFilter ).commit();
            sharedPreferences.edit().putInt( getString( R.string.dem_filter_min ), minVal ).commit();
            sharedPreferences.edit().putInt( getString( R.string.dem_filter_max ), maxVal ).commit();
        }
        else if( dataTypeIndex == AGB_INDEX )
        {
            sharedPreferences.edit().putBoolean( getString( R.string.agb_filter_set ), isApplyingFilter ).commit();
            sharedPreferences.edit().putInt( getString( R.string.agb_filter_min ), minVal ).commit();
            sharedPreferences.edit().putInt( getString( R.string.agb_filter_max ), maxVal ).commit();
        }
    }



    /**
     * Displays filter values for the given data type, if the corresponding filter has been set.
     */
    public void displayFilterValues( String filterSetKey, String minValKey, String maxValKey )
    {
        // Check if the filter for the given data type has been set.
        if( sharedPreferences.getBoolean( filterSetKey, false ) )
        {
            setEditTexts( String.valueOf( sharedPreferences.getInt( minValKey, NO_FILTER_VAL ) ),
                    String.valueOf( sharedPreferences.getInt( maxValKey, NO_FILTER_VAL ) ) );
        }
        // Else the filter is not set and the EditTexts should be cleared.
        else
        {
            setEditTexts( EMPTY_STRING, EMPTY_STRING );
        }
    }



    /**
     * Applies a filter to the current data type.
     */
    public void setEditTexts( String minValText, String maxValText )
    {
        minValEditText.setText( minValText, TextView.BufferType.EDITABLE );
        maxValEditText.setText( maxValText, TextView.BufferType.EDITABLE );
    }



    /**
     * Returns the app to the Action Menu.
     * Adds or shows the Action Menu and removes the Data Filter.
     * Also closes the keyboard if it's currently open.
     */
    private void openActionMenu()
    {
        // Close the keyboard, if it's currently open.
        try
        {
            InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService( getContext().INPUT_METHOD_SERVICE );
            imm.hideSoftInputFromWindow( mainActivity.getCurrentFocus().getWindowToken(), 0 );
        }
        catch( Exception e )
        {
            // Keyboard was not already open!
        }

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
        if( fragmentManager.findFragmentByTag( "data_filter" ) != null )
        {
            fragmentTransaction.remove( fragmentManager.findFragmentByTag( "data_filter" ) );
        }

        fragmentTransaction.commit();
    }


} // End of Filter Manager.
