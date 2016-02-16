package us.gravwith.android;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import us.gravwith.android.util.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by John C. Quinn on 11/9/15.
 *
 * Last modified: 12/21/2015
 *
 * class 'RequestRepliesRunnable'
 *
 * this runnable is used to post a request to the server, parse any incoming json, and store
 * that json in the relevant ContentProvider
 *
 * it attempts to accurately track its status, and reports success only if the HTTP response code
 * is 200, AND no exceptions have occurred saving and storing the data
 */
class RequestRepliesRunnable implements Runnable {

    private boolean VERBOSE = true;

    private String TAG = "RequestRepliesRunnable";

    private final ReplyRequestMethods mService;

    static final int REQUEST_REPLIES_FAILED = -1;
    static final int REQUEST_REPLIES_STARTED = 0;
    static final int REQUEST_REPLIES_SUCCESS = 1;

    interface ReplyRequestMethods extends ServerTask.ServerTaskMethods {

        void handleRepliesRequestState(int state);

        void insert(Uri uri, ContentValues values);
    }


    public RequestRepliesRunnable(ReplyRequestMethods methods) {
        mService = methods;
    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveReplies...");
        }

        mService.setTaskThread(Thread.currentThread());

        HttpsURLConnection conn = null;
        Bundle b = mService.getDataBundle();
        LogUtils.printBundle(b, TAG);

        boolean success = true;

        int responseCode = -1;

        try {
            if (Thread.interrupted()) {
                return;
            }

            mService.handleRepliesRequestState(REQUEST_REPLIES_STARTED);

            conn = mService.getURLConnection();

            if (Thread.interrupted()) {
                return;
            }

            /* send necessary data to the server */
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "thread id: " +
                        b.getInt("threadID"));
            }

            jGen.writeStartObject();
            jGen.writeNumberField("threadID", b.getInt("threadID"));
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            if (Thread.currentThread().isInterrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not storing...");
                return;
            }

            /* send receive incoming data from the server, parsing as JSON */
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

            /* store incoming data in the content provider */
            for (ContentValues row : valuesList) {
                mService.insert(FireFlyContentProvider.CONTENT_URI_REPLY_LIST,row); //todo implement bulkinsert
            }

            responseCode = conn.getResponseCode();

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
            mService.setResponseCode(responseCode);

            if (success && responseCode == HttpsURLConnection.HTTP_OK) {
                mService.handleRepliesRequestState(REQUEST_REPLIES_SUCCESS);
            } else {
                mService.handleRepliesRequestState(REQUEST_REPLIES_FAILED);
            }
        }

        mService.setTaskThread(null);
        Thread.interrupted();
        if (VERBOSE) Log.v(TAG, "exiting requestLiveReplies...");
    }
}
