package com.jokrapp.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.tech.TagTechnology;
import android.os.AsyncTask;
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
    private String threadFilePath;
    private String threadID;
    private String unique;
    private String replies;

    private ImageView displayView;
    private Bitmap image;
    private ProgressBar progressBar;

    private static final String IMAGE_KEY = "bitmap";

    public WeakReference<Thread> imageLoaderThreadReference = new WeakReference(null);

    public LiveThreadReceiver receiver;

    Handler imageLoadHandler = new Handler();

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

        receiver = new LiveThreadReceiver();
        IntentFilter filter = new IntentFilter(Constants.ACTION_IMAGE_LOADED);
        activity.registerReceiver(receiver, filter);


    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getArguments() != null) {
            Bundle args = getArguments();
            threadName = args.getString(LiveThreadEntry.COLUMN_NAME_NAME);
            threadTitle = args.getString(LiveThreadEntry.COLUMN_NAME_TITLE);
            threadText = args.getString(LiveThreadEntry.COLUMN_NAME_DESCRIPTION);
            threadFilePath = args.getString(LiveThreadEntry.COLUMN_NAME_FILEPATH);
            threadID = args.getString(LiveThreadEntry.COLUMN_NAME_THREAD_ID);
            unique = args.getString(LiveThreadEntry.COLUMN_NAME_UNIQUE);
            replies = args.getString(LiveThreadEntry.COLUMN_NAME_REPLIES);
        }

    }

    @Override
    public void setArguments(Bundle args) {

        if (args != null) {
            threadName = args.getString(LiveThreadEntry.COLUMN_NAME_NAME);
            threadTitle = args.getString(LiveThreadEntry.COLUMN_NAME_TITLE);
            threadText = args.getString(LiveThreadEntry.COLUMN_NAME_DESCRIPTION);
            threadFilePath = args.getString(LiveThreadEntry.COLUMN_NAME_FILEPATH);
            threadID = args.getString(LiveThreadEntry.COLUMN_NAME_THREAD_ID);
            unique = args.getString(LiveThreadEntry.COLUMN_NAME_UNIQUE);
            replies = args.getString(LiveThreadEntry.COLUMN_NAME_REPLIES);
            Log.i(TAG,"incoming name: " + threadName);
            Log.i(TAG,"incoming threadFilePath: " + threadFilePath);
        }

        super.setArguments(args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState!=null) {
            image = savedInstanceState.getParcelable(IMAGE_KEY);
        }


        return inflater.inflate(R.layout.fragment_live_thread,container,false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onSaveInstanceState...");

        outState.putParcelable(IMAGE_KEY, image);

        if (LiveFragment.VERBOSE) Log.v(TAG, "exiting onSaveInstanceState...");
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (LiveFragment.VERBOSE) Log.v(TAG, "entering onViewStateRestored...");


        if (LiveFragment.VERBOSE) Log.v(TAG,"exiting onViewStateRestored...");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (LiveFragment.VERBOSE) Log.v(TAG,"entering onViewCreated...");
        super.onViewCreated(view, savedInstanceState);


        //   ((TextView)view.findViewById(R.id.live_thread_number)).setText(String.valueOf(threadNum));
        ((TextView)view.findViewById(R.id.live_thread_name)).setText(threadName);
        ((TextView)view.findViewById(R.id.live_thread_title)).setText(threadTitle);
        ((TextView)view.findViewById(R.id.live_thread_text)).setText(threadText);
        ((TextView)view.findViewById(R.id.live_thread_unique)).setText(unique);
        ((TextView)view.findViewById(R.id.live_thread_replies)).setText(replies);
        view.findViewById(R.id.live_thread_infoLayout).setOnClickListener(this);
        view.findViewById(R.id.live_thread_text).setOnClickListener(this);



        displayView = ((ImageView) view.findViewById(R.id.live_thread_imageView));
        progressBar = ((ProgressBar)view.findViewById(R.id.threadprogressbar));

        /*
         * No saved image was loaded, load from file or request
         */
        if (image == null) {

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

                ((MainActivity)getActivity()).sendMsgDownloadImage("live",threadFilePath);
            }

        } else {
            if (LiveFragment.VERBOSE) {
                Log.v(TAG,"Image was not null, setting...");
            }
            displayView.setImageBitmap(image);
            progressBar.setVisibility(View.INVISIBLE);
        }

        if (LiveFragment.VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.live_thread_infoLayout:
                View threadTextView = ((RelativeLayout) v.getParent()).findViewById(R.id.live_thread_text);

                //todo insert threadTextView inside the viewPager, but on top of live buttons
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

    private class LiveThreadLoadingTask extends AsyncTask<String,Integer,Bitmap> {
        ImageView display;
        ProgressBar progressBar;

        public LiveThreadLoadingTask(ImageView imageView) {
            display = imageView;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar = new ProgressBar(display.getContext(),null,android.R.attr.progressBarStyleHorizontal);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Log.i("LiveThreadFragment","Decoding image in LiveThreadLoadingTask");
            return BitmapFactory.decodeFile(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            display.setImageBitmap(bitmap);

        }
    }

    public class LiveThreadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LiveFragment.VERBOSE) {
                Log.v(TAG,"received intent...");
            }

            String path = intent.getExtras().getString(Constants.KEY_S3_KEY);

            if (imageLoaderThreadReference.get() == null) {
                imageLoaderThreadReference = new WeakReference<>(new Thread(new ImageLoaderRunnable(path)));
            } else {
                imageLoaderThreadReference.get().interrupt();
                imageLoaderThreadReference = new WeakReference<>(new Thread(new ImageLoaderRunnable(path)));
            }

            imageLoaderThreadReference.get().start();
        }
    }

    private class ImageLoaderRunnable implements Runnable {

        String filepath;

        public ImageLoaderRunnable(String path) {
            filepath = path;
        }

        @Override
        public void run() {

                if (!Thread.interrupted()) {
                    image = BitmapFactory.decodeFile(getActivity().getCacheDir().toString() + "/" + filepath);

                    imageLoadHandler.post(new Runnable() {

                        public void run() {
                            if (isVisible()) {
                                progressBar.setVisibility(View.INVISIBLE);
                                displayView.setImageBitmap(image);
                            }
                        }

                    });
                } else {
                    Log.i(TAG,"thread was interrupted... canceling...");
                }
        }
    }


}