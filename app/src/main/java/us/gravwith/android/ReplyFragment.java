package us.gravwith.android;


import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;

import java.net.HttpURLConnection;
import java.util.UUID;

import us.gravwith.android.util.Utility;


/**
 * Author/Copyright John C. Quinn All Rights Reserved
 * Date last modified: 2015-06-17
 *
 * A simple {@link Fragment} subclass. factory method to
 * create an instance of this fragment.
 */
public class ReplyFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        FloatingActionMenu.OnMenuToggleListener {

    public static final int REPLY_LOADER_ID = 3;

    private final boolean VERBOSE = false;
    private final String TAG = ReplyFragment.class.getSimpleName();

    private LiveFragment.onLiveFragmentInteractionListener mListener;
    private ListView mListView;
    private TextView replyErrorText;
    private RelativeLayout textingFooterView;
    private RelativeLayout textingParentView;

    private RelativeLayout opHeaderView;

    private TextView opDescription;
    private TextView opTime;
    private PhotoView opPhoto;


    private FloatingActionMenu radicalMenuView;

    private TextView replyCountView;

    private static ReplyButtonListener replyButtonListener;

    /*
     * this is an array of the buttons which should be hidden unless there is content to display
     */
    private int[] contentButtonsArray = new int[]{
            R.id.button_reply_report
    };

    HybridCursorAdapter mAdapter;

    public static ReplyFragment newInstance() {

        ReplyFragment fragment = new ReplyFragment();
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

            Bundle args = new Bundle();
            mAdapter = new HybridCursorAdapter(getActivity(),null,0);
            getLoaderManager().restartLoader(ReplyFragment.REPLY_LOADER_ID, args, this);
        }

        replyButtonListener = new ReplyButtonListener();
    }

    @Override
    public void onStart() {
        if (VERBOSE) Log.v(TAG, "entering onStart...");
        super.onStart();

        if (mAdapter == null) {
            mAdapter = new HybridCursorAdapter(getActivity(),null,0);
        }

        if (VERBOSE) Log.v(TAG,"exiting onStart...");
    }

    @Override
    public void onResume() {
        if (VERBOSE) Log.v(TAG, "entering onResume...");
        super.onResume();

        if (mListView != null) {
            mListView.setAdapter(mAdapter);
        }
        /*if (!hasRefreshed) {
        /* go ahead and get the latest list */
            //triggerReplyRefresh();
        //}

        if (VERBOSE) Log.v(TAG, "exiting onResume...");
    }

    @Override
    public void onPause() {
        if (VERBOSE) Log.v(TAG, "entering onPause...");
        super.onPause();

        if (VERBOSE) Log.v(TAG, "exiting onPause...");
    }

    @Override
    public void onStop() {
        if (VERBOSE) Log.v(TAG, "entering onStop...");
        super.onStop();

        if (VERBOSE) Log.v(TAG, "exiting onStop...");
    }

    @Override
    public void onDestroy() {
        if (VERBOSE) Log.v(TAG, "entering onDestroy...");
        getLoaderManager().destroyLoader(REPLY_LOADER_ID);

        replyButtonListener = null;
        mAdapter = null;
        PhotoManager.cancelDirectory(Constants.KEY_S3_REPLIES_DIRECTORY);

        super.onDestroy();

        if (VERBOSE) Log.v(TAG, "exiting onDestroy...");
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

        mListView = (ListView)v.findViewById(R.id.reply_list_view);

        mListView.setAdapter(mAdapter);

        //textingFooterView = (RelativeLayout)v.findViewById(R.id.reply_texting_layout);

        textingFooterView = (RelativeLayout)
                inflater.inflate(R.layout.listview_footer_texting, mListView, false);

        textingParentView = (RelativeLayout)v.findViewById(R.id.layout_reply_texting);
        textingParentView.findViewById(R.id.button_reply_capture)
                .setOnClickListener(replyButtonListener);
        textingParentView.findViewById(R.id.button_reply_send)
                .setOnClickListener(replyButtonListener);

        updateSmartFooter();

        opHeaderView = (RelativeLayout)
                inflater.inflate(R.layout.fragment_reply_detail_row, mListView, false);
        opDescription = (TextView)opHeaderView.findViewById(R.id.reply_detail_row_text);
        opTime = (TextView)opHeaderView.findViewById(R.id.reply_detail_row_time);
        opPhoto = (PhotoView)opHeaderView.findViewById(R.id.photoView);

        radicalMenuView = (FloatingActionMenu)v.findViewById(R.id.reply_radical_menu);
        //radicalMenuView.setOnMenuToggleListener(this);
        replyErrorText = (TextView)v.findViewById(R.id.textView_reply_error);
        replyCountView = (TextView)v.findViewById(R.id.textView_reply_count);

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        break;

                    case MotionEvent.ACTION_UP:
                        break;
                }

                return false;
            }
        });

/*        String[] fromColumns = {
                SQLiteDbContract.LiveReplies.COLUMN_NAME_NAME,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_DESCRIPTION,
                SQLiteDbContract.LiveReplies.COLUMN_NAME_TIME};

        int[] toViews = {R.id.reply_detail_row_name, R.id.reply_detail_row_text, R.id.reply_detail_row_time};*/

              //contentButtonViews.add(v.findViewById(R.id.button_reply_load))

        if (VERBOSE) {Log.v(TAG, "exiting onCreateView...");}
        return v;
    }


    @Override
    public void onDestroyView() {
       //todo maintain active references to avoid the necessity to search

        View view = getView();
        if(view != null) {
            view.findViewById(R.id.button_reply_refresh).setOnClickListener(null);
            view.findViewById(R.id.button_reply_send).setOnClickListener(null);
            //view.findViewById(R.id.button_reply_capture).setOnClickListener(null);
        }
        mListView = null;
        radicalMenuView = null;
        textingFooterView = null;
        replyErrorText = null;
        replyCountView = null;
        opDescription = null;
        opPhoto = null;
        opHeaderView = null;

        textingParentView = null;

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
        resetDisplay();

        mListView.addFooterView(textingFooterView);
        mListView.addHeaderView(opHeaderView);

        textingFooterView
                .findViewById(R.id.button_reply_send)
                .setOnClickListener(replyButtonListener);

        view.findViewById(R.id.button_reply_refresh).setOnClickListener(replyButtonListener);
        view.findViewById(R.id.button_reply_report).setOnClickListener(replyButtonListener);
        textingFooterView.findViewById(R.id.button_reply_capture).setOnClickListener(replyButtonListener);

        opPhoto.setOnClickListener(replyButtonListener);

    //    view.findViewById(R.id.button_reply_capture).setOnClickListener(replyButtonListener);

        //anything that requires the UI to already exist goes here
        if (VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        View view = getView();

        if(view != null) {
            view.findViewById(R.id.button_reply_refresh).setOnClickListener(replyButtonListener);
            view.findViewById(R.id.button_reply_send).setOnClickListener(replyButtonListener);
            //view.findViewById(R.id.button_reply_test).setOnClickListener(replyButtonListener);
            //view.findViewById(R.id.button_reply_capture).setOnClickListener(replyButtonListener);
            resetDisplay();
        }
    }

    @Override
    public void onMenuToggle(boolean opened) {
        if (opened) {
            ((TransitionDrawable)radicalMenuView.getBackground()).startTransition(
                    getResources().getInteger(R.integer.radical_background_transition_duration)
            );
        } else {
            ((TransitionDrawable) radicalMenuView.getBackground()).reverseTransition(
                    getResources().getInteger(R.integer.radical_background_transition_duration)
            );
        }
    }

    public void resetDisplay() {
        if (isAdded() && mListener.getCurrentThread() != null) {
            Log.d(TAG, "restarting loader...");
            Bundle b = new Bundle();
            b.putString(SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID,
                    Utility.dehyphenUUID(mListener.getCurrentThread()));
            getLoaderManager().restartLoader(REPLY_LOADER_ID, b, this);
            replyCountView.setText(mListener.getCurrentRepliesCount());
        }
    }

    public void triggerReplyRefresh() {

        if (mListView != null) {
            mListView.setAdapter(null);
        }
        mListener.sendMsgRequestReplies(mListener.getCurrentThread());
    }


    public void handleReplyResponseState(Message msg) {
        if (VERBOSE) {
            Log.v(TAG,"entering handleReplyResponseState...");
        }

        switch (msg.arg2) {
            case HttpURLConnection.HTTP_OK:
                if (VERBOSE) Log.v(TAG,"Response code : " + msg.arg2);

                mListView.setAdapter(mAdapter);
                break;

            default:
                //Toast.makeText(getActivity(), "Response code : " + msg.arg2, Toast.LENGTH_SHORT).show();
                break;
        }

        if (VERBOSE) {
            Log.v(TAG, "exiting handleReplyResponseState...");
        }
    }

    public void setOpInfo(UUID threadID, String mImageKey, String descriptionText, String currentTime) {
        if (VERBOSE) Log.v(TAG, "entering setOpInfo... with " + mImageKey + " and " + descriptionText);

        /* updates displayed text*/
        opDescription.setText(descriptionText);
        opTime.setText(currentTime + " (OP)");

        /* updates view tag for use by ReportManager*/
        opHeaderView.setTag(R.integer.content_id_key, threadID);

        /* updates image thumbnail*/
        opHeaderView.findViewById(R.id.photoProgress).setVisibility(View.VISIBLE);

        opPhoto.setImageKey(
                Constants.KEY_S3_LIVE_DIRECTORY,
                mImageKey,
                true,
                null
        );

        Utility.clearTextAndFocus(((EditText)textingFooterView
                .findViewById(R.id.editText_reply_comment)));
        Utility.clearTextAndFocus((EditText)textingParentView
                .findViewById(R.id.editText_reply_comment));

        if (VERBOSE) Log.v(TAG, "exiting setOpInfo...");
    }

    public void closeRadical() {
        if (radicalMenuView != null) {
            radicalMenuView.close(false);
        }
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

            mListener.getAnalyticsReporter().ReportClickEvent(v);

            EditText commentText;

            MainActivity activity = (MainActivity)getActivity();

            switch (v.getId()) {

                case R.id.button_reply_refresh:
                    getActivity()
                            .getContentResolver()
                            .delete(FireFlyContentProvider.CONTENT_URI_REPLY_LIST, null,null);

                    triggerReplyRefresh();

                    resetDisplay();
                    break;

                case R.id.button_reply_send:

                    if (isAdded()) {
                        if (textingParentView.getVisibility() == View.VISIBLE) {
                            commentText = ((EditText) textingParentView.findViewById(R.id.editText_reply_comment));
                        } else {
                            commentText = ((EditText) textingFooterView.findViewById(R.id.editText_reply_comment));
                        }
                        RelativeLayout layout = (RelativeLayout) commentText.getParent();

                        activity.setReplyFilePath("");
                        //activity.setLiveFilePath("");
                        //activity.setLiveCreateThreadInfo("", commentText.getText().toString());
                        activity.setLiveCreateReplyInfo(commentText.getText().toString(),
                                mListener.getCurrentThread(),
                                mListener.getCurrentTopicARN());
                        //triggerReplyRefresh();

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
                                mListener.getCurrentThread(),
                                mListener.getCurrentTopicARN());
                        //activity.setLiveCreateThreadInfo("","",commentText.getText().toString());

                        InputMethodManager imm = (InputMethodManager) activity
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);

                        commentText.setText("");
                    }
                    break;

                case R.id.button_reply_report:

                    if (VERBOSE) Log.v(TAG,"entering report mode...");

                    ReportManager manager = new ReportManager((MainActivity)getActivity(),
                            mListView, new ReportManager.ReportStatusListener() {
                        @Override
                        public void onRequestSuccess() {

                        }

                        @Override
                        public void onRequestError(int code) {

                        }

                        @Override
                        public void onRequestStarted() {

                        }

                        @Override
                        public void onDialogClosed(boolean didTheyHitYes) {
                            mListener.getAnalyticsReporter().ReportBehaviorEvent(
                                    AnalyticsReporter.ANALYTICS_ACTION_BUTTON_PRESS,
                                    mListener.getAnalyticsReporter()
                                            .getButtonResourceID(R.id.button_reply_report),
                                    (didTheyHitYes) ? "yes":"no"
                            );
                        }
                    });

                    manager.startReportSelectionMode();
                    break;

                case R.id.photoView:

                    Log.d(TAG,"View string is : " + v.toString());

                    // Retrieves the urlString from the cursor
                    String s3Key = ((PhotoView)v).getImageKey();
                    //s3Key = s3Key.substring(0,s3Key.length()-1);

                    Log.d(TAG, "grabbed key is: " + s3Key);
                    /*
                     * Creates a new Intent to get the full picture for the thumbnail that the user clicked.
                     * The full photo is loaded into a separate Fragment
                     */
                     Intent localIntent =
                            new Intent(Constants.ACTION_VIEW_IMAGE).putExtra(Constants.KEY_S3_KEY,s3Key)
                                    .putExtra(Constants.KEY_S3_DIRECTORY,Constants.KEY_S3_LIVE_DIRECTORY)
                                    .putExtra(Constants.KEY_PREVIEW_IMAGE,false);

                    // Broadcasts the Intent to receivers in this app. See DisplayActivity.FragmentDisplayer.
                    LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(localIntent);

                    break;


                /*case R.id.button_reply_test:

                    if (VERBOSE) Log.v(TAG,"entering devbutton mode...");

                    Toast.makeText(getActivity(), "Enabling super-user...",Toast.LENGTH_SHORT)
                            .show();
                    break;*/

            }
        }

    }


    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        if (VERBOSE) Log.v(TAG,"entering onCreateLoader...");

        String[] selectionArgs = {
                String.valueOf(args.get(SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID))
        };

        Log.d(TAG,"current selection args = " + args.get(SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID));

            CursorLoader loader = new CursorLoader(
                    this.getActivity(),
                    FireFlyContentProvider.CONTENT_URI_REPLY_LIST,
                    null,
                    SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID + " = ?" ,
                    selectionArgs,
                    SQLiteDbContract.LiveReplies.COLUMN_NAME_TIME
            );

        if (VERBOSE) Log.v(TAG,"exiting onCreateLoader...");
            return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (VERBOSE) Log.v(TAG,"entering onLoadFinished...");

          /* only set the action buttons to visible if there is content */
        /*if (data == null) {
            Log.e(TAG, "why was the returned cursor null?");
            mAdapter.swapCursor(null);
            return;
        }*/

     /*   Log.e(TAG,"cursor row count : " + data.getCount());
        if (data.getCount() > 0) {
            View v = getView();
            if (v!=null) {
                Utility.showViewsInArray(contentButtonsArray,v);
            }
        } else {
            View v = getView();
            if (v!=null) {
                Utility.hideViewsInArray(contentButtonsArray,v);
            }

            Log.i(TAG, "There are no replies... notify user?");
            //todo, explain what replies are, and suggest a reply
        }*/

        mAdapter.swapCursor(data);
        mListView.setAdapter(mAdapter);
        mListView.setSelection(0);

        if (replyCountView != null) {
            replyCountView.setText(String.valueOf(data.getCount()));

            mListener.updateCurrentReplies(data.getCount());
            mListener.updateLiveReplyCount();
            updateSmartFooter();
        }



        if (VERBOSE) Log.v(TAG,"exiting onLoadFinished...");
    }

    private static UpdateFooterRunnable updateFooterRunnable;

    public void updateSmartFooter() {

        if (updateFooterRunnable == null) {
            updateFooterRunnable = new UpdateFooterRunnable();
        }

        if (mListView.getHandler() != null) {
            mListView.getHandler().removeCallbacks(updateFooterRunnable);
            mListView.getHandler().postAtFrontOfQueue(updateFooterRunnable);
        }
    }

    class UpdateFooterRunnable implements Runnable {

        public void run() {

            int numvisibleitems = mListView.getFirstVisiblePosition() - mListView.getLastVisiblePosition();

            if (mAdapter.getCount() > numvisibleitems) {
                //hide parent view
                textingParentView.setVisibility(View.GONE);

                //show footer view
                textingFooterView.setVisibility(View.VISIBLE);
            } else {
                //hide footer view
                textingFooterView.setVisibility(View.GONE);

                //show parent view
                textingParentView.setVisibility(View.VISIBLE);

            }
        }

    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (VERBOSE) Log.v(TAG, "entering onLoaderReset...");
        mAdapter.swapCursor(null);
        if (VERBOSE) Log.v(TAG,"exiting onLoaderReset...");
    }

}

