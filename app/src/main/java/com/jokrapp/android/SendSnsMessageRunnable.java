package com.jokrapp.android;

import android.os.Bundle;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobile.AWSMobileClient;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jokrapp.android.SQLiteDbContract.MessageEntry;
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
public class SendSnsMessageRunnable implements Runnable{

    interface MessageMethods {

        void handleMessageState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final String TO = "to";

    private final boolean VERBOSE = true;

    private final String TAG = "SendMessageRunnable";

    private final MessageMethods mTask;

    public SendSnsMessageRunnable(MessageMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendMessageRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());

        HttpURLConnection conn = null;
        Bundle b = mTask.getDataBundle();
        if (VERBOSE) LogUtils.printBundle(b, TAG);

        b.putString(MessageEntry.COLUMN_RESPONSE_ARN,b.getString(Constants.MESSAGE_TARGET));

        int responseCode = -10;
        try {

            AWSMobileClient.defaultMobileClient().getPushManager().publishMessage(b);

        } catch (AmazonClientException e) {
            Log.e(TAG, "error handling JSON", e);
            mTask.handleMessageState(REQUEST_FAILED);
        } finally {
            mTask.handleMessageState(REQUEST_SUCCESS);

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
