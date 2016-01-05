package us.gravwith.android.user;

import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;

import us.gravwith.android.util.LogUtils;

/**
 * Created by John C. Quinn on 11/9/15.
 *
 * class 'InitializeUserRunnable'
 *
 * connects to the server, and attempts to get a userID, for use in future connections.
 * requires {@link LoginRunnable.LoginUserMethods} to be
 * implemented in the calling class.
 *
 * passes the state, success or fail, back to its caller
 */
public class LoginRunnable implements Runnable {

    private boolean VERBOSE = true;

    private String TAG = "InitializeUserRunnable";

    public static final int LOGIN_FAILED = -1;
    public static final int LOGIN_STARTED = 0;
    public static final int LOGIN_SUCCESS = 1;

    interface LoginUserMethods {

        HttpURLConnection getURLConnection();

        void handleLoginState(int state);

        void setUserToken(String returnedToken);

        void setTaskThread(Thread thread);
    }

    final LoginUserMethods mService;

    public LoginRunnable(LoginUserMethods methods) {
        mService = methods;
    }

    @Override
    public void run() {
        if (VERBOSE) {
            Log.v(TAG, "enter InitializeUser...");
        }

        mService.setTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        if (Thread.interrupted()) {
            return;
        }

        mService.handleLoginState(LOGIN_STARTED);
        HttpURLConnection conn;
        conn = mService.getURLConnection();

        UUID userID = null;

        try {

            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            InputStream responseStream = conn.getInputStream();
            if (responseStream == null) {
                Log.e(TAG, "No input stream was retrieved from the connection... exiting...");
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> asMap = objectMapper.readValue(conn.getInputStream(), Map.class);
            userID = UUID.fromString(String.valueOf(asMap.get("id")));

            if (VERBOSE) {
                LogUtils.printMapToVerbose(asMap, TAG);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing initialize user...", e);
            //Toast.makeText(getApplicationContext(),"Error initializing new user with the server...",Toast.LENGTH_SHORT).show();
        }

        if (userID == null) {
            Log.e(TAG, "userID was not successfully retrieved...");
            mService.handleLoginState(LOGIN_FAILED);
        } else {
            Log.i(TAG,"userID retrieved : " + userID.toString());

            mService.setUserToken(userID.toString());
            mService.handleLoginState(LOGIN_SUCCESS);
        }

        mService.setTaskThread(null);
        Thread.interrupted();
        if (VERBOSE) {
            Log.v(TAG, "exiting InitializeUser...");
        }
    }

}
