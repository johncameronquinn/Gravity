/*
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

package com.jokrapp.android;
import com.amazonaws.services.s3.AmazonS3Client;
import com.jokrapp.android.PhotoDecodeRunnable.TaskRunnableDecodeMethods;
import com.jokrapp.android.PhotoDownloadRunnable.TaskRunnableDownloadMethods;
import com.jokrapp.android.PhotoDiskLoadRunnable.TaskRunnableDiskLoadMethods;
import com.jokrapp.android.PhotoRequestDownloadRunnable.TaskRunnableRequestMethods;

import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * This class manages PhotoRequestDownloadRunnable and PhotoRequestDownloadRunnable objects.  It does't perform
 * the download or decode; instead, it manages persistent storage for the tasks that do the work.
 * It does this by implementing the interfaces that the download and decode classes define, and
 * then passing itself as an argument to the constructor of a download or decode object. In effect,
 * this allows PhotoTask to start on a Thread, run a download in a delegate object, then
 * run a decode, and then start over again. This class can be pooled and reused as necessary.
 */
public class PhotoTask implements
        TaskRunnableDiskLoadMethods, TaskRunnableDecodeMethods, TaskRunnableDownloadMethods, TaskRunnableRequestMethods {

    /*
     * Creates a weak reference to the ImageView that this Task will populate.
     * The weak reference prevents memory leaks and crashes, because it
     * automatically tracks the "state" of the variable it backs. If the
     * reference becomes invalid, the weak reference is garbage- collected. This
     * technique is important for referring to objects that are part of a
     * component lifecycle. Using a hard reference may cause memory leaks as the
     * value continues to change; even worse, it can cause crashes if the
     * underlying component is destroyed. Using a weak reference to a View
     * ensures that the reference is more transitory in nature.
     */
    private WeakReference<PhotoView> mImageWeakRef;

    // The image's URL
    private String mImageKey;

    private String mImageDirectory;

    private File mCacheDirectory;

    // The width and height of the decoded image
    private int mTargetHeight;
    private int mTargetWidth;

    private final String TAG = "PhotoTask";
    // Is the cache enabled for this transaction?
    private boolean mCacheEnabled;

    private HandlerThread handlerThread = new HandlerThread(
            "DownloadStateHandlerThread");
    /*
     * Field containing the Thread this task is running on.
     */
    Thread mThreadThis;

    /*
     * Fields containing references to the two runnable objects that handle downloading and
     * decoding of the image.
     */
    private Runnable mPhotoDownloadRunnable;
    private Runnable mDecodeRunnable;
    private Runnable mRequestRunnable;
    private Runnable mDiskDecodeRunnable;

    private Runnable mDownloadRunnable;

    // A buffer for containing the bytes that make up the image
    byte[] mImageBuffer;

    // the Amazon s3 client required for security
    AmazonS3Client mS3Client;

    // The decoded image
    private Bitmap mDecodedImage;

    // The Thread on which this task is currently running.
    private Thread mCurrentThread;

    /*
     * An object that contains the ThreadPool singleton.
     */
    private static PhotoManager sPhotoManager;

    private Messenger mMessenger;

    /**
     * Creates an PhotoTask containing a download object and a decoder object.
     */
    PhotoTask(Handler responseHandler) {
        // Create the runnables
        mDecodeRunnable = new PhotoDecodeRunnable(this);

        sPhotoManager = PhotoManager.getInstance();

        mMessenger = new Messenger(responseHandler);
    }

    /**
     * Initializes the Task
     *
     * @param photoManager A ThreadPool object
     * @param photoView An ImageView instance that shows the downloaded image
     * @param cacheFlag Whether caching is enabled
     */
    void initializeDownloaderTask(
            PhotoManager photoManager,
            PhotoView photoView,
            boolean cacheFlag,
            AmazonS3Client s3Client)
    {
        // Sets this object's ThreadPool field to be the input argument
        sPhotoManager = photoManager;

        // Gets the URL for the View
        mImageKey = photoView.getImageKey();
        mImageDirectory = photoView.getmImageDirectory();

        if (mImageDirectory.equals(Constants.KEY_S3_LOCAL_DIRECTORY) ||
                mImageDirectory.equals(Constants.KEY_S3_MESSAGE_DIRECTORY)) {
            mPhotoDownloadRunnable = new PhotoDownloadRunnable(this);
        } else {
            mRequestRunnable = new PhotoRequestDownloadRunnable(this);
            mDiskDecodeRunnable = new PhotoDiskLoadRunnable(this);
        }



        // Instantiates the weak reference to the incoming view
        mImageWeakRef = new WeakReference<>(photoView);
        mCacheDirectory = photoView.getContext().getCacheDir();

        // Sets the cache flag to the input argument
        mCacheEnabled = cacheFlag;

        // Gets the width and height of the provided ImageView
        mTargetWidth = photoView.getWidth();
        mTargetHeight = photoView.getHeight();

        mS3Client = s3Client;

    }

    // Implements HTTPDownloaderRunnable.getByteBuffer
    @Override
    public byte[] getByteBuffer() {

        // Returns the global field
        return mImageBuffer;
    }

    /**
     * Recycles an PhotoTask object before it's put back into the pool. One reason to do
     * this is to avoid memory leaks.
     */
    void recycle() {

        // Deletes the weak reference to the imageView
        if ( null != mImageWeakRef ) {
            mImageWeakRef.clear();
            mImageWeakRef = null;
        }

        // Releases references to the byte buffer and the BitMap
        mImageBuffer = null;
        mDecodedImage = null;
    }

    // Implements PhotoRequestDownloadRunnable.getTargetWidth. Returns the global target width.
    @Override
    public int getTargetWidth() {
        return mTargetWidth;
    }

    // Implements PhotoRequestDownloadRunnable.getTargetHeight. Returns the global target height.
    @Override
    public int getTargetHeight() {
        return mTargetHeight;
    }

    // Detects the state of caching
    boolean isCacheEnabled() {
        return mCacheEnabled;
    }

    // Implements PhotoRequestDownloadRunnable.getImageKey. Returns the global Image Key.
    @Override
    public String getImageKey() {
        return mImageKey;
    }

    @Override
    public String getImageDirectory() {
        return mImageDirectory;
    }

    @Override
    public String getImageBucket() {
        return "launch-zone"; //todo add multi-bucket support
    }


    @Override
    public AmazonS3Client getS3Client() { return mS3Client; }

    @Override
    public File getCacheDirectory() {
        return mCacheDirectory;
    }

    // Implements PhotoRequestDownloadRunnable.setByteBuffer. Sets the image buffer to a buffer object.
    @Override
    public void setByteBuffer(byte[] imageBuffer) {
        mImageBuffer = imageBuffer;
    }

    // Delegates handling the current state of the task to the PhotoManager object
    void handleState(int state) {
        sPhotoManager.handleState(this, state);
    }

    // Returns the image that PhotoDecodeRunnable decoded.
    Bitmap getImage() {
        return mDecodedImage;
    }

    // Returns the instance that downloaded the image
    Runnable getS3DownloadRunnable() {
        mDownloadRunnable = mPhotoDownloadRunnable;
        return mPhotoDownloadRunnable;
    }

    // Returns the runnable to request a download from the service
    Runnable getDownloadRequestRunnable() {
        mDownloadRunnable = mRequestRunnable;
        return mRequestRunnable; }

    // Returns the instance that downloaded the image
    Runnable getDiskloadRunnable() {
        mDownloadRunnable = mDiskDecodeRunnable;
        return mDiskDecodeRunnable;
    }

    // Returns the instance that decode the image
    Runnable getPhotoDecodeRunnable() {
        return mDecodeRunnable;
    }

    // Returns the instance that downloaded the image
    Runnable getDownloadRunnable() {
        return mDownloadRunnable;
    }


    public Messenger getResponseMessenger() {
        return mMessenger;
    }


    public void setResponseMessenger(Messenger messenger) {
        mMessenger = messenger;
    }

    @Override
    public Messenger getmService() {
        return ((MainActivity)mImageWeakRef.get().getContext()).getMessenger();
    }


    // Returns the ImageView that's being constructed.
    public PhotoView getPhotoView() {
        if ( null != mImageWeakRef ) {
            return mImageWeakRef.get();
        }
        return null;
    }

    /*
     * Returns the Thread that this Task is running on. The method must first get a lock on a
     * static field, in this case the ThreadPool singleton. The lock is needed because the
     * Thread object reference is stored in the Thread object itself, and that object can be
     * changed by processes outside of this app.
     */
    public Thread getCurrentThread() {
        synchronized(sPhotoManager) {
            return mCurrentThread;
        }
    }

    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    public void setCurrentThread(Thread thread) {
        synchronized(sPhotoManager) {
            mCurrentThread = thread;
        }
    }

    // Implements ImageCoderRunnable.setImage(). Sets the Bitmap for the current image.
    @Override
    public void setImage(Bitmap decodedImage) {
        mDecodedImage = decodedImage;
    }

    // Implements PhotoRequestDownloadRunnable.setHTTPDownloadThread(). Calls setCurrentThread().
    @Override
    public void setDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    /*
     * Implements PhotoRequestDownloadRunnable.handleHTTPState(). Passes the download state to the
     * ThreadPool object.
     */

    @Override
    public void handleDownloadState(int state) {
        int outState;

        // Converts the download state to the overall state
        switch(state) {
            case PhotoDownloadRunnable.S3_STATE_COMPLETED:
                if (Constants.LOGV) Log.v(TAG, "S3 Download completed...");

                outState = PhotoManager.DOWNLOAD_COMPLETE;
                break;
            case PhotoDownloadRunnable.S3_STATE_FAILED:
                if (Constants.LOGV) Log.v(TAG, "S3 Download failed...");

                outState = PhotoManager.DOWNLOAD_FAILED;
                break;
            default:
                if (Constants.LOGV) Log.v(TAG, "Download started...");

                outState = PhotoManager.DOWNLOAD_STARTED;
                break;
        }
        // Passes the state to the ThreadPool object.
        handleState(outState);
    }

    @Override
    public void handleRequestState(int state) {
        int outState;

        // Converts the download state to the overall state
        switch(state) {
            case PhotoRequestDownloadRunnable.REQUEST_STATE_COMPLETED:
                if (Constants.LOGV) Log.v(TAG, "Request completed...");

                outState = PhotoManager.REQUEST_COMPLETE;
                break;
            case PhotoRequestDownloadRunnable.REQUEST_STATE_FAILED:
                if (Constants.LOGV) Log.v(TAG, "Request failed...");

                outState = PhotoManager.REQUEST_FAILED;
                break;
            default:
                if (Constants.LOGV) Log.v(TAG, "Request started...");

                outState = PhotoManager.REQUEST_STARTED;
                break;
        }
        // Passes the state to the ThreadPool object.
        handleState(outState);
    }

    // Implements PhotoDecodeRunnable.setImageDecodeThread(). Calls setCurrentThread().
    @Override
    public void setImageDecodeThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    /*
     * Implements PhotoDecodeRunnable.handleDecodeState(). Passes the decoding state to the
     * ThreadPool object.
     */
    @Override
    public void handleDecodeState(int state) {
        int outState;

        // Converts the decode state to the overall state.
        switch(state) {
            case PhotoDecodeRunnable.DECODE_STATE_COMPLETED:
                if (Constants.LOGV) Log.v(TAG, "Decode completed...");
                outState = PhotoManager.TASK_COMPLETE;

                break;
            case PhotoDecodeRunnable.DECODE_STATE_FAILED:
                if (Constants.LOGV) Log.v(TAG, "Decode failed...");

                outState = PhotoManager.DOWNLOAD_FAILED;
                break;
            default:
                if (Constants.LOGV) Log.v(TAG, "Decode started...");

                outState = PhotoManager.DECODE_STARTED;
                break;
        }

        // Passes the state to the ThreadPool object.
        handleState(outState);
    }

    @Override
    public void handleDiskloadState(int state) {
        int outState;

        // Converts the decode state to the overall state.
        switch(state) {
            case PhotoDiskLoadRunnable.DISKLOAD_STATE_COMPLETED:
                if (Constants.LOGV) Log.v(TAG, "Diskload completed...");
                outState = PhotoManager.DISKLOAD_COMPLETE;

                break;
            case PhotoDiskLoadRunnable.DISKLOAD_STATE_FAILED:
                if (Constants.LOGV) Log.v(TAG, "Diskload failed...");

                outState = PhotoManager.DOWNLOAD_FAILED;
                break;
            default:
                if (Constants.LOGV) Log.v(TAG, "Diskload started...");

                outState = PhotoManager.DISKLOAD_STARTED;
                break;
        }

        // Passes the state to the ThreadPool object.
        handleState(outState);
    }

}
