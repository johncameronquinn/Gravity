package us.gravwith.android;

import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.services.sns.model.AuthorizationErrorException;
import com.amazonaws.services.sns.model.InternalErrorException;
import com.amazonaws.services.sns.model.TopicLimitExceededException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.security.InvalidParameterException;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by John C. Quinn on 11/12/15.
 *
 * this class sends a blank object to the server.
 * this will be used by many objects in combination with a responserunnable
 */
public class CreateGCMTopicRunnable implements Runnable {

    private final boolean VERBOSE = true;
    private final String TAG = CreateGCMTopicRunnable.class.getSimpleName();
    private final CreateGCMTopicMethods mService;


    static final int CREATE_TOPIC_FAILED = -1;
    static final int CREATE_TOPIC_STARTED = 0;
    static final int CREATE_TOPIC_SUCCESS = 1;


    interface CreateGCMTopicMethods {

        void setTaskThread(Thread thread);
        void handleCreateGCMTopicState(int state);
        Bundle getDataBundle();
        void setTopicARN(String arn);

    }

    public CreateGCMTopicRunnable(CreateGCMTopicMethods methods) {
        mService = methods;
    }


    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "entering sendBlankObjectRunnable...");
        }

        mService.setTaskThread(Thread.currentThread());
        mService.handleCreateGCMTopicState(CREATE_TOPIC_STARTED);

        boolean success = true;

        String arn = null;
        try {
            if (Thread.interrupted()) {
                if (VERBOSE)Log.v(TAG,"current thread is interrupted, not sending...");
                return;
            }

            arn = AWSMobileClient
                    .defaultMobileClient()
                    .getPushManager()
                    .createTopic(
                            mService.getDataBundle()
                                    .getString(SQLiteDbContract.LiveEntry.COLUMN_NAME_FILEPATH)
                    );

        } catch (InvalidParameterException e) {
            Log.e(TAG, "Invalid parameters...", e);
            success = false;
        } catch (TopicLimitExceededException ex) {
            Log.e(TAG, "Topic Limit Exceeded...", ex);
            success = false;
        } catch (InternalErrorException exe) {
            Log.e(TAG, "Internal error exception...", exe);
            success = false;
        } catch (AuthorizationErrorException exa) {
            Log.e(TAG, "Authorization Error Occurred...", exa);
            success = false;
        } finally {

            if (success && arn != null) {
                Log.d(TAG, "No Exceptions, so... success :3");
                mService.setTopicARN(arn);
                mService.handleCreateGCMTopicState(CREATE_TOPIC_SUCCESS);
            } else {
                Log.d(TAG, "Something went wrong...");
                mService.handleCreateGCMTopicState(CREATE_TOPIC_FAILED);
            }
        }

        mService.setTaskThread(null);
        if (VERBOSE) Log.v(TAG, "exiting SendBlankObjectRunnable...");

    }
}
