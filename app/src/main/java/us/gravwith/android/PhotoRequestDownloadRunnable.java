package us.gravwith.android;

/*
 * Copyright (C) ${year} The Android Open Source Project
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
        import android.os.Bundle;
        import android.os.Message;
        import android.os.Messenger;
        import android.os.RemoteException;
        import android.util.Log;

        import us.gravwith.android.PhotoDecodeRunnable.TaskRunnableDecodeMethods;
        import us.gravwith.android.util.LogUtils;

/**
 * This task downloads bytes from a resource addressed by a URL.  When the task
 * has finished, it calls handleState to report its results.
 *
 * Objects of this class are instantiated and managed by instances of PhotoTask, which
 * implements the methods of {@link TaskRunnableDecodeMethods}. PhotoTask objects call
 * {@link #PhotoRequestDownloadRunnable(TaskRunnableRequestMethods) PhotoRequestDownloadRunnable()} with
 * themselves as the argument. In effect, an PhotoTask object and a
 * PhotoRequestDownloadRunnable object communicate through the fields of the PhotoTask.
 */
class PhotoRequestDownloadRunnable implements Runnable {
    // Sets the size for each read action (bytes)
    private static final int READ_SIZE = 1024 * 2;

    // Sets a tag for this class
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "PhotoRequestRunnable";

    // Constants for indicating the state of the download
    static final int REQUEST_STATE_FAILED = -1;
    static final int REQUEST_STATE_STARTED = 0;
    static final int REQUEST_STATE_COMPLETED = 1;

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskRunnableRequestMethods mPhotoTask;


    /**
     *
     * An interface that defines methods that PhotoTask implements. An instance of
     * PhotoTask passes itself to an PhotoRequestDownloadRunnable instance through the
     * PhotoRequestDownloadRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    interface TaskRunnableRequestMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setDownloadThread(Thread currentThread);

        /**
         * Returns the current contents of the download buffer
         * @return The byte array downloaded from the URL in the last read
         */
        byte[] getByteBuffer();

        /**
         * Sets the current contents of the download buffer
         * @param buffer The bytes that were just read
         */
        void setByteBuffer(byte[] buffer);

        /**
         * Defines the actions for each state of the PhotoTask instance.
         * @param state The current state of the task
         */
        void handleRequestState(int state);

        /**
         * Gets the URL for the image being downloaded
         * @return The image URL
         */
        String getImageKey();

        String getImageDirectory();

        Messenger getmService();

        Messenger getResponseMessenger();
    }

    /**
     * This constructor creates an instance of PhotoRequestDownloadRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param photoTask The PhotoTask, which implements TaskRunnableDecodeMethods
     */
    PhotoRequestDownloadRunnable(TaskRunnableRequestMethods photoTask) {
        mPhotoTask = photoTask;
    }

    /*
     * Defines this object's task, which is a set of instructions designed to be run on a Thread.
     */
    @SuppressWarnings("resource")
    @Override
    public void run() {
        if (Constants.LOGD) {
            Log.d(LOG_TAG,"entering run...");
        }

        Messenger serviceMessenger = mPhotoTask.getmService();
        if (serviceMessenger == null ) {
            Log.e(LOG_TAG,"requestMessenger was null... exiting...");
            // Sets the reference to the current Thread to null, releasing its storage
            mPhotoTask.setDownloadThread(null);
            // Clears the Thread's interrupt flag
            Thread.interrupted();
            return;
        }
     //   Log.w(LOG_TAG,"Downloading from PhotoRequestDownloadRunnable is not available... please try again...");

        /*
         * Stores the current Thread in the the PhotoTask instance, so that the instance
         * can interrupt the Thread.
         */
        mPhotoTask.setDownloadThread(Thread.currentThread());

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        /*
         * Gets the image cache buffer object from the PhotoTask instance. This makes the
         * to both PhotoRequestDownloadRunnable and PhotoTask.
         */
        byte[] byteBuffer = mPhotoTask.getByteBuffer();

        /*
         * A try block that downloads a Picasa image from a URL. The URL value is in the field
         * PhotoTask.mImageURL
         */
        // Tries to download the picture from Picasa
        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted

            // If there's no cache buffer for this image
            if (null == byteBuffer) {

                Message msg = Message.obtain(null,DataHandlingService.MSG_DOWNLOAD_IMAGE);
                msg.replyTo = mPhotoTask.getResponseMessenger();

                if (Constants.LOGV) Log.v(LOG_TAG,"printing bundle for message about to be sent.");

                Bundle b = new Bundle();
                b.putString(Constants.KEY_S3_DIRECTORY,mPhotoTask.getImageDirectory());
                b.putString(Constants.KEY_S3_KEY, mPhotoTask.getImageKey());
                msg.setData(b);

                serviceMessenger.send(msg);

                LogUtils.printBundle(b, LOG_TAG);

                if (Constants.LOGV) Log.v(LOG_TAG,"Message sent.");

                mPhotoTask.handleRequestState(REQUEST_STATE_STARTED);
            }

            /*
             * Sets the status message in the PhotoTask instance. This sets the
             * ImageView background to indicate that the image is being
             * decoded.
             */

            // Catches exceptions thrown in response to a queued interrupt
        } catch (RemoteException e) {
            // Does nothing
            mPhotoTask.handleRequestState(REQUEST_STATE_FAILED);
            // In all cases, handle the results
        }finally {

            // If the byteBuffer is null, reports that the download failed.
            if (null == byteBuffer) {

//                mPhotoTask.handleDownloadState(HTTP_STATE_COMPLETED);
  //              mPhotoTask.handleDownloadState(HTTP_STATE_FAILED);
            }

            /*
             * The implementation of setHTTPDownloadThread() in PhotoTask calls
             * PhotoTask.setCurrentThread(), which then locks on the static ThreadPool
             * object and returns the current thread. Locking keeps all references to Thread
             * objects the same until the reference to the current Thread is deleted.
             */

            // Sets the reference to the current Thread to null, releasing its storage
            mPhotoTask.setDownloadThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }
    }
}