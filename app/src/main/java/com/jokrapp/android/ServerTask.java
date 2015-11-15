package com.jokrapp.android;

import android.os.Bundle;
import java.util.UUID;


/**
 * Created by ev0x on 11/9/15.
 */
public abstract class ServerTask {


    private Thread mThread;

    public abstract Runnable getRequestRunnable();

    public abstract Runnable getServerConnectRunnable();

    public abstract void initializeTask(DataHandlingService mService, Bundle dataSendBundle, UUID userID);

    public void setTaskThread(Thread thread) {
        this.mThread = thread;
    }

    public Thread getTaskThread() {
        return this.mThread;
    }

}
