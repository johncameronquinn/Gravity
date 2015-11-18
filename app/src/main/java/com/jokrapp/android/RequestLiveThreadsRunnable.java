package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Created by ev0x on 11/12/15.
 */
public class RequestLiveThreadsRunnable implements Runnable {

    private final boolean VERBOSE = true;
    private final String TAG = "RequestThreadsRunnable";
    private final ThreadRequestMethods mService;


    static final int REQUEST_THREADS_FAILED = -1;
    static final int REQUEST_THREADS_STARTED = 0;
    static final int REQUEST_THREADS_SUCCESS = 1;


    interface ThreadRequestMethods {

        void handleThreadsRequestState(int state);

        void setTaskThread(Thread thread);

        HttpURLConnection getURLConnection();

        void insert(Uri uri, ContentValues values);
    }

    public RequestLiveThreadsRunnable(ThreadRequestMethods methods) {
        mService = methods;

    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveThreads...");
        }

        mService.setTaskThread(Thread.currentThread());
        mService.handleThreadsRequestState(REQUEST_THREADS_STARTED);

        HttpURLConnection conn = null;
        int responseCode = -10;

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

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not downloading.");
                return;
            }

            JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();

            ArrayList<LinkedHashMap<String, Object>> jsonArray =
                    objectMapper.readValue(jParser, ArrayList.class);

            List<ContentValues> valuesList = new ArrayList<>();

            if (!jsonArray.isEmpty()) {
                LinkedHashMap<String, Object> map;

                for (int i = 0; i < jsonArray.size(); i++) {
                    map = jsonArray.get(i);
                    map.put(SQLiteDbContract.LiveRepliesEntry.COLUMN_ID, map.remove("order"));
                    map.put(SQLiteDbContract.LiveRepliesEntry.COLUMN_NAME_THREAD_ID, map.remove("id"));

                    android.os.Parcel myParcel = android.os.Parcel.obtain();
                    myParcel.writeMap(map);
                    myParcel.setDataPosition(0);
                    android.content.ContentValues values = android.content.ContentValues.CREATOR.createFromParcel(myParcel);
                    valuesList.add(values);
                }

                responseCode = conn.getResponseCode();

                for (ContentValues row : valuesList) {
                     mService.insert(FireFlyContentProvider.CONTENT_URI_LIVE, row); //todo implement bulkinsert
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "IOException when retrieving replies...", e);
            mService.handleThreadsRequestState(REQUEST_THREADS_FAILED);
        } finally {
            if (responseCode == HttpURLConnection.HTTP_OK) {
                mService.handleThreadsRequestState(REQUEST_THREADS_SUCCESS);
            } else {
                mService.handleThreadsRequestState(REQUEST_THREADS_FAILED);
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        mService.setTaskThread(null);
        if (VERBOSE) Log.v(TAG, "exiting requestLiveThreads...");

    }
}
