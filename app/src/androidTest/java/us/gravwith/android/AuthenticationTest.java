package us.gravwith.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import us.gravwith.android.user.LoginManager;

/**
 * Created by John C. Quinn on 2/15/16.
 *
 * This class tests the login and authentication system on a live android device
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AuthenticationTest {

    private final String TAG = AuthenticationTest.class.getSimpleName();

    @Test
    public void testAuthentication() {
        final LoginManager manager = new LoginManager(null);

        LoginManager.addAuthenticationStatusListener(new LoginManager.authenticationStatusListener() {
            @Override
            public void onLoginFailed() {
                Log.i(TAG,"login failed...");
            }

            @Override
            public void onLoginStarted() {
                Log.i(TAG,"login started...");
            }

            @Override
            public void onLoginSuccess(String userToken) {
                Log.i(TAG,"Token returned : " + userToken);
            }

            @Override
            public void onInitializeFailed() {
                Log.i(TAG,"initialize failed...");
            }

            @Override
            public void onInitializeStarted() {
                Log.i(TAG,"initialize started...");
            }

            @Override
            public void onInitializeSuccess(UUID userID) {
                Log.i(TAG,"userID returned : " + userID);
            }
        });


        ExecutorService service = new ScheduledThreadPoolExecutor(8);
        manager.tryInitialize(service);
    }
}
