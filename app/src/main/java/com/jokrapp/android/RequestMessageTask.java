package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.net.HttpURLConnection;
import java.util.UUID;

import com.jokrapp.android.RequestMessagesRunnable.RequestMessagesMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class RequestMessageTask extends ServerTask implements RequestMessagesMethods, ServerConnectMethods{

    Bundle dataBundle;

    private DataHandlingService mService;

    private boolean VERBOSE = true;
    private final String TAG = "RequestRepliesTask";

    private HttpURLConnection mConnection;

    private Runnable mServerConnectRunnable;
    private Runnable mRequestRunnable;
    private Runnable mSaveIncomingRunnable;
    private UUID userID;
    private final String urlString = "/message/get/";

    public RequestMessageTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new RequestMessagesRunnable(this);
    }

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering initializeLocalTask...");
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;
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

        mService.handleDownloadState(outState,this);
    }

    public void handleRequestMessagesState(int state) {
        int outState = -1;

        switch (state) {
            case RequestMessagesRunnable.REQUEST_FAILED:
                Log.d(TAG, "request messages failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case RequestMessagesRunnable.REQUEST_STARTED:
                Log.d(TAG,"request messages started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case RequestMessagesRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "request messages started...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        mService.handleDownloadState(outState,this);
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

}
