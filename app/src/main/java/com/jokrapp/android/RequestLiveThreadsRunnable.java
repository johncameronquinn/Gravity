package com.jokrapp.android;

import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.UUID;

/**
 * Created by ev0x on 11/12/15.
 */
public class RequestLiveThreadsRunnable implements Runnable {

    private final boolean VERBOSE = true;
    private final String TAG = "RequestThreadsRunnable";
    private final ThreadRequestMethods mService;


    static final int REQUEST_THREADS_FAILED = -1;
    static final int REQUEST_THREADS_STARTED = 0;
    static final int REQUEST_THREADS_SUCCESS = 1;


    interface ThreadRequestMethods {


        HttpURLConnection connectToServer(String ServerPath) throws ConnectException;

        void handleThreadsRequestState(int state);


        void setRequestRepliesThread(Thread thread);

        int saveIncomingJsonArray(int where, URLConnection conn, Bundle extradata)
                throws IOException;


        String getRequestThreadsPath();

        boolean handleResponseCode(int code);
    }

    public RequestLiveThreadsRunnable(ThreadRequestMethods methods) {
        mService = methods;

    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveThreads...");
        }

        mService.handleThreadsRequestState(REQUEST_THREADS_STARTED);

        HttpURLConnection conn;
        try {
            conn = mService.connectToServer(mService.getRequestThreadsPath());
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            mService.handleThreadsRequestState(REQUEST_THREADS_FAILED);
            return;
        }

        if (VERBOSE) Log.v(TAG,"now emptying live table prior to insertion...");

        int rows = -1;
        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            rows = mService.saveIncomingJsonArray(
                    DataHandlingService.MSG_REQUEST_LIVE_THREADS,
                    conn,
                    null
            );

            mService.handleResponseCode(conn.getResponseCode());
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        } finally {
            if (rows >= 0) {
                mService.handleThreadsRequestState(REQUEST_THREADS_SUCCESS);
            } else {
                mService.handleThreadsRequestState(REQUEST_THREADS_FAILED);
            }
        }

        if (VERBOSE) Log.v(TAG, "exiting requestLiveThreads...");
    }
}
