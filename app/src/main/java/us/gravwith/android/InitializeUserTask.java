package us.gravwith.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import us.gravwith.android.user.InitializeUserRunnable;
import us.gravwith.android.user.InitializeUserRunnable.InitializeUserMethods;
import us.gravwith.android.user.LoginManager;

import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class InitializeUserTask extends ServerTask implements InitializeUserMethods, ServerConnectRunnable.ServerConnectMethods {


    Bundle dataBundle;

    private DataHandlingService mService;

    private boolean VERBOSE = true;
    private final String TAG = "RequestRepliesTask";

    private HttpsURLConnection mConnection;

    private Runnable mServerConnectRunnable;
    private Runnable mRequestRunnable;
    private UUID userID;
    private final String urlString = "/security/create/";

    public InitializeUserTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new InitializeUserRunnable(this);
    }

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering initializeLocalTask...");
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;
    }

    public void setServerConnection(HttpsURLConnection connection) {
        mConnection = connection;
    }

    public HttpsURLConnection getURLConnection() {
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
        mService.handleDownloadState(outState, this);
    }

    public void handleLoginState(int state) {
        LoginManager.handleLoginState(state);
    }

    public void handleInitializeState(int state) {
        int outState = LoginManager.INVALID_STATE_RETURNED;

        switch (state) {
            case RequestLocalRunnable.REQUEST_FAILED:
                Log.d(TAG, "initialize user failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case RequestLocalRunnable.REQUEST_STARTED:
                Log.d(TAG,"initialize user started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case RequestLocalRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "initialize user success...");
                outState = DataHandlingService.INITIALIZE_TASK_COMPLETED;
                break;
        }
        mService.handleDownloadState(outState,this);
    }

    public void setUserID(UUID userID) {
        this.userID = userID;
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
