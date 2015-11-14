package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokrapp.android.RequestRepliesRunnable.ReplyRequestMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class RequestRepliesTask implements ReplyRequestMethods, ServerConnectMethods{


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

    private ContentValues[] valuesArray;

    public RequestRepliesTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRepliesRunnable = new RequestRepliesRunnable(this);
    }

    public void initializeRepliesTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering initializeRepliesTask...");
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;
    }

    public String getRequestRepliesPath() {
        return mService.GET_LIVE_THREAD_REPLIES;
    }

    public void setRequestRepliesThread(Thread thread) {
        mThread = thread;
    }

    public void setServerConnectThread(Thread thread) {
        mThread = thread;
    }

    public void setServerConnection(HttpURLConnection connection) {
        mConnection = connection;
    }

    public HttpURLConnection getURLConnection() {
        return mConnection;
    }

    public Thread getRequestRepliesThread() {
        return mThread;
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRepliesRunnable() {
        return mRequestRepliesRunnable;
    }

    public void insert(Uri uri, ContentValues values) {
        mService.insert(uri,values);
    }


    public void handleServerConnectState(int state) {
        switch (state) {
            case ServerConnectRunnable.CONNECT_STATE_FAILED:
                Log.d(TAG, "server connection failed...");
                break;

            case ServerConnectRunnable.CONNECT_STATE_STARTED:
                Log.d(TAG,"server connection started...");
                break;

            case ServerConnectRunnable.CONNECT_STATE_COMPLETED:
                Log.d(TAG, "successfully connected to server :3");
                mThread = new Thread(getRequestRepliesRunnable());
                mThread.start();
                break;
        }

    }

    public void handleRepliesRequestState(int state) {
        switch (state) {
            case RequestRepliesRunnable.REQUEST_REPLIES_FAILED:
                Log.d(TAG, "request replies failed...");
                break;

            case RequestRepliesRunnable.REQUEST_REPLIES_STARTED:
                Log.d(TAG,"request replies started...");
                break;

            case RequestRepliesRunnable.REQUEST_REPLIES_SUCCESS:
                Log.d(TAG,"request replies success...");
                break;
        }
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
