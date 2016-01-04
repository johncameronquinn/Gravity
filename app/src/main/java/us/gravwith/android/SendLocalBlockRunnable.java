package us.gravwith.android;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by John C. Quinn on 11/14/15.
 *
 * class 'SendLocalBlockRunnable'
 *
 * sends a message to block the provided user to the server
 */
public class SendLocalBlockRunnable implements Runnable{

    interface LocalBlockMethods {

        void handleLocalBlockState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpsURLConnection getURLConnection();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;


    private final boolean VERBOSE = true;

    private final String TAG = "SendLocalBlockRunnable";

    private final LocalBlockMethods mTask;

    public SendLocalBlockRunnable(LocalBlockMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendLocalBlockRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());

        String userToBlock = mTask.getDataBundle().getString(Constants.KEY_USER_ID,"");

        if (!"".equals(userToBlock)) {
            Log.d(TAG,"the user to block : " + userToBlock);
        } else {
            mTask.handleLocalBlockState(REQUEST_FAILED);
            return;
        }

        HttpsURLConnection conn;

        int responseCode = -10;
        try {
            conn = mTask.getURLConnection();
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeStringField("block", userToBlock);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
            mTask.handleLocalBlockState(REQUEST_FAILED);
        } finally {
            if (responseCode == HttpURLConnection.HTTP_OK) {
                mTask.handleLocalBlockState(REQUEST_SUCCESS);
            } else {
                mTask.handleLocalBlockState(REQUEST_FAILED);
            }
        }

        mTask.setTaskThread(null);
        if (VERBOSE) {
            Log.v(TAG, "exiting SendLocalBlockRunnable...");
        }
    }
}
