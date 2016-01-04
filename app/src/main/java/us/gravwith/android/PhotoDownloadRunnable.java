package us.gravwith.android;

/**
 * Created by ev0x on 11/12/15.
 */


import android.util.Log;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

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

class PhotoDownloadRunnable implements Runnable {
    // Sets the size for each read action (bytes)
    private static final int READ_SIZE = 1024 * 2;

    // Sets a tag for this class
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "PhotoDownloadRunnable";

    // Constants for indicating the state of the download
    static final int S3_STATE_FAILED = -1;
    static final int S3_STATE_STARTED = 0;
    static final int S3_STATE_COMPLETED = 1;

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskRunnableDownloadMethods mPhotoTask;

    /**
     *
     * An interface that defines methods that PhotoTask implements. An instance of
     * PhotoTask passes itself to an PhotoRequestDownloadRunnable instance through the
     * PhotoRequestDownloadRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    interface TaskRunnableDownloadMethods {

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
        void handleDownloadState(int state);

        /**
         * Gets the String for the image being downloaded
         * @return The image String
         */
        String getImageKey();

        /**
         * Gets the s3 bucket directory of the image to be downloaded
         * @return The image directory
         */
        String getImageDirectory();


        /**
         * Gets the s3 bucket of the image to be downloaded
         * @return The s3 bucket
         */
        String getImageBucket();


        /**
         * Gets the AmazonS3Client for the image to be downloaded with
         * @return The s3 client to use
         */
        AmazonS3Client getS3Client();


    }

    /**
     * This constructor creates an instance of PhotoRequestDownloadRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param photoTask The PhotoTask, which implements TaskRunnableDecodeMethods
     */
    PhotoDownloadRunnable(TaskRunnableDownloadMethods photoTask) {
        mPhotoTask = photoTask;
    }

    /*
     * Defines this object's task, which is a set of instructions designed to be run on a Thread.
     */
    @SuppressWarnings("resource")
    @Override
    public void run() {

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
            if (Thread.interrupted()) {

                throw new InterruptedException();
            }

            // If there's no cache buffer for this image
            if (null == byteBuffer) {

                /*
                 * Calls the PhotoTask implementation of {@link #handleDownloadState} to
                 * set the state of the download
                 */
                mPhotoTask.handleDownloadState(S3_STATE_STARTED);

                // Defines a handle for the byte download stream
                InputStream byteStream = null;

                // Downloads the image and catches IO errors
                try {



                    if (Constants.LOGV) {
                        Log.v(LOG_TAG,"bucket is : " + mPhotoTask.getImageBucket());
                        Log.v(LOG_TAG,"directory is : " + mPhotoTask.getImageDirectory());
                        Log.v(LOG_TAG,"image key is : " + mPhotoTask.getImageKey());
                    }
                    // Opens an HTTP connection to the image's URL
                    S3Object s3Object =
                            mPhotoTask.getS3Client().getObject(mPhotoTask.getImageBucket(),
                            //        mPhotoTask.getImageDirectory() + "/"
                            mPhotoTask.getImageKey());

                    // Before continuing, checks to see that the Thread
                    // hasn't been interrupted
                    if (Thread.interrupted()) {

                        throw new InterruptedException();
                    }
                    // Gets the input stream containing the image
                    byteStream = s3Object.getObjectContent();

                    if (Thread.interrupted()) {

                        throw new InterruptedException();
                    }
                    /*
                     * Gets the size of the file being downloaded. This
                     * may or may not be returned.
                     */
                    long contentSize = s3Object.getObjectMetadata().getContentLength();

                    /*
                     * If the size of the image isn't available
                     */
                    if (-1 == contentSize) {

                        // Allocates a temporary buffer
                        byte[] tempBuffer = new byte[READ_SIZE];

                        // Records the initial amount of available space
                        int bufferLeft = tempBuffer.length;

                        /*
                         * Defines the initial offset of the next available
                         * byte in the buffer, and the initial result of
                         * reading the binary
                         */
                        int bufferOffset = 0;
                        int readResult = 0;

                        /*
                         * The "outer" loop continues until all the bytes
                         * have been downloaded. The inner loop continues
                         * until the temporary buffer is full, and then
                         * allocates more buffer space.
                         */
                        outer: do {
                            while (bufferLeft > 0) {

                                /*
                                 * Reads from the URL location into
                                 * the temporary buffer, starting at the
                                 * next available free byte and reading as
                                 * many bytes as are available in the
                                 * buffer.
                                 */
                                readResult = byteStream.read(tempBuffer, bufferOffset,
                                        bufferLeft);

                                /*
                                 * InputStream.read() returns zero when the
                                 * file has been completely read.
                                 */
                                if (readResult < 0) {
                                    // The read is finished, so this breaks
                                    // the to "outer" loop
                                    break outer;
                                }

                                /*
                                 * The read isn't finished. This sets the
                                 * next available open position in the
                                 * buffer (the buffer index is 0-based).
                                 */
                                bufferOffset += readResult;

                                // Subtracts the number of bytes read from
                                // the amount of buffer left
                                bufferLeft -= readResult;

                                if (Thread.interrupted()) {

                                    throw new InterruptedException();
                                }
                            }
                            /*
                             * The temporary buffer is full, so the
                             * following code creates a new buffer that can
                             * contain the existing contents plus the next
                             * read cycle.
                             */

                            // Resets the amount of buffer left to be the
                            // max buffer size
                            bufferLeft = READ_SIZE;

                            /*
                             * Sets a new size that can contain the existing
                             * buffer's contents plus space for the next
                             * read cycle.
                             */
                            int newSize = tempBuffer.length + READ_SIZE;

                            /*
                             * Creates a new temporary buffer, moves the
                             * contents of the old temporary buffer into it,
                             * and then points the temporary buffer variable
                             * to the new buffer.
                             */
                            byte[] expandedBuffer = new byte[newSize];
                            System.arraycopy(tempBuffer, 0, expandedBuffer, 0,
                                    tempBuffer.length);
                            tempBuffer = expandedBuffer;
                        } while (true);

                        /*
                         * When the entire image has been read, this creates
                         * a permanent byte buffer with the same size as
                         * the number of used bytes in the temporary buffer
                         * (equal to the next open byte, because tempBuffer
                         * is 0=based).
                         */
                        byteBuffer = new byte[bufferOffset];

                        // Copies the temporary buffer to the image buffer
                        System.arraycopy(tempBuffer, 0, byteBuffer, 0, bufferOffset);

                        /*
                         * The download size is available, so this creates a
                         * permanent buffer of that length.
                         */
                    } else {
                        byteBuffer = new byte[((int) contentSize)];

                        // How much of the buffer still remains empty
                        int remainingLength = (int)contentSize;

                        // The next open space in the buffer
                        int bufferOffset = 0;

                        /*
                         * Reads into the buffer until the number of bytes
                         * equal to the length of the buffer (the size of
                         * the image) have been read.
                         */
                        while (remainingLength > 0) {
                            int readResult = byteStream.read(
                                    byteBuffer,
                                    bufferOffset,
                                    remainingLength);
                            /*
                             * EOF should not occur, because the loop should
                             * read the exact # of bytes in the image
                             */
                            if (readResult < 0) {

                                // Throws an EOF Exception
                                throw new EOFException();
                            }

                            // Moves the buffer offset to the next open byte
                            bufferOffset += readResult;

                            // Subtracts the # of bytes read from the
                            // remaining length
                            remainingLength -= readResult;

                            if (Thread.interrupted()) {

                                throw new InterruptedException();
                            }
                        }
                    }

                    if (Thread.interrupted()) {

                        throw new InterruptedException();
                    }

                    // If an IO error occurs, returns immediately
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (AmazonS3Exception ex) {
                    Log.e(LOG_TAG,"Amazon exception found... " + ex);
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
            mPhotoTask.handleDownloadState(S3_STATE_COMPLETED);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
        } finally {

            // If the byteBuffer is null, reports that the download failed.
            if (null == byteBuffer) {
                mPhotoTask.handleDownloadState(S3_STATE_FAILED);
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
