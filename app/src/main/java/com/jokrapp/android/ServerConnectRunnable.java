package com.jokrapp.android;

import android.os.*;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.util.UUID;

/**
 * class 'ServerConnectRunnable'
 *
 * @author John C. Quinn
 * created on 11/14/15
 * last modified : 11/18/15
 *
 * A single runnable task used to establish a connection to the server.
 * Utilitzes {@link ServerTask} to manage necessary data.
 */
public class ServerConnectRunnable implements Runnable {

    interface ServerConnectMethods {

        void handleServerConnectState(int state);

        Bundle getDataBundle();

        UUID getUserID();

        String getURLPath();

        void setTaskThread(Thread connectThread);

        void setServerConnection(HttpURLConnection serverConnection);
    }

    private final ServerConnectMethods mTask;

    private final boolean VERBOSE = true;

    private final String TAG = "ServerConnectRunnable";

    private final int NUMBER_OF_CONNECT_TRIES = 5;

    // Constants for indicating the state of the decode
    static final int CONNECT_STATE_FAILED = -1;
    static final int CONNECT_STATE_STARTED = 0;
    static final int CONNECT_STATE_COMPLETED = 1;


    private final int SERVER_SOCKET = 80; //does not change
    private final String CONNECTION_PROTOCOL = "http";
    private final int READ_TIMEOUT = 10000;
    private final int CONNECT_TIMEOUT = 20000;

    private final int BASE_RE_ATTEMPT_DELAY = 1000;

    public ServerConnectRunnable (ServerConnectMethods methods){
        mTask = methods;
    }

    @Override
    public void run() {

        mTask.setTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        HttpURLConnection conn = null;

        int responseCode = -1;
            try {
                mTask.handleServerConnectState(CONNECT_STATE_STARTED);

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (VERBOSE) Log.v(TAG, "creating to " + mTask.getURLPath());
                URL url;

                try {
                    url  = new URL(
                            CONNECTION_PROTOCOL,
                            Constants.SERVER_URL,
                            SERVER_SOCKET,
                            mTask.getURLPath()
                    );

                } catch (MalformedURLException e) {
                    Log.e(TAG, "MalformedURL provided...", e);
                    mTask.handleServerConnectState(CONNECT_STATE_FAILED);
                    return;
                }

                int attempt = 0;

                /*
                 * Attempts to create and set the connection to the server
                 * continues to retry with a delay of 1 * 2^n seconds, where n is the number
                 * of previous attempts. Times out and reports after 5 attempts.
                 */
                do {
                    Log.i(TAG,"connecting to server...");

                    try {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(READ_TIMEOUT);
                        conn.setConnectTimeout(CONNECT_TIMEOUT);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setInstanceFollowRedirects(false);

                        UUID userID = mTask.getUserID();
                        if (userID == null) {
                            conn.setRequestProperty("X-Client-UserID", "");
                        } else {
                            conn.setRequestProperty("X-Client-UserID", userID.toString());
                        }

                        conn.setUseCaches(false);

                        // < 100 is undetermined.
                        // 1nn is informal (shouldn't happen on a GET/HEAD)
                        // 2nn is success
                        // 3nn is redirect
                        // 4nn is client error
                        // 5nn is server error

                    } catch (ConnectException e) {
                        Log.e(TAG, "Failed to open a connection to the server...", e);
                        conn = null;
                    } catch (ProtocolException e) {
                        Log.e(TAG, "ProtocolException when trying to open a connection to the " +
                                "server...", e);
                        conn = null;
                    } catch (SocketException e) {
                        Log.e(TAG, "SocketException...", e);
                        conn = null;
                    } catch (IOException e) {
                        Log.e(TAG,
                                "IOException when trying to open a connection to the server...",
                                e);
                        conn = null;
                    } finally {
                        if (attempt > 0) {
                            Double delay = BASE_RE_ATTEMPT_DELAY * Math.pow(2, attempt);
                            if (VERBOSE) Log.v(TAG,"thread sleeping for " + delay + " seconds...");
                            Thread.sleep(delay.longValue());
                        }
                        attempt++;
                    }
                } while (conn==null && attempt < NUMBER_OF_CONNECT_TRIES);

            } catch (InterruptedException e) {
                Log.e(TAG,"interruptedException occured...",e);
                conn = null;
            } finally {
                if (conn != null) {
                    mTask.setServerConnection(conn);
                    mTask.handleServerConnectState(CONNECT_STATE_COMPLETED);
                } else {
                    mTask.handleServerConnectState(CONNECT_STATE_FAILED);
                }

                Thread.interrupted();
            }

        mTask.setTaskThread(null);
    }

}
