package com.example.biomapper;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Provides an interface for the user to select a point on the map they want
 * to be their "region of interest."
 */
public class RoiManager extends Fragment
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
        View view = inflater.inflate( R.layout.fragment_roi_manager, container, false );
        return view;
    }



    /**
     *
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState )
    {
        //
        if( savedInstanceState == null )
        {
            View mapView = mainActivity.baseMap.getView();
            ViewGroup baseMapParent = (ViewGroup) mainActivity.baseMap.getView().getParent();
            ViewGroup roiParent = (ViewGroup) getView().getParent();
            if( baseMapParent != null && roiParent != null)
            {
                // Get indexes for removing/adding the map view.
                int oldMapIndex = baseMapParent.indexOfChild( mapView );
                int newMapIndex = roiParent.indexOfChild( mainActivity.findViewById( R.id.roi_manager_container ) );

                // Remove the view from the Main Map.
                baseMapParent.removeViewAt( oldMapIndex );

                // Create parameters for adding the view.
                FrameLayout.LayoutParams viewParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );

                final View toolbar = mainActivity.findViewById( R.id.roi_manager_toolbar );
                // - - - TODO Make it so the Base Map margins are ynamically based on the toolbar and accept/cancel bar - - -
                /*
                mainActivity.runOnUiThread(
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                viewParams.topMargin = toolbar.getHeight();
                            }
                        }
                );*/
                // - - - This line to be removed after margin obtained dynamically - - -
                viewParams.topMargin = 165;//toolbar.getHeight();

                final View acceptCancelBar = mainActivity.findViewById( R.id.accept_cancel_bar );
                /*
                mainActivity.runOnUiThread(
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                viewParams.bottomMargin = acceptCancelBar.getHeight();
                            }
                        }
                );*/
                // - - - This line to be removed after margin obtained dynamically - - -
                viewParams.bottomMargin = 190;//acceptCancelBar.getHeight();

                // Add the Base Map view to the Roi Manager
                roiParent.addView( mapView, 0, viewParams );
            }
        }

        // Create the toolbar.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.roi_manager_toolbar);

        // Add a back button to the toolbar and make it functional.
        toolbar.setNavigationIcon(getContext().getDrawable(R.drawable.toolbar_back_icon));
        toolbar.setNavigationOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        openActionMenu();
                    }
                }
        );
    }



    /**
     * Returns the app to the Action Menu.
     * "Moves" the Base Map back to the Main Map.
     * Also shows the Action Menu and removes the ROI Manager.
     */
    private void openActionMenu()
    {
        // ----- Move Base Map view back to the Main Map. -----
        View mapView = mainActivity.baseMap.getView();
        ViewGroup mainMapParent = ((ViewGroup) mainActivity.findViewById( R.id.main_map_container ).getParent() );
        ViewGroup roiParent = (ViewGroup) getView().getParent();
        if( mainMapParent != null && roiParent != null)
        {
            // Get indexes for removing/adding the map view.
            int oldMapIndex = roiParent.indexOfChild( mapView );

            // Remove the view from the ROI Manger.
            roiParent.removeViewAt( oldMapIndex );

            // Create parameters for adding the view.
            FrameLayout.LayoutParams viewParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            viewParams.topMargin = 0;
            viewParams.bottomMargin = 0;

            // Add the Base Map view to the Main Map.
            mainMapParent.addView( mapView, 0, viewParams );
        }

        // ----- Begin necessary fragment transactions. -----
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Ensure that the base map is hidden.
        if( mainActivity.baseMap != null )
        {
            fragmentTransaction.hide( mainActivity.baseMap );
        }

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

        // If the Roi Selection fragment exists, hide it.
        if( fragmentManager.findFragmentByTag("roi_manager") != null )
        {
            fragmentTransaction.remove( fragmentManager.findFragmentByTag("roi_manager") );
        }

        fragmentTransaction.commit();
    }

}