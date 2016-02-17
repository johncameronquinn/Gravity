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
public class ReportContentRunnable implements Runnable{

    interface ReportContentMethods {

        void handleReportContentState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpsURLConnection getURLConnection();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final boolean VERBOSE = true;

    private final String TAG = "ReportContentRunnable";

    private final ReportContentMethods mTask;

    public ReportContentRunnable(ReportContentMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendLocalBlockRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());

        String contentID = mTask.getDataBundle().getString(Constants.KEY_CONTENT_ID);

        if (!"".equals(contentID)) {
            Log.d(TAG,"reporting content with ID : " + contentID);
        } else {
            mTask.handleReportContentState(REQUEST_FAILED);
            return;
        }

        HttpsURLConnection conn = mTask.getURLConnection();

        int responseCode = -10;

        boolean success = true;
        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeStringField(Constants.KEY_CONTENT_ID, contentID);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
            mTask.handleReportContentState(REQUEST_FAILED);
            success = false;

            try {
                responseCode = conn.getResponseCode();
            } catch (IOException ex) {
                Log.e(TAG,"error getting response code",ex);
            }

        } finally {
            if (success && responseCode == HttpURLConnection.HTTP_OK) {
                mTask.handleReportContentState(REQUEST_SUCCESS);
            } else {
                mTask.handleReportContentState(REQUEST_FAILED);
            }
        }

        mTask.setTaskThread(null);
        if (VERBOSE) {
            Log.v(TAG, "exiting SendLocalBlockRunnable...");
        }
    }
}
