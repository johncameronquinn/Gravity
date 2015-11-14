package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokrapp.android.util.LogUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by ev0x on 11/9/15.
 */
class RequestRepliesRunnable implements Runnable {

    private boolean VERBOSE = true;

    private String TAG = "RequestRepliesRunnable";

    private final ReplyRequestMethods mService;

    static final int REQUEST_REPLIES_FAILED = -1;
    static final int REQUEST_REPLIES_STARTED = 0;
    static final int REQUEST_REPLIES_SUCCESS = 1;

    interface ReplyRequestMethods {

        void handleRepliesRequestState(int state);


        void setRequestRepliesThread(Thread thread);

        String getRequestRepliesPath();

        Bundle getDataBundle();

        HttpURLConnection getURLConnection();

        void insert(Uri uri, ContentValues values);
    }


    public RequestRepliesRunnable(ReplyRequestMethods methods) {
        mService = methods;
    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveReplies...");
        }

        mService.setRequestRepliesThread(Thread.currentThread());

        HttpURLConnection conn = null;
        Bundle b = mService.getDataBundle();
        LogUtils.printBundle(b,TAG);

        int responsecode = -1;

        try {
            if (Thread.interrupted()) {
                return;
            }

            mService.handleRepliesRequestState(REQUEST_REPLIES_STARTED);

            conn = mService.getURLConnection();

            if (Thread.interrupted()) {
                return;
            }

            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "thread id: " +
                        b.getInt("threadID"));
            }

            jGen.writeStartObject();
            jGen.writeNumberField(DataHandlingService.THREAD_ID, b.getInt("threadID"));
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not storing...");
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
                    map.put(SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID, b.getInt("threadID"));
                    android.os.Parcel myParcel = android.os.Parcel.obtain();
                    myParcel.writeMap(map);
                    myParcel.setDataPosition(0);
                    android.content.ContentValues values = android.content.ContentValues.CREATOR.createFromParcel(myParcel);
                    valuesList.add(values);
                }
            }


            if (VERBOSE) Log.v(TAG, "now saving JSON...");
            for (ContentValues row : valuesList) {
                mService.insert(FireFlyContentProvider.CONTENT_URI_REPLY_LIST,row); //todo implement bulkinsert
            }

            responsecode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "IOException when retrieving replies...", e);
            mService.handleRepliesRequestState(REQUEST_REPLIES_FAILED);
        } finally {
            if (responsecode == HttpURLConnection.HTTP_OK) {
                mService.handleRepliesRequestState(REQUEST_REPLIES_SUCCESS);
            } else {
                mService.handleRepliesRequestState(REQUEST_REPLIES_FAILED);
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        mService.setRequestRepliesThread(null);
        Thread.interrupted();
        if (VERBOSE) Log.v(TAG, "exiting requestLiveReplies...");
    }
}
