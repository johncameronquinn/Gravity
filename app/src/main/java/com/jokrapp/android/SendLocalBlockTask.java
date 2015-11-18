package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.jokrapp.android.SendLocalBlockRunnable.LocalBlockMethods;
import com.jokrapp.android.ServerConnectRunnable.ServerConnectMethods;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendLocalBlockTask extends ServerTask implements LocalBlockMethods {

    private boolean VERBOSE = true;
    private final String TAG = "SendLocalBlockTask";

    private Runnable mRequestRunnable;

    private final String urlString = "/moderation/block/";

    public String getURLPath() {
        return urlString;
    }

    public SendLocalBlockTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new SendLocalBlockRunnable(this);
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRunnable() {
        return mRequestRunnable;
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

        handleDownloadState(outState, this);
    }

    public void handleLocalBlockState(int state) {
        int outState = -1;

        switch (state) {
            case SendLocalBlockRunnable.REQUEST_FAILED:
                Log.d(TAG, "send block failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendLocalBlockRunnable.REQUEST_STARTED:
                Log.d(TAG,"send block started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendLocalBlockRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "send block success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        handleDownloadState(outState,this);
    }


}
