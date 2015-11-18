package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import java.net.HttpURLConnection;
import java.util.UUID;


/**
 * Created by ev0x on 11/9/15.
 */
public abstract class ServerTask {

    protected Runnable mServerConnectRunnable;
    protected DataHandlingService mService;
    protected UUID userID;

    protected HttpURLConnection mConnection;
    protected Bundle dataBundle;

    private Thread mThread;

    public abstract Runnable getRequestRunnable();

    public abstract Runnable getServerConnectRunnable();

    public abstract void initializeTask(DataHandlingService mService, Bundle dataSendBundle, UUID userID);

    public void setTaskThread(Thread thread) {
        this.mThread = thread;
    }

    protected void insert(Uri uri, ContentValues values) {
        mService.insert(uri, values);
    }

    public Thread getTaskThread() {
        return this.mThread;
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
