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
import android.widget.ImageButton;
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
    private static int currentThread = LiveFragment.NO_LIVE_THREADS_ID;
    public static final int REPLY_LOADER_ID = 3;

    private final boolean VERBOSE = false;
    private final String TAG = "ReplyFragment";

    private static final String CURRENT_THREAD_KEY = "threadkey";

    SimpleCursorAdapter mAdapter;

    public static ReplyFragment newInstance(int currentThread) {
        Bundle args = new Bundle();
        args.putInt(CURRENT_THREAD_KEY, currentThread);

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

            if (VERBOSE) Log.v(TAG,"initializing loader at id " + ReplyFragment.REPLY_LOADER_ID);

            if (currentThread == LiveFragment.NO_LIVE_THREADS_ID) {
                currentThread = b.getInt(CURRENT_THREAD_KEY);
                Bundle args = new Bundle();
                args.putString(CURRENT_THREAD_KEY, String.valueOf(currentThread));
                getLoaderManager().restartLoader(ReplyFragment.REPLY_LOADER_ID, args, this);
            } else {
                currentThread = b.getInt(CURRENT_THREAD_KEY);
            }

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

        String[] fromColumns = {
                SQLiteDbContract.LiveReplies.COLUMN_NAME_NAME,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_DESCRIPTION,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_TIME};

        int[] toViews = {R.id.reply_detail_row_name, R.id.reply_detail_row_text, R.id.reply_detail_row_time};

        if (currentThread!=LiveFragment.NO_LIVE_THREADS_ID) {
            mAdapter = new SimpleCursorAdapter(getActivity(),
                    R.layout.fragment_reply_detail_row, null, fromColumns, toViews, 0);
            listView.setAdapter(mAdapter);
        }

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
        view.findViewById(R.id.button_reply_refresh).setOnClickListener(getButtonListener(this));
        view.findViewById(R.id.button_send_reply).setOnClickListener(getButtonListener(this));
        view.findViewById(R.id.button_reply_capture).setOnClickListener(getButtonListener(this));
        setCurrentThread(String.valueOf(currentThread));
        //anything that requires the UI to already exist goes here
        if (VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        View view = getView();

        if(view != null) {
            view.findViewById(R.id.button_reply_refresh).setOnClickListener(getButtonListener(this));
            view.findViewById(R.id.button_send_reply).setOnClickListener(getButtonListener(this));
            view.findViewById(R.id.button_reply_capture).setOnClickListener(getButtonListener(this));
            setCurrentThread(String.valueOf(currentThread));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {

        View view = getView();

        super.onResume();
        if(view != null) {
            view.findViewById(R.id.button_reply_refresh).setOnClickListener(getButtonListener(this));
            view.findViewById(R.id.button_send_reply).setOnClickListener(getButtonListener(this));
            view.findViewById(R.id.button_reply_capture).setOnClickListener(getButtonListener(this));
            setCurrentThread(String.valueOf(currentThread));
        }
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
        //((ImageButton)getActivity().findViewById(R.id.button_reply_refresh)).setText(thread);
        currentThread = Integer.valueOf(thread);
    }

    public void resetDisplay() {
        Log.d(TAG, "restarting loader...");

        Bundle b = new Bundle();
        b.putString(CURRENT_THREAD_KEY,String.valueOf(currentThread));
        getLoaderManager().restartLoader(REPLY_LOADER_ID,b,this);
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
    public class ReplyButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG, "OnClickRegistered..." + v.toString());
            }

            EditText commentText;

            MainActivity activity = (MainActivity)getActivity();

            switch (v.getId()) {

                case R.id.button_reply_refresh:
                    if (isAdded()) {
                        activity.sendMsgRequestReplies(currentThread);
                    }
                    resetDisplay();
                    break;

                case R.id.button_send_reply:

                    if (isAdded()) {
                        commentText   = ((EditText) activity.findViewById(R.id.editText_reply_comment));
                        RelativeLayout layout = (RelativeLayout) commentText.getParent();

                        activity.setLiveFilePath("");
                        activity.setLiveCreateReplyInfo(commentText.getText().toString(), getCurrentThread());

                        InputMethodManager imm = (InputMethodManager) activity
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);

                        commentText.setText("");
                    }

                    break;

                case R.id.button_reply_capture:

                    if (isAdded()) {

                        commentText   = ((EditText) activity.findViewById(R.id.editText_reply_comment));
                        RelativeLayout layout = (RelativeLayout) commentText.getParent();
                        activity.takeReplyPicture();
                        activity.setLiveCreateReplyInfo(commentText.getText().toString(),
                                getCurrentThread());

                        InputMethodManager imm = (InputMethodManager) activity
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);

                        commentText.setText("");
                    }
                    break;
            }
        }

    }


    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        if (VERBOSE) Log.v(TAG,"entering onCreateLoader...");

        String[] selectionArgs = {args.getString(CURRENT_THREAD_KEY)};

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
        if (VERBOSE) Log.v(TAG, "entering onLoaderReset...");
        mAdapter.swapCursor(null);
        if (VERBOSE) Log.v(TAG,"exiting onLoaderReset...");
    }
}
