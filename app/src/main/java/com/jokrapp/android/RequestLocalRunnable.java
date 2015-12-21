package com.jokrapp.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokrapp.android.SQLiteDbContract.LocalEntry;
import com.jokrapp.android.util.LogUtils;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by ev0x on 11/14/15.
 */
public class RequestLocalRunnable implements Runnable {

    interface RequestLocalMethods {

        void handleLocalRequestState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpsURLConnection getURLConnection();

        List<String> getImagesSeen();

        void insert(Uri uri, ContentValues values);
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final String TAG = "RequestLocalRunnable";

    private final boolean VERBOSE = true;

    RequestLocalMethods mTask;

    public RequestLocalRunnable(RequestLocalMethods methods) {
        mTask = methods;
    }

    @Override
    public void run() {
        Log.d(TAG, "entering requestLocalPosts...");


        mTask.setTaskThread(Thread.currentThread());

        HttpsURLConnection conn = null;
        Bundle b = mTask.getDataBundle();
        if (VERBOSE) LogUtils.printBundle(b, TAG);

        int responseCode = -1;


        double lat = b.getDouble(LocalEntry.COLUMN_NAME_LATITUDE, 0.0);
        double lon = b.getDouble(LocalEntry.COLUMN_NAME_LONGITUDE, 0.0);
        int numberOfImages = b.getInt(Constants.IMAGE_COUNT);

        Log.d(TAG, "requesting " + numberOfImages + " images, with lat " + lat + " and lng " + lon);


        try {
            if (Thread.interrupted()) {
                return;
            }

            mTask.handleLocalRequestState(REQUEST_STARTED);

            conn = mTask.getURLConnection();

            if (Thread.interrupted()) {
                return;
            }

            if (VERBOSE) Log.v(TAG,"sending local data...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeNumberField("latitude", lat);
            jGen.writeNumberField("longitude", lon);
            jGen.writeNumberField("count", numberOfImages);
            jGen.writeFieldName("seen");
            jGen.writeStartArray(mTask.getImagesSeen().size());
            for (String i : mTask.getImagesSeen()) {
                jGen.writeNumber(i);
            }
            jGen.writeEndArray();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close(); //closes the conn.getOutputStream as well

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

                    if (VERBOSE) LogUtils.printMapToVerbose(map,TAG);

                    //swap id for _ID, to allow listview loading, and add the thread ID
                    Object id = map.remove("id");
                    mTask.getImagesSeen().add(String.valueOf(id));
                    map.put(SQLiteDbContract.LiveReplies.COLUMN_ID, id);
                    b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LOCAL_DIRECTORY);
                    android.os.Parcel myParcel = android.os.Parcel.obtain();
                    myParcel.writeMap(map);
                    myParcel.setDataPosition(0);
                    android.content.ContentValues values = android.content.ContentValues.CREATOR.createFromParcel(myParcel);
                    valuesList.add(values);
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not saving...");
                return;
            }

            if (VERBOSE) Log.v(TAG, "now saving JSON...");
            for (ContentValues row : valuesList) {
                mTask.insert(FireFlyContentProvider.CONTENT_URI_LOCAL,row); //todo implement bulkinsert
            }

            responseCode = conn.getResponseCode();


        } catch (IOException e) {
            Log.d(TAG, "IOException");
            Log.d(TAG, "response code " + responseCode);
            Log.e(TAG, "error requesting images", e);
        } finally {
            if (responseCode == HttpURLConnection.HTTP_OK) {
                mTask.handleLocalRequestState(REQUEST_SUCCESS);
            } else {
                mTask.handleLocalRequestState(REQUEST_FAILED);
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        mTask.setTaskThread(null);
        Thread.interrupted();

        Log.d(TAG, "exiting requestLocalPosts...");
    }
}
