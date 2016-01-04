package us.gravwith.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;

import java.io.InputStream;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;


/**
 * Created by John C Quinn on 11/9/15.
 * Last modified: 11-17-15
 *
 * abstract class 'ServerTask'
 *
 * this class represents all the various forms of server tasks, that the threadpool can use to
 * connect to the server. These tasks will be treated mostly the same, for now.
 *
 * each derivative of this class must implement its own runnable to perform the request,
 * and also must implement its own URL to perform the request on.
 */
public abstract class ServerTask implements ServerConnectRunnable.ServerConnectMethods {

    /**
     * methods that all tasks under ServerTask must implement
     * these methods are defined in their respective runnables, and handled accordingly
     */
    interface ServerTaskMethods {

        /*
        * allows the ServerTask to maintain a reference to its current thread.
        * this is used to allow interrupts on this thread, if necessary
        */
        void setTaskThread(Thread thread);

        /*
         * allows access to the current connection held by the servertask, on which the
         * derivative classes will write from/to
         */
        HttpsURLConnection getURLConnection();

        /*
        * every task contains a dataBundle, passed by the client. This bundle contains
        * all necessary data to retry the transaction as necessary
        */
        Bundle getDataBundle();

        /* used by the runnables to set the HTTP response code */
        void setResponseCode(int ResponseCode);

        /* used by the service to access the response code */
        int getResponseCode();
    }


    private DataHandlingService mService;
    private Bundle dataBundle;
    private Thread mThread;
    private UUID userID;
    private HttpsURLConnection mConnection;
    private int responseWhat;
    private int responseCode;
    protected Runnable mServerConnectRunnable;

    public abstract Runnable getRequestRunnable();

    public abstract Runnable getServerConnectRunnable();

    public abstract String getURLPath();

    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID) {
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;
    }


    public void initializeTask(DataHandlingService mService, Bundle dataBundle, UUID userID,
                               int resp) {
        this.mService = mService;
        this.dataBundle = dataBundle;
        this.userID = userID;

        responseWhat = resp;
    }

    public void setServerConnection(HttpsURLConnection connection) {
        mConnection = connection;
    }

    protected void handleDownloadState(int outstate,ServerTask task) {
        mService.handleDownloadState(outstate, task);
    }

    protected void handleUploadState(int outstate,ServerTask task) {
        mService.handleUploadState(outstate, task);
    }

    public HttpsURLConnection getURLConnection() {
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

    public void delete(Uri uri, ContentValues values) { mService.delete(uri, values); }

    public Thread getTaskThread() {
        return this.mThread;
    }

    public int getResponseWhat() {
        return responseWhat;
    }

    public DataHandlingService getService() {
        return mService;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public InputStream getCAInput() {
        return mService.getResources().openRawResource(R.raw.ca);
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
