/*
 * Copyright (c) 2015. John C Quinn, All Rights Reserved.
 */

package us.gravwith.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StashGalleryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StashGalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StashGalleryFragment extends Fragment implements FragmentManager.OnBackStackChangedListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private final String TAG = "StashGalleryFragment";
    private final boolean VERBOSE = true;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // A handle to the main screen view
    View mMainView;


    // Tracks whether Fragments are displaying side-by-side
    boolean mSideBySide;

    // Tracks whether navigation should be hidden
    boolean mHideNavigation;

    // Tracks whether the app is in full-screen mode
    boolean mFullScreen;

    // Tracks the number of Fragments on the back stack
    int mPreviousStackCount;

    private Context context;

    private OnFragmentInteractionListener mListener;

    // An instance of the status broadcast receiver
    DownloadStateReceiver mDownloadStateReceiver;

    // Instantiates a new broadcast receiver for handling Fragment state
    private FragmentDisplayer mFragmentDisplayer = new FragmentDisplayer();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StashGalleryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StashGalleryFragment newInstance(String param1, String param2) {
        StashGalleryFragment fragment = new StashGalleryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public StashGalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle stateBundle) {
        if (VERBOSE) Log.v(TAG,"entering onCreate...");

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Activity mActivity = getActivity();
        context = mActivity;

        // Sets fullscreen-related flags for the display
        mActivity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        // Calls the super method (required)
        super.onCreate(stateBundle);


        /*
         * Creates an intent filter for DownloadStateReceiver that intercepts broadcast Intents
         */

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);

        // Sets the filter's category to DEFAULT
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        // Instantiates a new DownloadStateReceiver
        mDownloadStateReceiver = new DownloadStateReceiver();

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(context).registerReceiver(
                mDownloadStateReceiver,
                statusIntentFilter);

        /*
         * Creates intent filters for the FragmentDisplayer
         */

        // One filter is for the action ACTION_VIEW_IMAGE
        IntentFilter displayerIntentFilter = new IntentFilter(
                Constants.ACTION_VIEW_IMAGE);

        // Registers the receiver
        LocalBroadcastManager.getInstance(context).registerReceiver(
                mFragmentDisplayer,
                displayerIntentFilter);

        // Creates a second filter for ACTION_ZOOM_IMAGE
        displayerIntentFilter = new IntentFilter(Constants.ACTION_ZOOM_IMAGE);

        // Registers the receiver
        LocalBroadcastManager.getInstance(context).registerReceiver(
                mFragmentDisplayer,
                displayerIntentFilter);

        // Gets an instance of the support library FragmentManager
  //      FragmentManager localFragmentManager = getFragmentManager();

        /*
         * Detects if side-by-side display should be enabled. It's only available on xlarge and
         * sw600dp devices (for example, tablets). The setting in res/values/ is "false", but this
         * is overridden in values-xlarge and values-sw600dp.
         */
        mSideBySide = getResources().getBoolean(R.bool.sideBySide);

        /*
         * Detects if hiding navigation controls should be enabled. On xlarge andsw600dp, it should
         * be false, to avoid having the user enter an additional tap.
         */
        mHideNavigation = getResources().getBoolean(R.bool.hideNavigation);

        /*
         * Adds the back stack change listener defined in this Activity as the listener for the
         * FragmentManager. See the method onBackStackChanged().
         */
    /*    localFragmentManager.addOnBackStackChangedListener(this);

        // If the incoming state of the Activity is null, sets the initial view to be thumbnails
        if (null == stateBundle) {

            // Starts a Fragment transaction to track the stack
            FragmentTransaction localFragmentTransaction = localFragmentManager
                    .beginTransaction();

            // Adds the PhotoThumbnailFragment to the host View
            localFragmentTransaction.add(R.id.fragmentHost,
                    new PhotoThumbnailFragment(), Constants.THUMBNAIL_FRAGMENT_TAG);

            // Commits this transaction to display the Fragment
            localFragmentTransaction.commit();

            // The incoming state of the Activity isn't null.
        } else {

            // Gets the previous state of the fullscreen indicator
            mFullScreen = stateBundle.getBoolean(Constants.EXTENDED_FULLSCREEN);

            // Sets the fullscreen flag to its previous state
            setFullScreen(mFullScreen);

            // Gets the previous backstack entry count.
            mPreviousStackCount = localFragmentManager.getBackStackEntryCount();
        }*/

        if (VERBOSE) Log.v(TAG,"exiting onCreate...");
    }

    @Override
    public void onDestroy() {
        if (VERBOSE) Log.v(TAG,"entering onDestroy...");
        // If the DownloadStateReceiver still exists, unregister it and set it to null
        if (mDownloadStateReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mDownloadStateReceiver);
            mDownloadStateReceiver = null;
        }

        // Unregisters the FragmentDisplayer instance
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.mFragmentDisplayer);

        // Sets the main View to null
        mMainView = null;

        super.onDestroy();

        if (VERBOSE) Log.v(TAG,"exiting onDestroy...");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {

        PhotoManager.cancelAll();
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.v(TAG,"entering onCreateView...");
        // Inflate the layout for this fragment

        mMainView = inflater.inflate(R.layout.fragmenthost, container, false);

        if (VERBOSE) Log.v(TAG,"exiting onCreateView...");
        return mMainView;
    }

    @Override
    public void onViewCreated(View view, Bundle stateBundle) {
        super.onViewCreated(view, stateBundle);

        FragmentManager localFragmentManager = getFragmentManager();
        localFragmentManager.addOnBackStackChangedListener(this);

        // If the incoming state of the Activity is null, sets the initial view to be thumbnails
        if (null == stateBundle) {

            // Starts a Fragment transaction to track the stack
            FragmentTransaction localFragmentTransaction = localFragmentManager
                    .beginTransaction();

            // Adds the PhotoThumbnailFragment to the host View
            localFragmentTransaction.add(R.id.fragmentHost,
                    new PhotoThumbnailFragment(), Constants.THUMBNAIL_FRAGMENT_TAG);

            // Commits this transaction to display the Fragment
            localFragmentTransaction.commit();

            // The incoming state of the Activity isn't null.
        } else {

            // Gets the previous state of the fullscreen indicator
            mFullScreen = stateBundle.getBoolean(Constants.EXTENDED_FULLSCREEN);

            // Sets the fullscreen flag to its previous state
            setFullScreen(mFullScreen);

            // Gets the previous backstack entry count.
            mPreviousStackCount = localFragmentManager.getBackStackEntryCount();
        }

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement BaseFragmentInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    /**
     * This class uses the BroadcastReceiver framework to detect and handle status messages from
     * the service that downloads URLs.
     */
    private class DownloadStateReceiver extends BroadcastReceiver {

        private DownloadStateReceiver() {

            // prevents instantiation by other packages.
        }
        /**
         *
         * This method is called by the system when a broadcast Intent is matched by this class'
         * intent filters
         *
         * @param context An Android context
         * @param intent The incoming broadcast Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            /*
             * Gets the status from the Intent's extended data, and chooses the appropriate action
             */
            switch (intent.getIntExtra(Constants.EXTENDED_DATA_STATUS,
                    Constants.STATE_ACTION_COMPLETE)) {

                // Logs "started" state
                case Constants.STATE_ACTION_STARTED:
                    if (Constants.LOGD) {

                        Log.d(TAG, "State: STARTED");
                    }
                    break;
                // Logs "connecting to network" state
                case Constants.STATE_ACTION_CONNECTING:
                    if (Constants.LOGD) {

                        Log.d(TAG, "State: CONNECTING");
                    }
                    break;
                // Logs "parsing the RSS feed" state
                case Constants.STATE_ACTION_PARSING:
                    if (Constants.LOGD) {

                        Log.d(TAG, "State: PARSING");
                    }
                    break;
                // Logs "Writing the parsed data to the content provider" state
                case Constants.STATE_ACTION_WRITING:
                    if (Constants.LOGD) {

                        Log.d(TAG, "State: WRITING");
                    }
                    break;
                // Starts displaying data when the RSS download is complete
                case Constants.STATE_ACTION_COMPLETE:
                    // Logs the status
                    if (Constants.LOGD) {

                        Log.d(TAG, "State: COMPLETE");
                    }

                    // Finds the fragment that displays thumbnails
                    PhotoThumbnailFragment localThumbnailFragment =
                            (PhotoThumbnailFragment) getFragmentManager().findFragmentByTag(
                                    Constants.THUMBNAIL_FRAGMENT_TAG);

                    // If the thumbnail Fragment is hidden, don't change its display status
                    if ((localThumbnailFragment == null)
                            || (!localThumbnailFragment.isVisible()))
                        return;

                    // Indicates that the thumbnail Fragment is visible
                    localThumbnailFragment.setLoaded(true);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * This class uses the broadcast receiver framework to detect incoming broadcast Intents
     * and change the currently-visible fragment based on the Intent action.
     * It adds or replaces Fragments as necessary, depending on how much screen real-estate is
     * available.
     */
    private class FragmentDisplayer extends BroadcastReceiver {

        // Default null constructor
        public FragmentDisplayer() {

            // Calls the constructor for BroadcastReceiver
            super();
        }
        /**
         * Receives broadcast Intents for viewing or zooming pictures, and displays the
         * appropriate Fragment.
         *
         * @param context The current Context of the callback
         * @param intent The broadcast Intent that triggered the callback
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: called");

            // Declares a local FragmentManager instance
            FragmentManager fragmentManager1;

            // Declares a local instance of the Fragment that displays photos
            PhotoFragment photoFragment;

            // Stores a string representation of the URL in the incoming Intent
            String urlString;

            // If the incoming Intent is a request is to view an image
            if (intent.getAction().equals(Constants.ACTION_VIEW_IMAGE)) {

                // Gets an instance of the support library fragment manager
                fragmentManager1 = getFragmentManager();

                // Gets a handle to the Fragment that displays photos
                photoFragment =
                        (PhotoFragment) fragmentManager1.findFragmentByTag(
                                Constants.PHOTO_FRAGMENT_TAG
                        );

                // Gets the URL of the picture to display
                urlString = intent.getStringExtra(Constants.KEY_S3_KEY);

                // If the photo Fragment exists from a previous display
                if (null != photoFragment) {

                    // If the incoming URL is not already being displayed
                    if (!urlString.equals(photoFragment.getImageKeyString())) {

                        // Sets the Fragment to use the URL from the Intent for the photo
                        photoFragment.setPhoto(Constants.KEY_S3_STASH_DIRECTORY,urlString,false);

                        // Loads the photo into the Fragment
                        photoFragment.loadPhoto();
                    }

                    // If the Fragment doesn't already exist
                } else {
                    // Instantiates a new Fragment
                    photoFragment = new PhotoFragment();

                    // Sets the Fragment to use the URL from the Intent for the photo
                    photoFragment.setPhoto(Constants.KEY_S3_STASH_DIRECTORY,urlString,false);

                    // Starts a new Fragment transaction
                    FragmentTransaction localFragmentTransaction2 =
                            fragmentManager1.beginTransaction();

                    // If the fragments are side-by-side, adds the photo Fragment to the display
                    if (mSideBySide) {
                        localFragmentTransaction2.add(
                                R.id.fragmentHost,
                                photoFragment,
                                Constants.PHOTO_FRAGMENT_TAG
                        );
                    /*
                     * If the Fragments are not side-by-side, replaces the current Fragment with
                     * the photo Fragment
                     */
                    } else {
                        localFragmentTransaction2.replace(
                                R.id.fragmentHost,
                                photoFragment,
                                Constants.PHOTO_FRAGMENT_TAG);
                    }

                    // Don't remember the transaction (sets the Fragment backstack to null)
                    localFragmentTransaction2.addToBackStack(null);

                    // Commits the transaction
                    localFragmentTransaction2.commit();
                }

                // If not in side-by-side mode, sets "full screen", so that no controls are visible
                if (!mSideBySide) setFullScreen(true);

            /*
             * If the incoming Intent is a request to zoom in on an existing image
             * (Notice that zooming is only supported on large-screen devices)
             */
            } else if (intent.getAction().equals(Constants.ACTION_ZOOM_IMAGE)) {

                // If the Fragments are being displayed side-by-side
                if (mSideBySide) {

                    // Gets another instance of the FragmentManager
                    FragmentManager localFragmentManager2 = getFragmentManager();

                    // Gets a thumbnail Fragment instance
                    PhotoThumbnailFragment localThumbnailFragment =
                            (PhotoThumbnailFragment) localFragmentManager2.findFragmentByTag(
                                    Constants.THUMBNAIL_FRAGMENT_TAG);

                    // If the instance exists from a previous display
                    if (null != localThumbnailFragment) {

                        // if the existing instance is visible
                        if (localThumbnailFragment.isVisible()) {

                            // Starts a fragment transaction
                            FragmentTransaction localFragmentTransaction2 =
                                    localFragmentManager2.beginTransaction();

                            /*
                             * Hides the current thumbnail, clears the backstack, and commits the
                             * transaction
                             */
                            localFragmentTransaction2.hide(localThumbnailFragment);
                            localFragmentTransaction2.addToBackStack(null);
                            localFragmentTransaction2.commit();

                            // If the existing instance is not visible, display it by going "Back"
                        } else {

                            // Pops the back stack to show the previous Fragment state
                            localFragmentManager2.popBackStack();
                        }
                    }

                    // Removes controls from the screen
                    setFullScreen(true);
                }
            }
        }
    }

    /*
    * A callback invoked when the task's back stack changes. This allows the app to
    * move to the previous state of the Fragment being displayed.
    *
    */
    @Override
    public void onBackStackChanged() {

        // Gets the previous global stack count
        int previousStackCount = mPreviousStackCount;

        // Gets a FragmentManager instance
        FragmentManager localFragmentManager = getFragmentManager();

        // Sets the current back stack count
        int currentStackCount = localFragmentManager.getBackStackEntryCount();

        // Re-sets the global stack count to be the current count
        mPreviousStackCount = currentStackCount;

        /*
         * If the current stack count is less than the previous, something was popped off the stack
         * probably because the user clicked Back.
         */
        boolean popping = currentStackCount < previousStackCount;
        Log.d(TAG, "backstackchanged: popping = " + popping);

        // When going backwards in the back stack, turns off full screen mode.
        if (popping) {
            setFullScreen(false);
        }
    }


    /**
     * Sets full screen mode on the device, by setting parameters in the current
     * window and View
     * @param fullscreen
     */
    public void setFullScreen(boolean fullscreen) {
        // If full screen is set, sets the fullscreen flag in the Window manager
        getActivity().getWindow().setFlags(
                fullscreen ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Sets the global fullscreen flag to the current setting
        mFullScreen = fullscreen;

        // If the platform version is Android 3.0 (Honeycomb) or above
        if (Build.VERSION.SDK_INT >= 11) {

            // Sets the View to be "low profile". Status and navigation bar icons will be dimmed
            int flag = fullscreen ? View.SYSTEM_UI_FLAG_LOW_PROFILE : 0;

            // If the platform version is Android 4.0 (ICS) or above
            if (Build.VERSION.SDK_INT >= 14 && fullscreen) {

                // Hides all of the navigation icons
                flag |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // Applies the settings to the screen View
            mMainView.setSystemUiVisibility(flag);

            // If the user requests a full-screen view, hides the Action Bar.
            if ( fullscreen ) {
                getActivity().getActionBar().hide();
            } else {
                getActivity().getActionBar().show();
            }
        }
    }

}
