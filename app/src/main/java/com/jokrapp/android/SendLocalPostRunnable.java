package com.jokrapp.android;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.jokrapp.android.util.LogUtils;
import com.jokrapp.android.SQLiteDbContract.LocalEntry;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by John C. Quinn on 11/14/15.
 *
 * class 'SendLocalBlockRunnable'
 *
 * sends a message to block the provided user to the server
 */
/***********************************************************************************************
 *
 *   IMAGE SENDING
 *
 */
/**
 * Handle action send images in the provided background thread with the provided
 * parameters.
 * <p/>
 * the client sends the latitude and longitude stored in the location passed in the arguments
 * and the image stored at the filepath provided
 * the sending process uses URLConnection to create the connection
 * the apache commons library and jackson's json parser are both utilized
 * <p/>
 * the server marks the time the image is received, and gives the image an ID
 *
 */
public class SendLocalPostRunnable implements Runnable{

    interface LocalPostMethods {

        void handleLocalPostState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpsURLConnection getURLConnection();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;


    private final boolean VERBOSE = true;

    private final String TAG = "SendLocalPostRunnable";

    private final LocalPostMethods mTask;

    public SendLocalPostRunnable(LocalPostMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendLocalPostRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());

        HttpsURLConnection conn = null;
        Bundle b = mTask.getDataBundle();
        LogUtils.printBundle(b, TAG);

        double lat = b.getDouble(LocalEntry.COLUMN_NAME_LATITUDE, 0.0);
        double lon = b.getDouble(LocalEntry.COLUMN_NAME_LONGITUDE, 0.0);
        String text = b.getString(LocalEntry.COLUMN_NAME_TEXT, "");
        String imageKey = b.getString(LocalEntry.COLUMN_NAME_FILEPATH, "");
        String arn = b.getString(LocalEntry.COLUMN_NAME_RESPONSE_ARN, "");


        int responseCode = -10;
        try {
            conn = mTask.getURLConnection();
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            jGen.writeStartObject();
            jGen.writeNumberField(LocalEntry.COLUMN_NAME_LATITUDE, lat);
            jGen.writeNumberField(LocalEntry.COLUMN_NAME_LONGITUDE, lon);
            jGen.writeStringField(LocalEntry.COLUMN_NAME_TEXT, text);
            jGen.writeStringField(LocalEntry.COLUMN_NAME_FILEPATH, imageKey);
            jGen.writeStringField(LocalEntry.COLUMN_NAME_RESPONSE_ARN, arn);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
            mTask.handleLocalPostState(REQUEST_FAILED);
        } finally {
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                mTask.handleLocalPostState(REQUEST_SUCCESS);
                File file = new File(imageKey);
                Log.i(TAG,"file is stored at" + imageKey);
                file.delete();
            } else {
                mTask.handleLocalPostState(REQUEST_FAILED);
                //todo retry request
            }

            if (conn != null) {
                conn.disconnect();
            }
        }


        mTask.setTaskThread(null);
        if (VERBOSE) {
            Log.v(TAG, "exiting SendLocalPostRunnable...");
        }
    }
}
