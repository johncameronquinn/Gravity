package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokrapp.android.SQLiteDbContract.LocalEntry;
import com.jokrapp.android.util.LogUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by ev0x on 11/14/15.
 */
public class RequestMessagesRunnable implements Runnable {

    interface RequestMessagesMethods {

        void handleRequestMessagesState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpURLConnection getURLConnection();

        void insert(Uri uri, ContentValues values);
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final String TAG = "RequestLocalRunnable";

    private final boolean VERBOSE = true;

    RequestMessagesMethods mTask;

    public RequestMessagesRunnable(RequestMessagesMethods methods) {
        mTask = methods;
    }

    @Override
    public void run() {
        Log.d(TAG, "entering requestLocalMessages...");


        mTask.setTaskThread(Thread.currentThread());

        HttpURLConnection conn = null;
        Bundle b = mTask.getDataBundle();
        LogUtils.printBundle(b, TAG);

        int responseCode = -10;

        try {
            if (Thread.interrupted()) {
                return;
            }

            mTask.handleRequestMessagesState(REQUEST_STARTED);

            conn = mTask.getURLConnection();

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not sending...");
                return;
            }

            if (VERBOSE) Log.v(TAG,"opening request...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not receiving...");
                return;
            }

            if (VERBOSE) Log.v(TAG, "opening inputStream to receive JSON..");
            JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();

            ArrayList<LinkedHashMap<String, Object>> jsonArray =
                    objectMapper.readValue(jParser, ArrayList.class);

            List<ContentValues> valuesList = new ArrayList<>();

            if (!jsonArray.isEmpty()) {
                LinkedHashMap<String, Object> map;

                for (int i = 0; i < jsonArray.size(); i++) {
                    map = jsonArray.get(i);
                    map.put(SQLiteDbContract.LiveReplies.COLUMN_ID, map.remove("id"));
                    b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_MESSAGE_DIRECTORY);
                    android.os.Parcel myParcel = android.os.Parcel.obtain();
                    myParcel.writeMap(map);
                    myParcel.setDataPosition(0);
                    ContentValues values = ContentValues.CREATOR.createFromParcel(myParcel);
                    valuesList.add(values);
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not saving...");
                return;
            }

            if (VERBOSE) Log.v(TAG, "now saving JSON...");
            for (ContentValues row : valuesList) {
                mTask.insert(FireFlyContentProvider.CONTENT_URI_MESSAGE,row); //todo implement bulkinsert
            }

            responseCode = conn.getResponseCode();

        } catch (IOException e) {
            Log.d(TAG, "response code " + responseCode);
            Log.e(TAG, "error requesting images", e);
        } finally {
            if (responseCode == HttpURLConnection.HTTP_OK) {
                mTask.handleRequestMessagesState(REQUEST_SUCCESS);
            } else {
                mTask.handleRequestMessagesState(REQUEST_FAILED);
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        mTask.setTaskThread(null);
        Thread.interrupted();

        Log.d(TAG, "exiting requestLocalMessages...");
    }
}
