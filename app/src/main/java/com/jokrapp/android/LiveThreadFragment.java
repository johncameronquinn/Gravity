package com.jokrapp.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.tech.TagTechnology;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.jokrapp.android.SQLiteDbContract.LiveThreadEntry;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

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

    private Drawable mEmptyDrawable;

    private ProgressBar progressBar;

    PhotoView mPhotoView;

    View textView;
    View detailView;

    String mImageKey;

    static LiveThreadFragment newInstance(String name,
                                          String title,
                                          String text,
                                          String filePath,
                                          String threadID,
                                          String unique,
                                          String replies)
    {
        LiveThreadFragment f = new LiveThreadFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString(LiveThreadEntry.COLUMN_NAME_NAME, name);
        args.putString(LiveThreadEntry.COLUMN_NAME_TITLE,title);
        args.putString(LiveThreadEntry.COLUMN_NAME_DESCRIPTION,text);
        args.putString(LiveThreadEntry.COLUMN_NAME_FILEPATH,filePath);
        args.putString(LiveThreadEntry.COLUMN_NAME_THREAD_ID,threadID);
        args.putString(LiveThreadEntry.COLUMN_NAME_UNIQUE,unique);
        args.putString(LiveThreadEntry.COLUMN_NAME_REPLIES,replies);
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

        // Always call the super method last
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mEmptyDrawable = getResources().getDrawable(R.drawable.imagenotqueued);

        if (getArguments() != null) {
            Bundle args = getArguments();
            threadName = args.getString(LiveThreadEntry.COLUMN_NAME_NAME);
            threadTitle = args.getString(LiveThreadEntry.COLUMN_NAME_TITLE);
            threadText = args.getString(LiveThreadEntry.COLUMN_NAME_DESCRIPTION);
            threadID = args.getString(LiveThreadEntry.COLUMN_NAME_THREAD_ID);
            unique = args.getString(LiveThreadEntry.COLUMN_NAME_UNIQUE);
            replies = args.getString(LiveThreadEntry.COLUMN_NAME_REPLIES);

            mImageKey = args.getString(LiveThreadEntry.COLUMN_NAME_FILEPATH);


            ((MainActivity)getActivity()).sendMsgDownloadImage(
                    Constants.KEY_S3_LIVE_DIRECTORY,
                    args.getString(SQLiteDbContract.LiveReplies.COLUMN_NAME_FILEPATH));
        }

    }

    @Override
    public void setArguments(Bundle args) {

        if (args != null) {
            threadName = args.getString(LiveThreadEntry.COLUMN_NAME_NAME);
            threadTitle = args.getString(LiveThreadEntry.COLUMN_NAME_TITLE);
            threadText = args.getString(LiveThreadEntry.COLUMN_NAME_DESCRIPTION);
            threadID = args.getString(LiveThreadEntry.COLUMN_NAME_THREAD_ID);
            unique = args.getString(LiveThreadEntry.COLUMN_NAME_UNIQUE);
            replies = args.getString(LiveThreadEntry.COLUMN_NAME_REPLIES);

            mImageKey = args.getString(LiveThreadEntry.COLUMN_NAME_FILEPATH);

            Log.i(TAG,"incoming name: " + threadName);
            Log.i(TAG,"incoming image key: " + mImageKey);

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
        mPhotoView = ((PhotoView) localView.findViewById(R.id.thumbImage));
        progressBar = ((ProgressBar) localView.findViewById(R.id.photoProgress));

        textView = localView.findViewById(R.id.live_thread_infoLayout);
        detailView = localView.findViewById(R.id.live_thread_text);

        /*
         * The click listener becomes this class (PhotoFragment). The onClick() method in this
         * class is invoked when users click a photo.
         */
        mPhotoView.setOnClickListener(this);
        textView.setOnClickListener(this);
        detailView.setOnClickListener(this);

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
            textView.setOnClickListener(null);
            detailView.setOnClickListener(null);
            this.mPhotoView = null;
            this.textView = null;
            this.detailView = null;
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
        super.onSaveInstanceState(outState);
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onSaveInstanceState...");

        outState.putString(Constants.KEY_S3_KEY, mImageKey);
        if (LiveFragment.VERBOSE) Log.v(TAG, "exiting onSaveInstanceState...");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onViewCreated...");
        super.onViewCreated(view, savedInstanceState);


        //   ((TextView)view.findViewById(R.id.live_thread_number)).setText(String.valueOf(threadNum));
        ((TextView)view.findViewById(R.id.live_thread_name)).setText(threadName);
        ((TextView)view.findViewById(R.id.live_thread_title)).setText(threadTitle);
        ((TextView)view.findViewById(R.id.live_thread_text)).setText(threadText);
        ((TextView)view.findViewById(R.id.live_thread_unique)).setText(unique);
        ((TextView)view.findViewById(R.id.live_thread_replies)).setText(replies);
       view.setTag(mImageKey);




        /*
         * No saved image was loaded, load from file or request
         */
    /*    if (image == null) {

            if (LiveFragment.VERBOSE) {
                Log.v(TAG,"loading image was null, attempting to decode from exposed filepath");
            }

            File file = new File(getActivity().getCacheDir().toString() + "/" + threadFilePath);
            if (file.exists() && imageLoaderThreadReference.get() == null) {

                Log.i(TAG, "filepath exists and no other images are being decoded");
                    imageLoaderThreadReference =
                            new WeakReference<>(new Thread(new ImageLoaderRunnable(threadFilePath)));
                    imageLoaderThreadReference.get().start();

            } else {
                if (LiveFragment.VERBOSE) {
                    Log.v(TAG,"requested image has not yet been downloaded... requesting: "
                            + threadFilePath);
                }

                ((MainActivity)getActivity())
                        .sendMsgDownloadImage(Constants.KEY_S3_LIVE_DIRECTORY,threadFilePath);
            }

        } else {
            if (LiveFragment.VERBOSE) {
                Log.v(TAG,"Image was not null, setting...");
            }
            displayView.setImageBitmap(image);
            progressBar.setVisibility(View.INVISIBLE);
        }*/

        if (LiveFragment.VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.live_thread_infoLayout:
                View threadTextView = ((RelativeLayout) v.getParent()).findViewById(R.id.live_thread_text);

                threadTextView.setVisibility(View.VISIBLE);
                threadTextView.bringToFront();
                v.setVisibility(View.INVISIBLE);
                break;


            case R.id.live_thread_text:
                ((RelativeLayout)v.getParent()).findViewById(R.id.live_thread_infoLayout).setVisibility(View.VISIBLE);
                v.setVisibility(View.INVISIBLE);
                break;
        }
    }


}