package com.jokrapp.android;

import android.util.Log;

import com.jokrapp.android.SendLocalPostRunnable.LocalPostMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendLocalPostTask extends ServerTask implements LocalPostMethods {

    private final boolean VERBOSE = true;
    private final String TAG = "SendLocalPostTask";
    private final String urlString = "/local/upload/";

    private Runnable mRequestRunnable;

    public SendLocalPostTask() {
        if (mServerConnectRunnable == null) {
            mServerConnectRunnable = new ServerConnectRunnable(this);
        } else {
            Log.d(TAG, "mServerConnectRunnable is being reused...");
        }
        mRequestRunnable = new SendLocalPostRunnable(this);
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRunnable() {
        return mRequestRunnable;
    }

    public String getURLPath() {
        return urlString;
    }


    public void handleServerConnectState(int state) {
        int outState = -10;

        switch (state) {
            case ServerConnectRunnable.CONNECT_STATE_FAILED:
                Log.d(TAG, "server connection failed...");
                outState = DataHandlingService.CONNECTION_FAILED;
                break;

            case ServerConnectRunnable.CONNECT_STATE_STARTED:
                outState = DataHandlingService.CONNECTION_STARTED;
                Log.d(TAG,"server connection started...");
                break;

            case ServerConnectRunnable.CONNECT_STATE_COMPLETED:
                outState = DataHandlingService.CONNECTION_COMPLETED;
                Log.d(TAG, "successfully connected to server :3");
                break;
        }
        handleUploadState(outState, this);
    }

    public void handleLocalPostState(int state) {
        int outState = -1;

        switch (state) {
            case SendLocalPostRunnable.REQUEST_FAILED:
                Log.d(TAG, "send local post failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendLocalPostRunnable.REQUEST_STARTED:
                Log.d(TAG,"send local post started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendLocalPostRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "send local post success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        handleUploadState(outState, this);
    }

}
