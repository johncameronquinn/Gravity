package com.jokrapp.android;


import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.Tracker;

import java.lang.ref.WeakReference;


/**
 * Author/Copyright John C. Quinn All Rights Reserved
 * Date last modified: 2015-06-17
 *
 * A simple {@link Fragment} subclass. factory method to
 * create an instance of this fragment.
 */
public class ReplyFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static int currentThread = LiveFragment.NO_LIVE_THREADS_ID;
    public static final int REPLY_LOADER_ID = 3;

    private final boolean VERBOSE = false;
    private final String TAG = "ReplyFragment";

    private static final String CURRENT_THREAD_KEY = "threadkey";

    private LiveFragment.onLiveFragmentInteractionListener mListener;

    private static ReplyButtonListener replyButtonListener;


    ReplyCursorAdapter mAdapter;

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


        replyButtonListener = new ReplyButtonListener();

    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(REPLY_LOADER_ID);

        replyButtonListener = null;
        mAdapter = null;
        PhotoManager.cancelDirectory(Constants.KEY_S3_REPLIES_DIRECTORY);
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

       // receiver = new ReplyReceiver();
//        IntentFilter filter = new IntentFilter(Constants.ACTION_IMAGE_REPLY_THUMBNAIL_LOADED);
//        filter.addAction(Constants.ACTION_IMAGE_REPLY_LOADED);
      //  activity.registerReceiver(receiver, filter);



        mListener = (MainActivity)activity;

        Bundle b = new Bundle();
        b.putString(CURRENT_THREAD_KEY, String.valueOf(currentThread));
        getLoaderManager().restartLoader(REPLY_LOADER_ID, b, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLoaderManager().destroyLoader(REPLY_LOADER_ID);
    //    getActivity().unregisterReceiver(receiver);
    //    receiver = null;
        mListener = null;
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

/*        String[] fromColumns = {
                SQLiteDbContract.LiveReplies.COLUMN_NAME_NAME,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_DESCRIPTION,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_TIME};

        int[] toViews = {R.id.reply_detail_row_name, R.id.reply_detail_row_text, R.id.reply_detail_row_time};*/

        if (currentThread!=LiveFragment.NO_LIVE_THREADS_ID) {
            //mAdapter = new SimpleCursorAdapter(getActivity(),
//                    R.layout.fragment_reply_detail_row, null, fromColumns, toViews, 0);

            mAdapter = new ReplyCursorAdapter(getActivity(),null,0);
            listView.setAdapter(mAdapter);
        }

        if (VERBOSE) {Log.v(TAG,"exiting onCreateView...");}
        return v;
    }



    @Override
    public void onDestroyView() {
       //todo maintain active references to avoid the necessity to search

        View view = getView();
        if(view != null) {
            view.findViewById(R.id.button_reply_refresh).setOnClickListener(null);
            view.findViewById(R.id.button_send_reply).setOnClickListener(null);
            view.findViewById(R.id.button_reply_capture).setOnClickListener(null);
        }

        super.onDestroyView();
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
        view.findViewById(R.id.button_reply_refresh).setOnClickListener(replyButtonListener);
        view.findViewById(R.id.button_send_reply).setOnClickListener(replyButtonListener);
        view.findViewById(R.id.button_reply_capture).setOnClickListener(replyButtonListener);
        setCurrentThread(String.valueOf(currentThread));
        //anything that requires the UI to already exist goes here
        if (VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        View view = getView();

        if(view != null) {
            view.findViewById(R.id.button_reply_refresh).setOnClickListener(replyButtonListener);
            view.findViewById(R.id.button_send_reply).setOnClickListener(replyButtonListener);
            view.findViewById(R.id.button_reply_capture).setOnClickListener(replyButtonListener);
            setCurrentThread(String.valueOf(currentThread));
        }
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
        //((ImageButton)getActivity().findViewById(R.id.button_reply_refresh)).setText(thread);
        currentThread = Integer.valueOf(thread);
        resetDisplay();
    }

    public void resetDisplay() {
        Log.d(TAG, "restarting loader...");

        if (isAdded()) {
            Bundle b = new Bundle();
            b.putString(CURRENT_THREAD_KEY, String.valueOf(currentThread));
            getLoaderManager().restartLoader(REPLY_LOADER_ID, b, this);
        }
    }

    public int getCurrentThread() { return currentThread;}


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

            Bundle b = new Bundle();
            b.putString(Constants.KEY_ANALYTICS_CATEGORY,Constants.ANALYTICS_CATEGORY_REPLY);

            switch (v.getId()) {

                case R.id.button_reply_refresh:
                    if (isAdded()) {
                        activity.sendMsgRequestReplies(currentThread);
                    }
                    b.putString(Constants.KEY_ANALYTICS_ACTION,"refresh");
                    b.putString(Constants.KEY_ANALYTICS_LABEL,"current thread");
                    b.putString(Constants.KEY_ANALYTICS_VALUE,String.valueOf(currentThread));

                    resetDisplay();
                    break;

                case R.id.button_send_reply:

                    if (isAdded()) {
                        commentText   = ((EditText) activity.findViewById(R.id.editText_reply_comment));
                        RelativeLayout layout = (RelativeLayout) commentText.getParent();

                        activity.setReplyFilePath("");
                        activity.setLiveCreateReplyInfo(commentText.getText().toString(), getCurrentThread());

                        InputMethodManager imm = (InputMethodManager) activity
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);

                        commentText.setText("");

                        b.putString(Constants.KEY_ANALYTICS_ACTION, "send reply");
                        b.putString(Constants.KEY_ANALYTICS_LABEL, "current thread");
                        b.putString(Constants.KEY_ANALYTICS_VALUE, String.valueOf(currentThread));
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


                        b.putString(Constants.KEY_ANALYTICS_ACTION,"take picture");
                    }
                    break;


            }

            mListener.sendMsgReportAnalyticsEvent(b);
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


  /*  private ReplyReceiver receiver;

    public class ReplyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LiveFragment.VERBOSE) {

            }
            Log.v(TAG, "received reply intent...");
            switch (intent.getAction()) {

                case Constants.ACTION_IMAGE_REPLY_THUMBNAIL_LOADED:
                Bundle data = intent.getExtras();
                String path = data.getString(Constants.KEY_S3_KEY);

                Log.d(TAG, "searching for image with tag: " + path.substring(0,path.length()-1));
                View layout = getActivity().findViewById(R.id.reply_list_view);
                View v = layout.findViewWithTag(path.substring(0,path.length()-1));
                if (v != null) {
                    if (v.isShown()) {
                        Log.i(TAG, v.toString());
                        if (VERBOSE)
                            Log.v(TAG, "Image loaded from view is visible, decoding and displaying...");
                        ImageView imageView = (ImageView) v.findViewById(R.id.reply_detail_row_image);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                switch (v.getId()) {
                                    case R.id.reply_detail_row_image:
                                        Toast.makeText(getActivity(),"requesting fullscreen mode...",Toast.LENGTH_SHORT).show();

                                        String tag =String.valueOf(((RelativeLayout)v.getParent()).getTag());

                                        MainActivity activity = (MainActivity) getActivity();
                                        activity.sendMsgDownloadImage(Constants.KEY_S3_REPLIES_DIRECTORY, tag
                                        );
                                        activity.findViewById(R.id.imageView_reply_fullscreen).setTag(tag);
                                        activity.findViewById(R.id.imageView_reply_fullscreen).setOnClickListener(this);

                                        break;

                                    case R.id.imageView_reply_fullscreen:
                                        Toast.makeText(getActivity(),"exiting fullscreen mode...",Toast.LENGTH_SHORT).show();
                                        v.setVisibility(View.INVISIBLE);
                                        break;
                                }
                            }
                        });

                        /* create full path from tag - DECODE THUMBNAIL ONLY (so add "s") */
/*                        String[] params = {getActivity().getCacheDir() + "/" + path};


                        new ImageLoadTask(imageView, null).execute(params);

                    } else {
                        if (VERBOSE) Log.v(TAG, "image is now shown do nothing...");
                    }
                } else {
                    Log.e(TAG, "searching for image tag failed...");
                }
                    break;

                case Constants.ACTION_IMAGE_REPLY_LOADED:
                    ImageView fullScreenView= (ImageView)getActivity().findViewById(R.id.imageView_reply_fullscreen);
                    new ImageLoadTask(fullScreenView
                            , null).execute(String.valueOf(getActivity().getCacheDir()+ "/" + fullScreenView.getTag()));
                    break;

            }
        }
    }*/

    /**
     * method 'handleErrorCode'
     *
     * intended to handle relevant error codes passed by the activity
     * @param code
     */
  /*  public void handleResponseCode(int code) {
        if (VERBOSE) Log.v(TAG,"Handling response code... " + code);

        switch (code) {
            case MainActivity.MSG_NOTHING_RETURNED:
                if (Constants.LOGV) Log.v(TAG, "Handling response message : MSG_NOTHING_RETURNED");
                if (getView()!=null) {
                    getView().findViewById(R.id.textView_reply_error).setVisibility(View.VISIBLE);
                } else {
                    Log.v(TAG,"View was null");
                }
                break;
            case MainActivity.MSG_NOT_FOUND:
                if (Constants.LOGV) Log.v(TAG,"Handling response message : MSG_NOT_FOUND");
                if (getView()!=null) {
                    Toast.makeText(getActivity(),"404 returned...",Toast.LENGTH_SHORT).show();
                }
                break;
            case MainActivity.MSG_SUCCESS:
                if (Constants.LOGV) Log.v(TAG,"Handling response message : MSG_SUCCESS");
                if (getView()!=null) {
                    getView().findViewById(R.id.textView_reply_error).setVisibility(View.INVISIBLE);
                }
                break;
            case MainActivity.MSG_UPLOAD_SUCCESS:
                if (Constants.LOGV) Log.v(TAG,"Handling response message : MSG_UPLOAD_SUCCESS");
                if (getView()!=null) {
                    Toast.makeText(getActivity(),"Post Successful:3",Toast.LENGTH_SHORT).show();
                    getView().findViewById(R.id.textView_reply_error).setVisibility(View.INVISIBLE);
                }
                break;
        }
    }*/


}
