package com.jokrapp.android;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jokrapp.android.SQLiteDbContract.MessageEntry;
import com.jokrapp.android.util.LogUtils;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;

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
public class SendMessageRunnable implements Runnable{

    interface MessageMethods {

        void handleMessageState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpsURLConnection getURLConnection();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final String TO = "to";

    private final boolean VERBOSE = true;

    private final String TAG = "SendMessageRunnable";

    private final MessageMethods mTask;

    public SendMessageRunnable(MessageMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendMessageRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());

        HttpsURLConnection conn = null;
        Bundle b = mTask.getDataBundle();
        if (VERBOSE) LogUtils.printBundle(b, TAG);

        String text = b.getString(MessageEntry.COLUMN_NAME_TEXT, "");
        String imageKey = b.getString(MessageEntry.COLUMN_NAME_FILEPATH, "");
        String messageTarget = b.getString(Constants.MESSAGE_TARGET);

        if (!"".equals(messageTarget)) {
            if (VERBOSE) {
                Log.v(TAG,"sending image message to :" + messageTarget);
            } else {
                Log.e(TAG,"messageTarget is null...");
                mTask.handleMessageState(REQUEST_FAILED);
                return;
            }
        }


        int responseCode = -10;
        try {
            conn = mTask.getURLConnection();
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server
            jGen.writeStartObject();
            jGen.writeStringField(TO, messageTarget);
            jGen.writeStringField(MessageEntry.COLUMN_NAME_FILEPATH, imageKey);
            jGen.writeStringField(Constants.KEY_TEXT, text);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
            mTask.handleMessageState(REQUEST_FAILED);
        } finally {
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                mTask.handleMessageState(REQUEST_SUCCESS);
                File file = new File(imageKey);
                Log.i(TAG,"file is stored at" + imageKey);
                file.delete();
            } else {
                mTask.handleMessageState(REQUEST_FAILED);
                //todo retry request
            }

            if (conn != null) {
                conn.disconnect();
            }
        }


        mTask.setTaskThread(null);
        if (VERBOSE) {
            Log.v(TAG, "exiting SendMessageRunnable...");
        }
    }
}
