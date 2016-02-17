package us.gravwith.android;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import us.gravwith.android.util.LogUtils;
import us.gravwith.android.util.ThreadUtils;


/**
 * Created by John C. Quinn on 11/12/15.
 */
public class RequestLiveThreadsRunnable implements Runnable {

    private final boolean VERBOSE = false;
    private final String TAG = "RequestThreadsRunnable";
    private final ThreadRequestMethods mService;


    static final int REQUEST_THREADS_FAILED = -1;
    static final int REQUEST_THREADS_STARTED = 0;
    static final int REQUEST_THREADS_SUCCESS = 1;


    interface ThreadRequestMethods extends ServerTask.ServerTaskMethods {

        void handleThreadsRequestState(int state);

        void insert(Uri uri, ContentValues values);

        void delete(Uri uri, ContentValues values);
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

        HttpsURLConnection conn = null;
        boolean success = true;

        int responseCode = -1;

        try {
            if (Thread.interrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not sending...");
                return;
            }

            conn = mService.getURLConnection();

            JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();

            ArrayList<LinkedHashMap<String, Object>> jsonArray =
                    objectMapper.readValue(jParser, ArrayList.class);

            List<ContentValues> valuesList = new ArrayList<>();

            if (!jsonArray.isEmpty()) {
                LinkedHashMap<String, Object> map;

                for (int i = 0; i < jsonArray.size(); i++) {
                    map = jsonArray.get(i);
                    map.put(SQLiteDbContract.LiveEntry.COLUMN_ID, map.remove("order"));
                    map.put(SQLiteDbContract.LiveEntry.COLUMN_NAME_THREAD_ID, map.remove("id"));
                    //map.put(SQLiteDbContract.LiveEntry.COLUMN_ID, map.remove("id"));
                    //map.put(SQLiteDbContract.LiveEntry.COLUMN_NAME_THREAD_ID, map.remove("id"));

                    android.os.Parcel myParcel = android.os.Parcel.obtain();
                    myParcel.writeMap(map);
                    myParcel.setDataPosition(0);
                    android.content.ContentValues values = android.content.ContentValues
                            .CREATOR.createFromParcel(myParcel);
                    valuesList.add(values);

                    if (VERBOSE)  {
                        Log.v(TAG,"printing incoming post object...");
                        LogUtils.printMapToVerbose(map, TAG);
                        Log.v(TAG,"done printing incoming post object.");
                    }
                }

                mService.delete(FireFlyContentProvider.CONTENT_URI_LIVE,null);

                for (ContentValues row : valuesList) {
                     mService.insert(FireFlyContentProvider.CONTENT_URI_LIVE, row); //todo implement bulkinsert
                }
            }

            responseCode = conn.getResponseCode();

        } catch (IOException e) {
            Log.e(TAG, "IOException when retrieving live threads...", e);
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
                Log.d(TAG, "No Exceptions and the response code is OK, success :3");
                mService.handleThreadsRequestState(REQUEST_THREADS_SUCCESS);
            } else {
                Log.d(TAG, "Something went wrong...");
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
