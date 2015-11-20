package com.jokrapp.android;

import android.util.Log;

import com.jokrapp.android.RequestRepliesRunnable.ReplyRequestMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class RequestRepliesTask extends ServerTask implements ReplyRequestMethods {

    private boolean VERBOSE = true;
    private final String TAG = "RequestRepliesTask";


    private Runnable mServerConnectRunnable;
    private Runnable mRequestRepliesRunnable;
    private Runnable mSaveIncomingRunnable;

    private final String urlString = "/reply/get/";

    public RequestRepliesTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRepliesRunnable = new RequestRepliesRunnable(this);
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRunnable() {
        return mRequestRepliesRunnable;
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

        handleDownloadState(outState,this);
    }

    public void handleRepliesRequestState(int state) {
        int outState = -1;

        switch (state) {
            case RequestRepliesRunnable.REQUEST_REPLIES_FAILED:
                Log.d(TAG, "request replies failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case RequestRepliesRunnable.REQUEST_REPLIES_STARTED:
                Log.d(TAG,"request replies started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case RequestRepliesRunnable.REQUEST_REPLIES_SUCCESS:
                Log.d(TAG,"request replies success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        handleDownloadState(outState,this);
    }


}
