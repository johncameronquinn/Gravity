package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.net.HttpURLConnection;
import java.util.UUID;
import com.jokrapp.android.RequestRepliesRunnable.ReplyRequestMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class RequestRepliesTask extends ServerTask implements ReplyRequestMethods, ServerConnectMethods{


    Bundle dataBundle;

    private DataHandlingService mService;

    private boolean VERBOSE = true;
    private final String TAG = "RequestRepliesTask";

    private HttpURLConnection mConnection;

    private Runnable mServerConnectRunnable;
    private Runnable mRequestRepliesRunnable;
    private Runnable mSaveIncomingRunnable;

    private Thread mThread;
    private UUID userID;
    private final String urlString = "/reply/get/";

    public RequestRepliesTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRepliesRunnable = new RequestRepliesRunnable(this);
    }

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering initializeRepliesTask...");
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;
    }

    public void setRequestRepliesThread(Thread thread) {
        mThread = thread;
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
        return mRequestRepliesRunnable;
    }

    public void insert(Uri uri, ContentValues values) {
        mService.insert(uri,values);
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