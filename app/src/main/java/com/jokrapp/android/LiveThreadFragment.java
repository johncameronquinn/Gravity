package com.jokrapp.android;

import android.app.Fragment;
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

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * fragment 'LiveThreadFragment'
 *
 *  Created by John C. Quinn on 8/20/15.
 *
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

    private WeakReference<Handler> progressHandlerReference;
    private ImageView displayView;
    private Bitmap image;

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

        // new LiveThreadLoadingTask((ImageView) view.findViewById(R.id.live_thread_imageView)).
        //         execute(threadFilePath);

       /* new Thread(new Runnable() {
            @Override
            public void run() {

                Uri uri = Uri.withAppendedPath(FireFlyContentProvider
                        .CONTENT_URI_LIVE_THREAD_LIST,String.valueOf(threadID));
                try {
                    if (isAdded()) {
                        image = BitmapFactory.decodeStream(getActivity()
                                .getContentResolver()
                                .openInputStream(uri));
                        imageLoadHandler.post(new Runnable() {

                            public void run() {
                                displayView.setImageBitmap(image);

                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "file returned from content provider was not found - Uri: " + uri.toString(), e);
                }
            }

        }).start();*/


       new Thread(new Runnable() {
            @Override
            public void run() {

                if (isAdded()) {

                    image = BitmapFactory.decodeFile(getActivity().getCacheDir().toString()+"/"+threadFilePath);

                    imageLoadHandler.post(new Runnable() {

                        public void run() {
                            displayView.setImageBitmap(image);
                        }

                    });

                }

            }

            }).start();





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

    public void setProgressHandler(Handler handler) {
        progressHandlerReference = new WeakReference<>(handler);
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
}