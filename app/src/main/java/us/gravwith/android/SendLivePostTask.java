package us.gravwith.android;

import android.util.Log;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendLivePostTask extends ServerTask implements SendLivePostRunnable.LivePostMethods,
    RequestLiveThreadsRunnable.ThreadRequestMethods {

    private Runnable mRequestRunnable;
    private Runnable mResponseRunnable;

    private boolean VERBOSE = true;
    private final String TAG = "SendLivePostTask";
    private final String urlString = "/live/upload/";

    public SendLivePostTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new SendLivePostRunnable(this);
        mResponseRunnable = new RequestLiveThreadsRunnable(this);
    }

    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    public Runnable getRequestRunnable() {
        return mRequestRunnable;
    }

    public Runnable getResponseRunnable() {
        return mResponseRunnable;
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

    public void handleLivePostState(int state) {
        int outState = -1;

        switch (state) {
            case SendLivePostRunnable.REQUEST_FAILED:
                Log.d(TAG, "send live post failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendLivePostRunnable.REQUEST_STARTED:
                Log.d(TAG, "send live post started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendLivePostRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "send live post success...");
                outState = DataHandlingService.REQUEST_COMPLETED;
                break;
        }

        handleDownloadState(outState, this);
    }

    public void handleThreadsRequestState(int state) {
        int outState = -1;

        switch (state) {
            case RequestLocalRunnable.REQUEST_FAILED:
                Log.d(TAG, "request threads failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case RequestLocalRunnable.REQUEST_STARTED:
                Log.d(TAG,"request threads started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case RequestLocalRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "request threads success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        handleDownloadState(outState,this);
    }

}
