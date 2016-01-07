package us.gravwith.android;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import us.gravwith.android.ReportContentRunnable.ReportContentMethods;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class ReportContentTask extends ServerTask implements ReportContentMethods {

    private boolean VERBOSE = true;
    private final String TAG = "ReportContentTask";

    private Runnable mRequestRunnable;

    private final String urlString = "/moderation/report/";

    private Messenger replyMessenger;

    public String getURLPath() {
        return urlString;
    }

    public ReportContentTask(Messenger replyMessenger) {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mRequestRunnable = new ReportContentRunnable(this);
        this.replyMessenger = replyMessenger;
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

    public void handleReportContentState(int state) {
        Message msg;
        switch (state) {
            case ReportContentRunnable.REQUEST_FAILED:
                Log.d(TAG, "report content failed...");

                msg = Message.obtain(null, ReportManager.REPORT_STATUS_FAILED);
                msg.arg1 = getResponseCode();
                break;

            case ReportContentRunnable.REQUEST_STARTED:
                Log.d(TAG,"report content started...");

                msg = Message.obtain(null,ReportManager.REPORT_STATUS_STARTED);
                msg.arg1 = getResponseCode();
                break;

            case ReportContentRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "report content successs...");

                msg = Message.obtain(null,ReportManager.REPORT_STATUS_SUCCESS);
                msg.arg1 = getResponseCode();
                break;

            default:
                throw new RuntimeException("invalid request state");
        }

        try {
            replyMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG,"error reporting report status",e);
        }

    }


}
