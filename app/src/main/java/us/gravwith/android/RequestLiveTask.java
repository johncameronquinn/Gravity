package us.gravwith.android;

import android.util.Log;

import us.gravwith.android.RequestLiveThreadsRunnable.ThreadRequestMethods;
import us.gravwith.android.SendBlankObjectRunnable.BlankObjectMethods;
/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class RequestLiveTask extends ServerTask implements ThreadRequestMethods, BlankObjectMethods {

    private boolean VERBOSE = true;
    private final String TAG = "RequestLiveTask";

    private Runnable mRequestRunnable;
    private Runnable mResponseRunnable;

    private final String urlString = "/live/get/";

    public RequestLiveTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new SendBlankObjectRunnable(this);
        mResponseRunnable = new RequestLiveThreadsRunnable(this);
    }

    public Runnable getResponseRunnable() {
        return mResponseRunnable;
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRunnable() {
        return mRequestRunnable;
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

        handleDownloadState(outState, this);
    }

    @Override
    public void handleSendBlankObjectState(int state) {
        int outState = -1;

        switch (state) {
            case SendBlankObjectRunnable.SEND_BLANK_FAILED:
                Log.d(TAG, "send blank object failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendBlankObjectRunnable.SEND_BLANK_STARTED:
                Log.d(TAG,"send blank object started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendBlankObjectRunnable.SEND_BLANK_SUCCESS:
                Log.d(TAG, "send blank object success...");
                outState = DataHandlingService.REQUEST_COMPLETED;
                break;
        }
        handleDownloadState(outState,this);
    }

    public void handleThreadsRequestState(int state) {
        int outState = -1;

        switch (state) {
            case RequestLiveThreadsRunnable.REQUEST_THREADS_FAILED:
                Log.d(TAG, "request threads failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case RequestLiveThreadsRunnable.REQUEST_THREADS_STARTED:
                Log.d(TAG,"request threads started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case RequestLiveThreadsRunnable.REQUEST_THREADS_SUCCESS:
                Log.d(TAG, "request threads success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        handleDownloadState(outState,this);
    }



}
