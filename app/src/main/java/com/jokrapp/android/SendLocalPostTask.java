package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.jokrapp.android.SendLocalPostRunnable.LocalPostMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

import java.net.HttpURLConnection;
import java.util.UUID;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendLocalPostTask extends ServerTask implements LocalPostMethods, ServerConnectMethods{

    private final boolean VERBOSE = true;
    private final String TAG = "SendLocalPostTask";
    private final String urlString = "/local/upload/";

    private Runnable mRequestRunnable;

    public SendLocalPostTask() {

        if(mServerConnectRunnable == null) {
            mServerConnectRunnable = new ServerConnectRunnable(this);
        } else {
            Log.d(TAG,"mServerConnectRunnable is being reused...");
        }

        mRequestRunnable = new SendLocalPostRunnable(this);
    }

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering initializeLocalPostTask...");
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
        mService.handleDownloadState(outState,this);
    }

}
