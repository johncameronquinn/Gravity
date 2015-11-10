package com.jokrapp.android;

import android.os.Bundle;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
public class RequestRepliesTask extends ServerTask {

    public RequestRepliesTask(DataHandlingService mService, Bundle dataBundle) {
        super(mService,dataBundle);
    }

    public String getRequestRepliesPath() {
        return mService.GET_LIVE_THREAD_REPLIES;
    }

    public void setRequestRepliesThread(Thread thread) {

    }

    public void handleReplyState(int state) {

    }
}
