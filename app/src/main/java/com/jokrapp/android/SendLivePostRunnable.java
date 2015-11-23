package com.jokrapp.android;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jokrapp.android.SQLiteDbContract.LiveRepliesEntry;
import com.jokrapp.android.util.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by John C. Quinn on 11/14/15.
 *
 * class 'SendLocalBlockRunnable'
 *
 * sends a message to block the provided user to the server
 */
/***********************************************************************************************
 *
 *   IMAGE SENDING
 *
 */

/**
 * Handle action send images in the provided background thread with the provided
 * parameters.
 * <p/>
 * the client sends the latitude and longitude stored in the location passed in the arguments
 * and the image stored at the filepath provided
 * the sending process uses URLConnection to create the connection
 * the apache commons library and jackson's json parser are both utilized
 * <p/>
 * the server marks the time the image is received, and gives the image an ID
 *
 */
public class SendLivePostRunnable implements Runnable{

    interface LivePostMethods {

        void handleLivePostState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpURLConnection getURLConnection();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final boolean VERBOSE = true;

    private final String TAG = "SendLivePostRunnable";

    private final LivePostMethods mTask;

    public SendLivePostRunnable(LivePostMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendLivePostRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());


        boolean success = true;
        HttpURLConnection conn = null;
        Bundle b = mTask.getDataBundle();
        if (VERBOSE) LogUtils.printBundle(b, TAG);

        String imageKey = b.getString(LiveRepliesEntry.COLUMN_NAME_FILEPATH, "");

        int responseCode = -10;
        try {
            conn = mTask.getURLConnection();
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            jGen.writeStartObject();
            jGen.writeStringField(LiveRepliesEntry.COLUMN_NAME_TITLE,
                    b.getString(LiveRepliesEntry.COLUMN_NAME_TITLE,"")
            );
            jGen.writeStringField(LiveRepliesEntry.COLUMN_NAME_NAME,
                    b.getString(LiveRepliesEntry.COLUMN_NAME_NAME,"")
            );
            jGen.writeStringField(LiveRepliesEntry.COLUMN_NAME_DESCRIPTION,
                    b.getString(LiveRepliesEntry.COLUMN_NAME_DESCRIPTION,"")
            );
            jGen.writeStringField(LiveRepliesEntry.COLUMN_NAME_FILEPATH,
                    imageKey
            );
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
            success = false;
        } finally {
            if (success) {
                mTask.handleLivePostState(REQUEST_SUCCESS);
                File file = new File(imageKey);
                Log.i(TAG,"file is stored at" + imageKey);
                file.delete();
            } else {
                mTask.handleLivePostState(REQUEST_FAILED);
                //todo retry request
            }

            if (conn != null) {
                conn.disconnect();
            }
        }


        mTask.setTaskThread(null);
        if (VERBOSE) {
            Log.v(TAG, "exiting SendLivePostRunnable...");
        }
    }
}
