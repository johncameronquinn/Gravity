package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.net.HttpURLConnection;
import java.util.UUID;


/**
 * Created by John C Quinn on 11/9/15.
 * Last modified: 11-17-15
 *
 * abstract class 'ServerTask'
 *
 * this class represents all the various forms of server tasks, that the threadpool can use to
 * connect to the server. these tasks will be treated mostly the same, for now.
 *
 * each derivative of this class must implement its own runnable to perform the request,
 * and also must implement its own URL to perform the request on.
 */
public abstract class ServerTask implements ServerConnectRunnable.ServerConnectMethods {


    private DataHandlingService mService;
    private Bundle dataBundle;
    private Thread mThread;
    private UUID userID;
    private HttpURLConnection mConnection;
    private int httpResponseCode;

    protected Runnable mServerConnectRunnable;

    public abstract Runnable getRequestRunnable();

    public abstract Runnable getServerConnectRunnable();

    public abstract String getURLPath();

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;
    }

    public void setServerConnection(HttpURLConnection connection) {
        mConnection = connection;
    }

    protected void handleDownloadState(int outstate,ServerTask task) {
        mService.handleDownloadState(outstate,task);
    }

    public HttpURLConnection getURLConnection() {
        return mConnection;
    }

    public Bundle getDataBundle() {
        return dataBundle;
    }

    public UUID getUserID() {
        return userID;
    }

    public void setUserID(UUID userID) {
        this.userID = userID;
    }

    public void setTaskThread(Thread thread) {
        this.mThread = thread;
    }

    public void insert(Uri uri, ContentValues values) {
        mService.insert(uri, values);
    }

    public Thread getTaskThread() {
        return this.mThread;
    }

    public DataHandlingService getService() {
        return mService;
    }

    public void recycle() {
        if (mConnection != null) {
            mConnection.disconnect();
            mConnection = null;
        }

        if (dataBundle!=null) {
            dataBundle.clear();
            dataBundle=null;
        }

    }

}
