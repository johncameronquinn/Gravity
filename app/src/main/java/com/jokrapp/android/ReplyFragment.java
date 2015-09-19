package com.jokrapp.android;


import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;

import java.lang.ref.WeakReference;


/**
 * Author/Copyright John C. Quinn All Rights Reserved
 * Date last modified: 2015-06-17
 *
 * A simple {@link Fragment} subclass. factory method to
 * create an instance of this fragment.
 */
public class ReplyFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private static int currentThread = 1;
    private final int REPLY_LOADER_ID = 3;

    private final boolean VERBOSE = true;
    private final String TAG = "ReplyFragment";

    SimpleCursorAdapter mAdapter;

    public static ReplyFragment newInstance(int currentThread) {
        Bundle args = new Bundle();
        args.putInt("currentThread", currentThread);

        ReplyFragment fragment = new ReplyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ReplyFragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();


        if (b != null) {
            currentThread = b.getInt("currentThread") +1;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * method 'onAttach'
     *
     * called when the fragment attachs to the activity
     * @param activity what it attached to
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (VERBOSE) Log.v(TAG,"initializing loader at id " + REPLY_LOADER_ID);
        getLoaderManager().initLoader(REPLY_LOADER_ID, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLoaderManager().destroyLoader(REPLY_LOADER_ID);
    }

    /**
     * method 'onCreateView'
     *
     * everything to create the UI goes here.
     *
     * @param inflater the layoutInflater - is the object that create views from xml files
     * @param container the parent to place the view in - in this case, the ViewPager
     * @param savedInstanceState null unless something was saved.
     * @return the view to create
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) {Log.v(TAG,"entering onCreateView...");}

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_reply, container, false);

        ListView listView = (ListView)v.findViewById(R.id.reply_list_view);

        String[] fromColumns = {SQLiteDbContract.LiveReplies.COLUMN_NAME_DESCRIPTION,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_TIME};
        int[] toViews = {R.id.reply_detail_row_text, R.id.reply_detail_row_time};

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.fragment_reply_detail_row, null,fromColumns, toViews, 0);
        listView.setAdapter(mAdapter);


        if (VERBOSE) {Log.v(TAG,"exiting onCreateView...");}
        return v;
    }

    /**
     * method 'onViewCreated'
     *
     * @param view view that was created
     * @param savedInstanceState null
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onViewCreated...");
        view.findViewById(R.id.button_new_reply).setOnClickListener(getButtonListener(this));
        view.findViewById(R.id.button_reply_refresh).setOnClickListener(getButtonListener(this));

        //anything that requires the UI to already exist goes here

        if (VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void setCurrentThread(String thread) {
        Log.i(TAG, "setting current thread to : " + thread + ".");
        ((Button)getActivity().findViewById(R.id.button_reply_refresh)).setText(thread);
        currentThread = Integer.valueOf(thread);
    }

    public void resetDisplay() {
        Log.d(TAG, "restarting loader...");
        getLoaderManager().restartLoader(REPLY_LOADER_ID,null,this);
    }

    public int getCurrentThread() { return currentThread;}

    private static WeakReference<ReplyButtonListener> buttonListenerReference;

    public static ReplyButtonListener getButtonListener(ReplyFragment parent) {
        if (buttonListenerReference == null) {
            buttonListenerReference = new WeakReference<>(parent.new ReplyButtonListener());
        }
        return buttonListenerReference.get();
    }

    /**
     * class 'replyButtonListener
     *
     * listens to all the general live thread interactions
     */
    public class ReplyButtonListener implements SeekBar.OnSeekBarChangeListener, View.OnClickListener,ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG, "OnClickRegistered..." + v.toString());
            }

            switch (v.getId()) {

                case R.id.button_reply_refresh:
                    if (isAdded()) {
                        ((MainActivity) getActivity()).sendMsgRequestReplies(currentThread);
                    }
                    resetDisplay();
                    break;


                case R.id.button_new_reply:
                    startNewReplyInputMode((MainActivity)getActivity(), v);
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
                    ((MainActivity)getActivity()).takeReplyPicture();
                    break;

                case OPEN_STASH_VALUE:

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
                ViewGroup group = (ViewGroup)getView();
                if (getView() != null) {
                    group.findViewById(R.id.button_new_reply).setVisibility(View.INVISIBLE);
                    SeekBar bar = ((SeekBar) group.findViewById(R.id.replySeekBar));
                    bar.setVisibility(View.VISIBLE);
                    bar.setProgress(STARTING_VALUE);
                    value = STARTING_VALUE;
                    bar.setOnSeekBarChangeListener(this);
                }
            } else {
                if (getView() != null) {
                    View group = getView();
                    group.findViewById(R.id.button_new_reply).setVisibility(View.VISIBLE);
                    SeekBar bar = ((SeekBar)group.findViewById(R.id.replySeekBar));
                    bar.setVisibility(View.INVISIBLE);
                    bar.setOnSeekBarChangeListener(null);
                    bar.setProgress(STARTING_VALUE);
                }
            }
        }

    }

    /***************************************************************************************************
     * REPLY MODE
     */
    private static WeakReference<ReplyModeButtonListener> buttonReplyListenerReference;

    public static ReplyModeButtonListener getReplyModeButtonListener(ReplyFragment parent) {
        if (buttonReplyListenerReference == null) {
            buttonReplyListenerReference = new WeakReference<>(parent.new ReplyModeButtonListener(parent));
        }
        return buttonReplyListenerReference.get();
    }

    /**
     * class 'ReplyMostButtonListener'
     *
     * special class that listens to the buttons inflated from the layout camera_reply_mode
     */
    public class ReplyModeButtonListener implements View.OnClickListener {

        View createReplyView;
        View plusButtonView;

        private ReplyFragment parent;

        public ReplyModeButtonListener(ReplyFragment parent) {
            this.parent = parent;
        }

        public void setCreateView(View view) {
            createReplyView = view;
        }

        public void setCreateReplyView(View v) {
            plusButtonView = v;
        }

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG,"OnClickRegistered..." + v.toString());
            }
            MainActivity activity = (MainActivity)getActivity();
            InputMethodManager imm = (InputMethodManager)activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            switch (v.getId()) {
                case R.id.button_camera_reply_mode_cancel:
                    ((ViewGroup)v.getParent().getParent()).removeView((View) v.getParent());
                    activity.removePendingLiveImage();
                    imm.hideSoftInputFromWindow(createReplyView.getWindowToken(), 0);
                    activity.enableScrolling();
                    plusButtonView.setVisibility(View.VISIBLE);
                    break;
                case R.id.button_camera_reply_mode_confirm:
                    RelativeLayout layout = (RelativeLayout) v.getParent().getParent();
                    String description = ((EditText)layout.findViewById(R.id.editText_reply_mode_comment)).getText().toString();
                    activity.setLiveCreateReplyInfo("unset", description, getCurrentThread());//todo load name from sharedpreferences
                    layout.removeView((View) v.getParent());
                    imm.hideSoftInputFromWindow(createReplyView.getWindowToken(), 0);
                    activity.enableScrolling();
                    plusButtonView.setVisibility(View.VISIBLE);
                    break;

                case R.id.button_camera_reply_mode_add_image:
                    getButtonListener(parent).setSeekMode(v);
                    ((ViewGroup) v.getParent().getParent()).removeView((View) v.getParent());
                    imm.hideSoftInputFromWindow(createReplyView.getWindowToken(), 0);
                    plusButtonView.setVisibility(View.VISIBLE);
                    activity.enableScrolling();
                    break;
            }
            plusButtonView = null;
            buttonReplyListenerReference = null;
        }

    }

    public void startNewReplyInputMode(MainActivity activity, View plusButtonView) {
        if (VERBOSE) {
            Log.v(TAG,"entering startNewReplyInputMode...");
        }
        activity.disableScrolling();
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout root = (RelativeLayout) activity.findViewById(R.id.layout_reply_root);
        View v = inflater.inflate(R.layout.camera_reply_mode,
                root,
                false);

        ReplyModeButtonListener rListener = getReplyModeButtonListener(this);
        plusButtonView.setVisibility(View.INVISIBLE);
        rListener.setCreateView(v);
        rListener.setCreateReplyView(plusButtonView);
        v.findViewById(R.id.button_camera_reply_mode_cancel).setOnClickListener(rListener);
        v.findViewById(R.id.button_camera_reply_mode_confirm).setOnClickListener(rListener);
        v.findViewById(R.id.button_camera_reply_mode_add_image).setOnClickListener(rListener);
        root.addView(v,root.getChildCount());

        if (VERBOSE) {
            Log.v(TAG,"exiting startNewReplyInputMode...");
        }
    }


    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        if (VERBOSE) Log.v(TAG,"entering onCreateLoader...");

        String threadID = String.valueOf(currentThread);
        String[] selectionArgs = {threadID};

            CursorLoader loader = new CursorLoader(
                    this.getActivity(),
                    FireFlyContentProvider.CONTENT_URI_REPLY_THREAD_LIST,
                    null,
                    SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID + "= ?" ,
                    selectionArgs,
                    SQLiteDbContract.LiveReplies.COLUMN_ID);

        if (VERBOSE) Log.v(TAG,"exiting onCreateLoader...");
            return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (VERBOSE) Log.v(TAG,"entering onLoadFinished...");
        mAdapter.swapCursor(data);
        if (VERBOSE) Log.v(TAG,"exiting onLoadFinished...");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (VERBOSE) Log.v(TAG,"entering onLoaderReset...");
        mAdapter.swapCursor(null);
        if (VERBOSE) Log.v(TAG,"exiting onLoaderReset...");
    }

    public void deleteLoader() {
        getLoaderManager().destroyLoader(REPLY_LOADER_ID);
    }
}
