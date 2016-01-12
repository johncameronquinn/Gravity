package us.gravwith.android;

import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by John C. Quinn on 11/12/15.
 *
 * this class sends a blank object to the server.
 * this will be used by many objects in combination with a responserunnable
 */
public class SendBlankObjectRunnable implements Runnable {

    private final boolean VERBOSE = true;
    private final String TAG = SendBlankObjectRunnable.class.getSimpleName();
    private final BlankObjectMethods mService;


    static final int SEND_BLANK_FAILED = -1;
    static final int SEND_BLANK_STARTED = 0;
    static final int SEND_BLANK_SUCCESS = 1;


    interface BlankObjectMethods extends ServerTask.ServerTaskMethods {

        void handleSendBlankObjectState(int state);

    }

    public SendBlankObjectRunnable(BlankObjectMethods methods) {
        mService = methods;
    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "entering sendBlankObjectRunnable...");
        }

        mService.setTaskThread(Thread.currentThread());
        mService.handleSendBlankObjectState(SEND_BLANK_STARTED);

        HttpsURLConnection conn = null;
        boolean success = true;

        int responseCode = -1;

        try {
            if (Thread.interrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not sending...");
                return;
            }

            conn = mService.getURLConnection();

            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "IOException when retrieving replies...", e);
            success = false;

            /*get the response code if it hasn't already been gotten*/
            try {
                responseCode = conn.getResponseCode();
            } catch (IOException ex) {
                Log.e(TAG,"error getting response code",ex);
            }

        } finally {

            if (success) {
                Log.d(TAG, "No Exceptions, so... success :3");
                mService.handleSendBlankObjectState(SEND_BLANK_SUCCESS);
            } else {
                Log.d(TAG, "Something went wrong...");
                mService.setResponseCode(responseCode);
                mService.handleSendBlankObjectState(SEND_BLANK_FAILED);
            }
        }

        mService.setTaskThread(null);
        if (VERBOSE) Log.v(TAG, "exiting SendBlankObjectRunnable...");

    }
}
