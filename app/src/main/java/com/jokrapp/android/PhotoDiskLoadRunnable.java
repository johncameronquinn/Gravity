package com.jokrapp.android;

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


import android.util.Log;

import com.jokrapp.android.PhotoDecodeRunnable.TaskRunnableDecodeMethods;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;




/**
 * This task downloads bytes from a resource addressed by a URL.  When the task
 * has finished, it calls handleState to report its results.
 *
 * Objects of this class are instantiated and managed by instances of PhotoTask, which
 * implements the methods of {@link TaskRunnableDecodeMethods}. PhotoTask objects call
 * {@link #PhotoDiskLoadRunnable(TaskRunnableDiskLoadMethods) PhotoRequestDownloadRunnable ()} with
 * themselves as the argument. In effect, an PhotoTask object and a
 * PhotoRequestDownloadRunnable object communicate through the fields of the PhotoTask.
 */
class PhotoDiskLoadRunnable implements Runnable {
    // Sets the size for each read action (bytes)
    private static final int READ_SIZE = 1024 * 2;

    // Sets a tag for this class
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "PhotoDiskloadRunnable";

    // Constants for indicating the state of the download
    static final int DISKLOAD_STATE_FAILED = -1;
    static final int DISKLOAD_STATE_STARTED = 0;
    static final int DISKLOAD_STATE_COMPLETED = 1;

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskRunnableDiskLoadMethods mPhotoTask;




    /**
     *
     * An interface that defines methods that PhotoTask implements. An instance of
     * PhotoTask passes itself to an PhotoRequestDownloadRunnable instance through the
     * PhotoRequestDownloadRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    interface TaskRunnableDiskLoadMethods {

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
        void handleDiskloadState(int state);

        /**
         * Gets the URL for the image being downloaded
         * @return The image URL
         */
        String getImageKey();


        File getCacheDirectory();
    }


    /**
     * This constructor creates an instance of PhotoRequestDownloadRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param photoTask The PhotoTask, which implements TaskRunnableDecodeMethods
     */
    PhotoDiskLoadRunnable(TaskRunnableDiskLoadMethods photoTask) {
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

        /*
         * Stores the current Thread in the the PhotoTask instance, so that the instance
         * can interrupt the Thread.
         */
        mPhotoTask.setDownloadThread(Thread.currentThread());

        // Moves the current Thread into the background
        //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

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
            if (Thread.interrupted()) {

                throw new InterruptedException();
            }

            // If there's no cache buffer for this image
            if (null == byteBuffer) {

                /*
                 * Calls the PhotoTask implementation of {@link #handleDownloadState} to
                 * set the state of the download
                 */
                mPhotoTask.handleDiskloadState(DISKLOAD_STATE_STARTED);

                // Defines a handle for the byte download stream
                InputStream byteStream = null;
                File f = new File(mPhotoTask.getCacheDirectory()+PhotoManager.STORAGE_PREFIX,mPhotoTask.getImageKey());

                // Downloads the image and catches IO errors
                try {
                    // Before continuing, checks to see that the Thread
                    // hasn't been interrupted
                    if (Thread.interrupted()) {

                        throw new InterruptedException();
                    }
                    // Gets the input stream containing the image
                    byteStream = new FileInputStream(f);

                    if (Thread.interrupted()) {

                        throw new InterruptedException();
                    }

                    byteBuffer = com.amazonaws.util.IOUtils.toByteArray(byteStream);

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    // If an IO error occurs, returns immediately
                } catch (IOException e) {
                    e.printStackTrace();
                    return;

                    /*
                     * If the input stream is still open, close it
                     */
                } finally {
                    if (null != byteStream) {
                        try {
                            byteStream.close();
                        } catch (Exception e) {

                        }
                    }
                }
            }

            /*
             * Stores the downloaded bytes in the byte buffer in the PhotoTask instance.
             */
            mPhotoTask.setByteBuffer(byteBuffer);

            /*
             * Sets the status message in the PhotoTask instance. This sets the
             * ImageView background to indicate that the image is being
             * decoded.
             */
            mPhotoTask.handleDiskloadState(DISKLOAD_STATE_COMPLETED);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
        } finally {

            // If the byteBuffer is null, reports that the download failed.
            if (null == byteBuffer) {
                mPhotoTask.handleDiskloadState(DISKLOAD_STATE_FAILED);
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