package us.gravwith.android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class ErrorReceiver extends BroadcastReceiver {

    private final int UNAUTHORIZED = 401;
    private final int SUCCESS = 200;
    private final int NOT_FOUND = 404;
    private final int ALREADY_REPORTED = 409;
    private final int UNPROCESSABLE_ENTITY = 422;
    private final int TOO_MANY_REQUESTS = 429;
    private final int BLOCK_CONFLICT = 409;

    private static List<SecurityErrorListener> securityErrorListeners = new LinkedList<>();

    public static void addSecurityErrorListener(SecurityErrorListener listener) {
        securityErrorListeners.add(listener);
    }

    public static void clearSecurityErrorListeners() {
        securityErrorListeners.clear();
    }

    interface SecurityErrorListener {
        void onUnauthorizedError(String message);
    }

    public ErrorReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(ErrorReceiver.class.getSimpleName(), "entering onReceive...");
        // an Intent broadcast.

        switch (intent.getIntExtra(Constants.RESPONSE_CODE,-1)) {
            case UNAUTHORIZED:

                for (SecurityErrorListener listener : securityErrorListeners) {
                    listener.onUnauthorizedError(intent.getStringExtra(Constants.RESPONSE_MESSAGE));
                }

                break;

            case SUCCESS:

                break;


            case -1:
                break;
        }

        Log.v(ErrorReceiver.class.getSimpleName(), "exiting onReceive...");
    }
}
