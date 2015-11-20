package com.jokrapp.android;

import android.util.Log;

import com.jokrapp.android.SendReplyRunnable.SendReplyMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendReplyTask extends ServerTask implements SendReplyMethods {

    private boolean VERBOSE = true;
    private final String TAG = "SendLivePostTask";
    private Runnable mRequestRunnable;
    private final String urlString = "/reply/upload/";

    public SendReplyTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new SendReplyRunnable(this);
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
                Log.d(TAG,"server connection started...");
                outState = DataHandlingService.CONNECTION_STARTED;
                break;

            case ServerConnectRunnable.CONNECT_STATE_COMPLETED:
                Log.d(TAG, "successfully connected to server :3");
                outState = DataHandlingService.CONNECTION_COMPLETED;
                break;

        }

        handleDownloadState(outState, this);
    }

    public void handleSendReplyState(int state) {
        int outState = -10;

        switch (state) {
            case SendReplyRunnable.REQUEST_FAILED:
                Log.d(TAG, "send reply failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendReplyRunnable.REQUEST_STARTED:
                Log.d(TAG,"send reply started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendReplyRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "send reply success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }

        handleDownloadState(outState,this);
    }

}
