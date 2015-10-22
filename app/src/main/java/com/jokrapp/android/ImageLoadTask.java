package com.jokrapp.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * class 'ImageLoadTask'
 *
 * basic AsyncTask that decodes an image at the provided path in a background thread,
 * and then displays
 *
 * Created by John C. Quinn on 10/21/15.
 */
public class ImageLoadTask extends AsyncTask<String,Integer,Bitmap> {
    WeakReference<ImageView> imageViewWeakReference;
    WeakReference<ProgressBar> progressBarWeakReference;


    public ImageLoadTask(ImageView v, ProgressBar b) {
        imageViewWeakReference = new WeakReference<>(v);
        progressBarWeakReference= new WeakReference<>(b);
    }

    @Override
    protected Bitmap doInBackground(String... params) {

        String path = params[0];
        File f = new File(path);

        if (f.exists()) {
            if (Constants.LOGV) Log.v("ImageLoadTask", "file exists - decoding image at path : " + path);
            return BitmapFactory.decodeFile(path);
        } else {
            Log.e("ImageLoadTask", "path does not exist : " + path);
            return null;
        }

    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        imageViewWeakReference.get().setImageBitmap(bitmap);
        progressBarWeakReference.get().setVisibility(View.INVISIBLE);
    }
}
