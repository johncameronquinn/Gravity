package com.jokrapp.android;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * fragment 'LiveThreadFragment'
 *
 *  Created by John C. Quinn on 8/20/15.
 *
 */
public class LiveThreadFragment extends Fragment {

    private static final String TAG = "LiveThreadFragment";

    private static final String ARG_THREAD_NUM = "tn";
    private static final String ARG_THREAD_NAME = "tna";
    private static final String ARG_THREAD_TITLE = "tt";
    private static final String ARG_THREAD_FILEPATH = "fp";

    private int threadNum;
    private String threadName;
    private String threadTitle;
    private String threadFilePath;
    private WeakReference<Handler> progressHandlerReference;
    private ImageView displayView;
    private Bitmap image;

    Handler imageLoadHandler = new Handler();

    static LiveThreadFragment newInstance(int threadPosition,
                                          String name,
                                          String title,
                                          String filePath)
    {
        LiveThreadFragment f = new LiveThreadFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt(ARG_THREAD_NUM, threadPosition);
        args.putString(ARG_THREAD_NAME, name);
        args.putString(ARG_THREAD_TITLE,title);
        args.putString(ARG_THREAD_FILEPATH,filePath);
        f.setArguments(args);

        return f;
    }

    public LiveThreadFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Bundle args = getArguments();
            threadNum = args.getInt(ARG_THREAD_NUM);
            threadName = args.getString(ARG_THREAD_NAME);
            threadTitle = args.getString(ARG_THREAD_TITLE);
            threadFilePath = args.getString(ARG_THREAD_FILEPATH);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_thread,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
     //   ((TextView)view.findViewById(R.id.live_thread_number)).setText(String.valueOf(threadNum));
        ((TextView)view.findViewById(R.id.live_thread_name)).setText(threadName);
        ((TextView)view.findViewById(R.id.live_thread_title)).setText(threadTitle);

        displayView = ((ImageView) view.findViewById(R.id.live_thread_imageView));

        // new LiveThreadLoadingTask((ImageView) view.findViewById(R.id.live_thread_imageView)).
        //         execute(threadFilePath);

        new Thread(new Runnable() {
            @Override
            public void run() {

                Uri uri = Uri.withAppendedPath(FireFlyContentProvider
                        .CONTENT_URI_LIVE_THREAD_INFO,String.valueOf(threadNum));
                try {
                    image = BitmapFactory.decodeStream(getActivity()
                            .getContentResolver()
                            .openInputStream(uri));
                    imageLoadHandler.post(new Runnable() {

                        public void run() {
                            displayView.setImageBitmap(image);

                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "file returned from content provider was not found - Uri: " + uri.toString(), e);
                }
            }

        }).start();
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