package com.jokrapp.android;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jokrapp.android.util.LogUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Created by ev0x on 11/9/15.
 */
class RequestRepliesRunnable implements Runnable {

    private boolean VERBOSE = true;

    private String TAG = "RequestRepliesRunnable";

    private final ReplyRequestMethods mService;

    static final int REQUEST_REPLIES_FAILED = -1;
    static final int REQUEST_REPLIES_STARTED = 0;
    static final int REQUEST_REPLIES_SUCCESS = 1;

    interface ReplyRequestMethods {


        HttpURLConnection connectToServer(String ServerPath) throws ConnectException;

        void handleRepliesRequestState(int state);


        void setRequestRepliesThread(Thread thread);

        int saveIncomingJsonArray(int where, URLConnection conn, Bundle extradata)
                throws IOException;


        String getRequestRepliesPath();

        Bundle getDataBundle();

        boolean handleResponseCode(int code);
    }


    public RequestRepliesRunnable(ReplyRequestMethods methods) {
        mService = methods;
    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveReplies...");
        }

        mService.setRequestRepliesThread(Thread.currentThread());


        Bundle b = mService.getDataBundle();
        LogUtils.printBundle(b,TAG);
        int rows = -1;


        try {
            if (Thread.interrupted()) {
                return;
            }

            mService.handleRepliesRequestState(REQUEST_REPLIES_STARTED);

            HttpURLConnection conn;
            try {
                conn = mService.connectToServer(mService.getRequestRepliesPath());
            } catch (ConnectException e) {
                Log.e(TAG, "failed to connect to the server... quitting...");
                mService.handleRepliesRequestState(REQUEST_REPLIES_FAILED);
                return;
            }

            if (Thread.interrupted()) {
                return;
            }

            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "thread id: " +
                        b.getInt("threadID"));
            }

            jGen.writeStartObject();
            jGen.writeNumberField(DataHandlingService.THREAD_ID, b.getInt("threadID"));
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();


            /**pass thread id**/
            rows = mService.saveIncomingJsonArray(DataHandlingService.MSG_REQUEST_REPLIES, conn, b);
            if (VERBOSE) Log.v(TAG,"number of replies returned : " + rows);

            mService.handleResponseCode(conn.getResponseCode());
        } catch (IOException e) {
            Log.e(TAG, "IOException when retrieving replies...", e);
            mService.handleRepliesRequestState(REQUEST_REPLIES_FAILED);
        } finally {
            if (rows > -1) {
                Log.d(TAG, "success retrieving replies...");
                mService.handleRepliesRequestState(REQUEST_REPLIES_SUCCESS);
            } else {
                Log.d(TAG, "failure retrieving replies...");
                mService.handleRepliesRequestState(REQUEST_REPLIES_FAILED);
            }
        }

        mService.setRequestRepliesThread(null);
        Thread.interrupted();
        if (VERBOSE) Log.v(TAG, "exiting requestLiveReplies...");
    }
}
