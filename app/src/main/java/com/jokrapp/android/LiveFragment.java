package com.jokrapp.android;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;
import com.jokrapp.android.SQLiteDbContract.LiveThreadInfoEntry;


/**
 * Author/Copyright John C. Quinn, All Rights Reserved.
 *
 * class 'LiveFragment'
 *
 * contains the live portion of the app
 *
 * //todo use a disk cache for storing thread topics
 */
public class LiveFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final boolean VERBOSE = true;
    private static final String TAG = "LiveFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_ADAPTER = "ada";

    private final int LIVE_LOADER_ID = 1;
    private int numberOfThreads;

    // TODO: Rename and change types of parameters
    private int currentThread;

    private VerticalViewPager threadPager;
    private FragmentStatePagerAdapter mAdapter;

  //  private Cursor data;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param messageCount the current number of messages that have been recieved.
     * @param threadPosition your thread's current position in live.
     * @return A new instance of fragment LocalFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LiveFragment newInstance(String messageCount, String threadPosition) {
        LiveFragment fragment = new LiveFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, messageCount);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * method 'createThread'
     *
     * displays a view allowing the user to enter the necessary information to create a thread
     *
     */
    /*public void createThread(MainActivity activity) {
        activity.disableScrolling();
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout root = (FrameLayout)activity.findViewById(R.id.live_root_layout);
        View v = inflater.inflate(R.layout.live_post,
                root,
                false);

        getPostButtonListener(this).setCreateView(v);
        v.findViewById(R.id.button_cancel_post).setOnClickListener(getPostButtonListener(this));
        v.findViewById(R.id.button_confirm_post).setOnClickListener(getPostButtonListener(this));
         root.addView(v,root.getChildCount());
    }*/

    public LiveFragment() {
        // Required empty public constructor
    }

/***************************************************************************************************
* LIFECYCLE METHODS
*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onCreate...");

        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
        if (savedInstanceState != null) {
        } else {
            }
       // mAdapter = new LiveAdapter(getFragmentManager());

        currentThread = 0;
        if (VERBOSE) Log.v(TAG,"initializing loader at id " + LIVE_LOADER_ID);
        getLoaderManager().initLoader(LIVE_LOADER_ID, null, this);
        if (VERBOSE) Log.v(TAG,"exiting onCreate...");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (VERBOSE) Log.v(TAG,"entering onSaveInstanceState...");

        if (VERBOSE) Log.v(TAG,"exiting onSaveInstanceState...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) Log.v(TAG,"entering onDestroy...");


        if (VERBOSE) Log.v(TAG,"destroying loader at id " + LIVE_LOADER_ID);
        getLoaderManager().destroyLoader(LIVE_LOADER_ID);

        if (VERBOSE) Log.v(TAG,"exiting onDestroy...");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.v(TAG,"entering onCreateView...");
        //ImageView imageView = (ImageView)getActivity().findViewById(R.id.imageView3);
        //MainActivity activity;
        //if (getActivity() instanceof MainActivity) {
        //    activity.loadBitmap(activity.theImages.get(5),imageView);
        //    activity = (MainActivity)getActivity();
        //}


        // Inflate the layout for this fragment
        //inflater.inflate(R.layout.fragment_live_thread,threadPager,false);//
        // FrameLayout rootLayout = (FrameLayout)rootView.findViewById(R.id.live_root_layout);
        // View threadView = inflater.inflate(R.layout.fragment_live_thread,rootLayout,false);
        // rootLayout.addView(threadView);

        View rootView = inflater.inflate(R.layout.fragment_live,container,false);
        rootView.findViewById(R.id.button_live_refresh).setOnClickListener(getButtonListener(this));
        rootView.findViewById(R.id.button_new_thread).setOnClickListener(getButtonListener(this));
        ((SeekBar)rootView.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(getButtonListener(this));

        if (VERBOSE) Log.v(TAG,"exiting onCreateView...");
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onViewCreated...");

        //    ImageButton upArrow = (ImageButton)view.findViewById(R.id.upArrow);
        //   ImageButton downArrow = (ImageButton)view.findViewById(R.id.downArrow);
        //  upArrow.setOnClickListener(getButtonListener(this));
        //  downArrow.setOnClickListener(getButtonListener(this));

        threadPager = (VerticalViewPager)view.findViewById(R.id.live_thread_pager);
       // if (mAdapter != null) {
//            threadPager.setAdapter(mAdapter);
  //      }

        if (VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity)activity).sendMsgRequestLiveThreads();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

/***************************************************************************************************
 * USER INTERACTION
 */
    /**
     * class 'livePostButtonListener'
     *
     * special class that listens to the live_write_post buttons, and gets data
     */
    /**
     * class 'liveButtonListener
     *
     * listens to all the general live thread interactions
     */
    public class LiveButtonListener implements SeekBar.OnSeekBarChangeListener, View.OnClickListener,ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG,"OnClickRegistered..." + v.toString());
            }

            switch (v.getId()) {

                case R.id.button_live_refresh:

                    ((MainActivity)getActivity()).sendMsgRequestLiveThreads();
                    break;


                case R.id.button_new_thread:
                    setSeekMode(v);
                    break;
            }
        }


        final int OPEN_CAMERA_VALUE = 0;
        final int STARTING_VALUE = 50;
        final int OPEN_STASH_VALUE = 100;
        int value = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            value = progressValue;
            switch (progressValue) {
                case OPEN_CAMERA_VALUE:
                    Toast.makeText(getActivity(),"Opening camera...",Toast.LENGTH_SHORT).show();
                    ((MainActivity)getActivity()).takeLivePicture();
                    break;

                case OPEN_STASH_VALUE:
                    Toast.makeText(getActivity(),"Opening stash...",Toast.LENGTH_SHORT).show();
                    break;
                case STARTING_VALUE:
                    //do nothing. possibly hide the seekbar?
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        private SeekBar seekBar;
        private int destValue;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //todo animator should interpolate the seekbar to the closest node (0,50,100)
            final int ANIMATION_SPEED_FACTOR = 1000;
            final int ANIMATION_ACCEL_FACTOR = 3;

            this.seekBar = seekBar;

            if (value < 20) {
                destValue = OPEN_CAMERA_VALUE;
            } else if (value > 80) {
                destValue = OPEN_STASH_VALUE;
            } else {
                destValue = STARTING_VALUE;
            }
            double distance = Math.abs(destValue - value);

            double speedRatio = (distance / seekBar.getMax());
            Log.d(TAG,"The distance is " + distance + " and the speedRatio is " + speedRatio);
            Log.d(TAG, "The adjusted duration is " + Math.round(speedRatio * ANIMATION_SPEED_FACTOR));

            ValueAnimator anim = ValueAnimator.ofInt(value, destValue);
            anim.setInterpolator(new AccelerateInterpolator(ANIMATION_ACCEL_FACTOR));
            anim.setDuration(Math.round(speedRatio * ANIMATION_SPEED_FACTOR));
            anim.addUpdateListener(this);
            anim.start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int animProgress = (Integer) animation.getAnimatedValue();
            seekBar.setProgress(animProgress);

            if (animProgress == destValue) {
                setSeekMode(null);
            }
        }

        private void setSeekMode(View v) {
            if (v != null) { //button_new_thread was pressed, set seek mode
             ViewGroup group = (ViewGroup)v.getParent();
                group.findViewById(R.id.button_new_thread).setVisibility(View.INVISIBLE);
                group.findViewById(R.id.live_thread_number).setVisibility(View.INVISIBLE);
                SeekBar bar = ((SeekBar)group.findViewById(R.id.seekBar));
                bar.setVisibility(View.VISIBLE);
                bar.setOnSeekBarChangeListener(this);
            } else {
                if (getView() != null) {
                    View group = getView();
                    group.findViewById(R.id.button_new_thread).setVisibility(View.VISIBLE);
                    group.findViewById(R.id.live_thread_number).setVisibility(View.VISIBLE);
                    SeekBar bar = ((SeekBar)group.findViewById(R.id.seekBar));
                    bar.setProgress(STARTING_VALUE);
                    value = STARTING_VALUE;
                    bar.setVisibility(View.INVISIBLE);
                    bar.setOnSeekBarChangeListener(null);
                }
            }
        }

    }

    /**
     * static method getButtonListener
     *
     * this is used to create a singleton reference to a buttonlistener, allowing one
     * buttonlistener to handle all button interactions
     *
     *
     * @param parent the context in which this functions
     * @return a ButtonListener to be used by all the buttons in CameraFragment
     */
    private static WeakReference<LiveButtonListener> buttonListenerReference;

    public static LiveButtonListener getButtonListener(LiveFragment parent) {
        if (buttonListenerReference == null) {
            buttonListenerReference = new WeakReference<>(parent.new LiveButtonListener());
        }

        return buttonListenerReference.get();
    }



    private class LiveAdapter extends FragmentStatePagerAdapter {
        private MatrixCursor data;
        private Handler liveThreadAdapterHandler;
        public LiveAdapter(FragmentManager fm, Cursor data) {
            super(fm);
            this.data = createMatrixCursorFromCursor(data);
            if (VERBOSE) {
                Log.v(TAG, "LiveAdapter created...");
            }

            liveThreadAdapterHandler = new Handler();

            if (data == null) {
                Log.v(TAG,"incoming cursor was null");
            } else {
                Log.v(TAG, "Live table contains " + data.getCount() + " threads...");
            }


        }

        @Override
        public Fragment getItem(int i) {
            Log.d(TAG, "getting fragment at position " + i);
            String name,title,fileName;
            data.moveToPosition(i);
            name = data.getString(data.getColumnIndexOrThrow(LiveThreadInfoEntry.COLUMN_NAME_NAME));
            title = data.getString(data.getColumnIndexOrThrow(LiveThreadInfoEntry.COLUMN_NAME_TITLE));
            fileName = data.getString(data.getColumnIndexOrThrow(LiveThreadInfoEntry.COLUMN_NAME_FILEPATH));

            if (getView() != null) {
            ((TextView)getView().findViewById(R.id.live_thread_number)).setText(String.valueOf(i));
            }

            LiveThreadFragment f =  LiveThreadFragment.newInstance(i, name, title, fileName);
            f.setProgressHandler(liveThreadAdapterHandler);
            return f;
        }

        @Override
        public void startUpdate(ViewGroup container) {
            super.startUpdate(container);
        }

        @Override
        public int getCount() {
            if (data == null) {
                return 0;
            } else {
                return data.getCount();
            }
        }

        private MatrixCursor createMatrixCursorFromCursor(Cursor c) {
            if (VERBOSE) Log.v(TAG,"enter createMatrixCursorFromCursor...");
            MatrixCursor newCursor = null;
            String[] columns = c.getColumnNames();
            MatrixCursor.RowBuilder b;
            newCursor = new MatrixCursor(columns, 1);
            while(c.moveToNext()) {
                b = newCursor.newRow();
                    /*
                     * Each row is built left to right by adding the columns
                     */
                for (String col : columns) {
                    // in case all columns are of string type. But if they are
                    // different then see my comment below
                    switch (c.getType(c.getColumnIndex(col))) {
                        case Cursor.FIELD_TYPE_STRING:
                            b.add(c.getString(c.getColumnIndex(col)));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            b.add(c.getInt(c.getColumnIndex(col)));
                            break;
                        case Cursor.FIELD_TYPE_NULL:
                            b.add(null);
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            b.add(c.getBlob(c.getColumnIndex(col)));
                            break;
                    }
                }
                // invoke your listener here with newCursor
                Log.d(TAG, "row added: " + newCursor.toString());
            }

            if (VERBOSE) Log.v(TAG,"exiting createMatrixCursorFromCursor...");
            return newCursor;
        }

    }

    /**************************************************************************************************
     * CONTENT LOADING
      */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (VERBOSE) Log.v(TAG,"enter onCreateLoader...");
        
        String[] projection = {
                SQLiteDbContract.LiveThreadInfoEntry.COLUMN_ID,
                SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_NAME,
                SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_TITLE,
                SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_FILEPATH
        };

        if (VERBOSE) Log.v(TAG,"loader created.");
        if (VERBOSE) Log.v(TAG,"exit onCreateLoader...");
        return new CursorLoader(getActivity(),
                FireFlyContentProvider.CONTENT_URI_LIVE_THREAD_INFO,
                projection,
                null,
                null,
                LiveThreadInfoEntry.COLUMN_ID);
        //sort by column ID
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (VERBOSE) Log.v(TAG,"enter onLoadFinished...");
        refreshThreadPager(data);

        if (VERBOSE) Log.v(TAG,"exit onLoadFinished...");
    }




    /**
     * method 'refreshThreadPager'
     *
     * refreshs the thread pager, and all the data in it
     *
     * @param data the new data to be loaded
     */
    public void refreshThreadPager(Cursor data) {
            threadPager.setAdapter(null);
            if (VERBOSE) Log.v(TAG,"mAdapter was null...");
            mAdapter = new LiveAdapter(getFragmentManager(), data);
            threadPager.setAdapter(mAdapter);
        Log.d(TAG, "Cursor returned has " + data.getCount() + " rows");
    }

    /**
     * method 'onLoaderReset'
     *
     * called when the data at the loader is no longer available
     *
     * @param loader the loader which was reset
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (VERBOSE) Log.v(TAG,"enter onLoaderReset...");

        threadPager = null;
        mAdapter= null;

        if (VERBOSE) Log.v(TAG,"exit onLoaderReset...");
    }
}
