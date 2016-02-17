package us.gravwith.android;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import us.gravwith.android.SQLiteDbContract.LiveReplies;
import us.gravwith.android.util.LogUtils;

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
public class SendReplyRunnable implements Runnable{

    interface SendReplyMethods {

        void handleSendReplyState(int state);

        void setTaskThread(Thread thread);

        Bundle getDataBundle();

        HttpsURLConnection getURLConnection();
    }

    static final int REQUEST_FAILED = -1;
    static final int REQUEST_STARTED = 0;
    static final int REQUEST_SUCCESS = 1;

    private final boolean VERBOSE = true;

    private final String TAG = "SendReplyRunnable";

    private final SendReplyMethods mTask;

    public SendReplyRunnable(SendReplyMethods methods) {
        mTask = methods;
    }

    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter SendReplyRunnable...");
        }

        mTask.setTaskThread(Thread.currentThread());

        HttpsURLConnection conn = null;

        Bundle b = mTask.getDataBundle();
        if (VERBOSE) LogUtils.printBundle(b, TAG);
        String imageKey = b.getString(
                LiveReplies.COLUMN_NAME_FILEPATH,
                ""
        );

        int responseCode = -10;
        try {
            mTask.handleSendReplyState(REQUEST_STARTED);

            conn = mTask.getURLConnection();
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server
            jGen.writeStartObject();
            jGen.writeStringField(LiveReplies.COLUMN_NAME_THREAD_ID,
                    b.getString(LiveReplies.COLUMN_NAME_THREAD_ID,"")
            );
            jGen.writeStringField(LiveReplies.COLUMN_NAME_DESCRIPTION,
                    b.getString(LiveReplies.COLUMN_NAME_DESCRIPTION, "")
            );
            jGen.writeStringField(LiveReplies.COLUMN_NAME_FILEPATH,
                    imageKey
            );
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
            mTask.handleSendReplyState(REQUEST_FAILED);
        } finally {
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                mTask.handleSendReplyState(REQUEST_SUCCESS);
                if (!"".equals(imageKey)) {
                    File file = new File(imageKey);
                    Log.i(TAG,"file is stored at" + imageKey);
                    file.delete();
                }
            } else {
                mTask.handleSendReplyState(REQUEST_FAILED);
                Log.e(TAG,"response code returned: " + responseCode);
                //todo retry request
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        mTask.setTaskThread(null);
        if (VERBOSE) {
            Log.v(TAG, "exiting SendReplyRunnable...");
        }
    }
}
