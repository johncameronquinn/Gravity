package us.gravwith.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.push.PushManager;

import java.util.concurrent.CountDownLatch;

/**
 * Splash Activity is the start-up activity that appears until a delay is expired
 * or the user taps the screen.  When the splash activity starts, various app
 * initialization operations are performed.
 */
public class SplashActivity extends Activity {
    private final static String LOG_TAG = SplashActivity.class.getSimpleName();
    private final CountDownLatch timeoutLatch = new CountDownLatch(1);

    /** SERVICE MANAGEMENT
     */
    private boolean isBound = false;

    private Messenger mService;

    /**
     * Connection to the service - from google example
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.d(LOG_TAG,"binding to service...");
            mService = new Messenger(service);
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d(LOG_TAG,"unbinding from service...");
            mService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Set a listener for changes in push notification state
        PushManager.setPushStateListener(new PushManager.PushStateListener() {
            @Override
            public void onPushStateChange(final PushManager pushManager, boolean isEnabled) {
                Log.d(LOG_TAG, "Push Notifications Enabled = " + isEnabled);
                // ...Put any application-specific logic here...
            }
        });

        Log.d(LOG_TAG, "binding the service to this class, creating if necessary");
        Intent intent = new Intent(this, DataHandlingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        final Thread thread = new Thread(new Runnable() {
            public void run() {

                //todo add sign-in via initializeUser here

                // if the user was already previously in to a provider.
                goMain();

                // Wait for the splash timeout.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { }

                // Expire the splash page delay.
                timeoutLatch.countDown();
            }
        });
        thread.start();
    }

    @Override
    protected void onDestroy() {

        //unbind the service now
        if (isBound) {

            Log.d(LOG_TAG, "sending message to disconnect GoogleApiClient...");
            Message msg1 = Message.obtain(null, DataHandlingService.MSG_DISCONNECT_CLIENT, 0, 0);

            try {
                mService.send(msg1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, "unbinding the service...");
            unbindService(mConnection);
            isBound = false;
        }

        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Touch event bypasses waiting for the splash timeout to expire.
        timeoutLatch.countDown();
        return true;
    }

    /**
     * Starts an activity after the splash timeout.
     * @param intent the intent to start the activity.
     */
    private void goAfterSplashTimeout(final Intent intent) {
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                // wait for the splash timeout expiry or for the user to tap.
                try {
                    timeoutLatch.await();
                } catch (InterruptedException e) {
                }

                SplashActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        startActivity(intent);
                        // finish should always be called on the main thread.
                        finish();
                    }
                });
            }
        });
        thread.start();
    }

    /**
     * Go to the main activity after the splash timeout has expired.
     */
    protected void goMain() {
        Log.d(LOG_TAG, "Launching Main Activity...");
        goAfterSplashTimeout(new Intent(this, MainActivity.class));
    }

    /**
     * Go to the sign in activity after the splash timeout has expired.
     */
    protected void goSignIn() {
        Log.d(LOG_TAG, "Launching Sign-in Activity...");
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!isBound) {
            Log.d(LOG_TAG, "binding the service to this class, creating if necessary");
            Intent intent = new Intent(this, DataHandlingService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(LOG_TAG, "enter onStop...");

        //unbind the service now
        if (isBound) {

            Log.d(LOG_TAG, "sending message to disconnect GoogleApiClient...");
            Message msg1 = Message.obtain(null, DataHandlingService.MSG_DISCONNECT_CLIENT, 0, 0);

            try {
                mService.send(msg1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, "unbinding the service...");
            unbindService(mConnection);
            isBound = false;
        }
        Log.d(LOG_TAG, "exit onStop...");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
