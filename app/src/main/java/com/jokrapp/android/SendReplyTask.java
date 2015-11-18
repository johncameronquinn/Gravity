package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.net.HttpURLConnection;
import java.util.UUID;
import com.jokrapp.android.SendReplyRunnable.SendReplyMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendReplyTask extends ServerTask implements SendReplyMethods, ServerConnectMethods{

    Bundle dataBundle;

    private DataHandlingService mService;

    private boolean VERBOSE = true;
    private final String TAG = "SendLivePostTask";

    private HttpURLConnection mConnection;

    private Runnable mServerConnectRunnable;
    private Runnable mRequestRunnable;

    private UUID userID;
    private final String urlString = "/reply/upload/";

    public SendReplyTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new SendReplyRunnable(this);
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

    public void insert(Uri uri, ContentValues values) {
        mService.insert(uri, values);
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
                Log.d(TAG,"server connection started...");
                outState = DataHandlingService.CONNECTION_STARTED;
                break;

            case ServerConnectRunnable.CONNECT_STATE_COMPLETED:
                Log.d(TAG, "successfully connected to server :3");
                outState = DataHandlingService.CONNECTION_COMPLETED;
                break;

        }

        mService.handleDownloadState(outState, this);
    }

    public void handleLivePostState(int state) {
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

        mService.handleDownloadState(outState,this);
    }

}
