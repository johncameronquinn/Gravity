/*
 * Copyright (c) 2015. John C Quinn, All Rights Reserved.
 */

package com.jokrapp.android;

/**
 * Created by ev0x on 9/21/15.
 *//*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



        import android.content.Intent;
        import android.os.Bundle;
        import android.app.Fragment;
        import android.support.v4.app.ShareCompat;
        import android.support.v4.content.LocalBroadcastManager;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;

        import java.net.MalformedURLException;
        import java.net.URL;

public class PhotoFragment extends Fragment implements View.OnClickListener {
    // Constants
    private static final String LOG_TAG = "ImageDownloaderThread";
    private static final String PHOTO_URL_KEY = "com.jokrapp.android.PHOTO_URL_KEY";
    private static final String TAG = "PhotoFragment";

    PhotoView mPhotoView;

    String mImageKey;
    String mImageDirectory;

    ShareCompat.IntentBuilder mShareCompatIntentBuilder;

    /**
     * Converts the stored URL string to a URL, and then tries to download the picture from that
     * URL.
     */
    public void loadPhoto() {
        if (Constants.LOGV) Log.v(TAG,"entering loadPhoto...");

        // If setPhoto() was called to store a URL, proceed
        if (mImageKey != null) {
            Log.d(TAG, "mImageKey is not null: " + mImageKey);


         /*
          * setImageURL(url,false,null) attempts to download and decode the picture at
          * at "url" without caching and without providing a Drawable. The result will be
          * a BitMap stored in the PhotoView for this Fragment.
          */
            mPhotoView.setImageKey(mImageDirectory,mImageKey, false, null);

        }


        if (Constants.LOGV) Log.v(TAG,"exiting loadPhoto...");
    }
    /**
     * Returns the stored URL string
     * @return The URL of the picture being shown by this Fragment, in String format
     */
    public String getImageKeyString() {
        return mImageKey;
    }

    /*
     * This callback is invoked when users click on a displayed image. The input argument is
     * a handle to the View object that was clicked
     */
    @Override
    public void onClick(View view) {
        if (Constants.LOGV) Log.v(TAG,"entering onClick with view: " + view.toString());
        // Sends a broadcast intent to zoom the image
        if (Constants.LOGV) Log.v(TAG,"sending broadcast to zoom the image");
        Intent localIntent = new Intent(Constants.ACTION_ZOOM_IMAGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(localIntent);

        if (Constants.LOGV) Log.v(TAG,"exiting onClick...");
    }

    /*
     * This callback is invoked when the Fragment is created.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    /*
     * This callback is invoked as the Fragment's View is being constructed.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(inflater, viewGroup, bundle);
        if (Constants.LOGV) Log.v(TAG,"entering onCreateView...");
        /*
         * Creates a View from the specified layout file. The layout uses the parameters specified
         * in viewGroup, but is not attached to any parent
         */
        View localView = inflater.inflate(R.layout.photo, viewGroup, false);

        // Gets a handle to the PhotoView View in the layout
        mPhotoView = ((PhotoView) localView.findViewById(R.id.photoView));

        /*
         * The click listener becomes this class (PhotoFragment). The onClick() method in this
         * class is invoked when users click a photo.
         */
        mPhotoView.setOnClickListener(this);

        // If the bundle argument contains data, uses it as a URL for the picture to display
        if (bundle != null) {
            mImageKey = bundle.getString(Constants.KEY_S3_KEY);
            mImageDirectory = bundle.getString(Constants.KEY_S3_DIRECTORY);
            if (Constants.LOGV) Log.v(TAG,"Loaded key: " + mImageKey);
        }

        if (mImageKey != null)
            loadPhoto();

        // Returns the resulting View

        if (Constants.LOGV) Log.v(TAG,"exiting onCreateView...");
        return localView;
    }

    /*
     * This callback is invoked as the Fragment's View is being destroyed
     */
    @Override
    public void onDestroyView() {
        // Logs the destroy operation
        Log.d(LOG_TAG, "onDestroyView");

        // If the View object still exists, delete references to avoid memory leaks
        if (mPhotoView != null) {

            mPhotoView.setOnClickListener(null);
            this.mPhotoView = null;
        }

        // Always call the super method last
        super.onDestroyView();
    }

    /*
     * This callback is invoked when the Fragment is no longer attached to its Activity.
     * Sets the URL for the Fragment to null
     */
    @Override
    public void onDetach() {
        // Logs the detach
        Log.d(LOG_TAG, "onDetach");

        // Removes the reference to the URL
        mImageKey = null;
        mImageDirectory = null;

        // Always call the super method last
        super.onDetach();
    }

    /*
     * This callback is invoked if the system asks the Fragment to save its state. This allows the
     * the system to restart the Fragment later on.
     */
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        // Always call the super method first
        super.onSaveInstanceState(bundle);
        if (Constants.LOGV) Log.v(TAG,"entering onSaveInstanceState...");
        // Puts the current URL for the picture being shown into the saved state
        bundle.putString(Constants.KEY_S3_KEY, mImageKey);
        bundle.putString(Constants.KEY_S3_DIRECTORY,mImageDirectory);

        if (Constants.LOGV) Log.v(TAG,"exiting onSaveInstanceState...");
    }

    /**
     * Sets the photo for this Fragment, by storing a URL that points to a picture
     * @param imageKey A String representation of the URL pointing to the picture
     */
    public void setPhoto(String s3Directory, String imageKey) {
        mImageKey = imageKey;
        mImageDirectory = s3Directory;
        if (Constants.LOGV) Log.v(LOG_TAG,"urlString provided is: " + imageKey);
    }
}
