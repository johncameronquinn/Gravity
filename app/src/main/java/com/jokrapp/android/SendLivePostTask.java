package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.jokrapp.android.SendLivePostRunnable.LivePostMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

import java.net.HttpURLConnection;
import java.util.UUID;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendLivePostTask extends ServerTask implements LivePostMethods, ServerConnectMethods{

    private Runnable mRequestRunnable;

    private boolean VERBOSE = true;
    private final String TAG = "SendLivePostTask";
    private final String urlString = "/live/upload/";

    public SendLivePostTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new SendLivePostRunnable(this);
    }

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering initializeSendLivePostTask...");
        this.mService = mService;
        this.userID = userID;
        this.dataBundle = dataBundle;
    }

    public void setServerConnection(HttpURLConnection connection) {
        mConnection = connection;
    }

    public HttpURLConnection getURLConnection() {
        return mConnection;
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRunnable() {
        return mRequestRunnable;
    }


    public Bundle getDataBundle() {
        return dataBundle;
    }

    public UUID getUserID() {
        return userID;
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

        mService.handleDownloadState(outState, this);
    }

    public void handleLivePostState(int state) {
        int outState = -1;

        switch (state) {
            case SendLivePostRunnable.REQUEST_FAILED:
                Log.d(TAG, "send live post failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendLivePostRunnable.REQUEST_STARTED:
                Log.d(TAG,"send live post started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendLivePostRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "send live post success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }

        mService.handleDownloadState(outState,this);
    }

}
