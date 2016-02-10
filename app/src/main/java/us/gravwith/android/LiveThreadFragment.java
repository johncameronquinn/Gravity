package us.gravwith.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import us.gravwith.android.util.Utility;

/**
 * fragment 'LiveThreadFragment'
 *
 *  Created by John C. Quinn on 8/20/15.
 *
 *  Represents a single live thread
 *
 *  instantiated from {@link CursorPagerAdapter}
 */
public class LiveThreadFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "LiveThreadFragment";

    private String threadName;
    private String threadTitle;
    private String threadText;
    private String threadID;
    private String unique;
    private String replies;
    private String topicARN;
    private int time;

    private Drawable mEmptyDrawable;

    private ProgressBar progressBar;

    PhotoView mPhotoView;

    //View textView;
    //View detailView;

    String mImageKey;

    static LiveThreadFragment newInstance(String name,
                                          String title,
                                          String text,
                                          String filePath,
                                          String threadID,
                                          String unique,
                                          String replies,
                                          String topicARN)
    {
        LiveThreadFragment f = new LiveThreadFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_NAME, name);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TITLE,title);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_DESCRIPTION,text);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_FILEPATH,filePath);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_THREAD_ID,threadID);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_UNIQUE,unique);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_REPLIES,replies);
        args.putString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TOPIC_ARN, topicARN);
        f.setArguments(args);

        return f;
    }

    static LiveThreadFragment newInstance() {
        LiveThreadFragment f = new LiveThreadFragment();
        return f;
    }


    public LiveThreadFragment() {
    }

    public String getThreadID(){
        return threadID;
    }

    public String getTopicARN(){
        return topicARN;
    }

    public String getUniqueCount(){
        return unique;
    }

    public String getTitle() {
        return threadTitle;
    }

    public String getDescription() {
        return threadText;
    }

    public String getReplyCount(){
        return replies;
    }

    public String getRelativeTime(){
        return Utility.getRelativeTimeStringFromLong(time);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);


    }

    /*
     * This callback is invoked when the Fragment is no longer attached to its Activity.
     * Sets the URL for the Fragment to null
     */
    @Override
    public void onDetach() {
        // Logs the detach
        if (LiveFragment.VERBOSE) Log.v(TAG, "onDetach");

        // Removes the reference to the URL
        mImageKey = null;
        threadName = null;
        threadText = null;
        threadTitle = null;
        threadID = null;
        unique = null;
        replies = null;
        time = 0;

        // Always call the super method last
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mEmptyDrawable = getResources().getDrawable(R.drawable.imagenotqueued);

        if (getArguments() != null) {
            Bundle args = getArguments();
            threadName = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_NAME);
            threadTitle = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TITLE);
            threadText = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_DESCRIPTION);
            threadID = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_THREAD_ID,"0");
            unique = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_UNIQUE);
            replies = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_REPLIES);
            topicARN = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TOPIC_ARN);
            time = Integer.parseInt(args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TIME,"0"));

            mImageKey = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_FILEPATH);

        }

    }

    @Override
    public void setArguments(Bundle args) {

        if (args != null) {
            threadName = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_NAME);
            threadTitle = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_DESCRIPTION);
            threadText = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_DESCRIPTION);
            threadID = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_THREAD_ID,"0");
            unique = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_UNIQUE);
            replies = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_REPLIES);
            topicARN = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TOPIC_ARN);
            time = Integer.parseInt(args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_TIME,"0"));

            mImageKey = args.getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_FILEPATH);
        }

        super.setArguments(args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState!=null) {
           // image = savedInstanceState.getParcelable(IMAGE_KEY);
        }

        View localView = inflater.inflate(R.layout.fragment_live_thread,container,false);
        mPhotoView = ((PhotoView) localView.findViewById(R.id.photoView));
        progressBar = ((ProgressBar) localView.findViewById(R.id.photoProgress));

        //textView = localView.findViewById(R.id.live_thread_infoLayout);
        //detailView = localView.findViewById(R.id.live_thread_text);

        /*
         * The click listener becomes this class (PhotoFragment). The onClick() method in this
         * class is invoked when users click a photo.
         */
        //mPhotoView.setOnClickListener(this);
        //textView.setOnClickListener(this);

        mPhotoView.setImageKey(Constants.KEY_S3_LIVE_DIRECTORY,mImageKey,true,mEmptyDrawable);

        return localView;
    }


    /*
     * This callback is invoked as the Fragment's View is being destroyed
     */
    @Override
    public void onDestroyView() {
        // Logs the destroy operation
        if (LiveFragment.VERBOSE) Log.v(TAG,"onDestroyView");

        // If the View object still exists, delete references to avoid memory leaks
        if (mPhotoView != null) {

            mPhotoView.setOnClickListener(null);
            //textView.setOnClickListener(null);
            this.mPhotoView = null;
            //this.textView = null;
            //this.detailView = null;
        }

        // Always call the super method last
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mEmptyDrawable = null;
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onSaveInstanceState...");

        outState.putString(Constants.KEY_S3_KEY, mImageKey);
        if (LiveFragment.VERBOSE) Log.v(TAG, "exiting onSaveInstanceState...");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onViewStateRestored...");
        if (savedInstanceState!=null) {
        }
        if (LiveFragment.VERBOSE) Log.v(TAG, "exiting onViewStateRestored...");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onViewCreated...");
        super.onViewCreated(view, savedInstanceState);


        //   ((TextView)view.findViewById(R.id.live_thread_number)).setText(String.valueOf(threadNum));
        /*((TextView)view.findViewById(R.id.live_thread_name)).setText(threadName);
        ((TextView)view.findViewById(R.id.live_thread_title)).setText(threadTitle);
        ((TextView)view.findViewById(R.id.live_thread_text)).setText(threadText);
        ((TextView)view.findViewById(R.id.live_thread_unique)).setText(unique);
        ((TextView)view.findViewById(R.id.live_thread_replies)).setText(replies);*/
       view.setTag(mImageKey);

        if (LiveFragment.VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

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
        }
    }


}