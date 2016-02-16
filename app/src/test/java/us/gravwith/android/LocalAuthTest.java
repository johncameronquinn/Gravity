package us.gravwith.android;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.test.mock.MockApplication;
import android.test.mock.MockResources;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import us.gravwith.android.user.LoginManager;

/**
 * Created by John C. Quinn on 2/15/16.
 *
 * This class tests the login and authentication system on a live android device
 */

@SmallTest
public class LocalAuthTest {

    private final String TAG = LocalAuthTest.class.getSimpleName();

    @Test
    public void testAuthentication() {

        final LoginManager manager = new LoginManager(null);

        LoginManager.addAuthenticationStatusListener(new LoginManager.authenticationStatusListener() {
            @Override
            public void onLoginFailed() {
                System.out.println("login failed");
            }

            @Override
            public void onLoginStarted() {
                System.out.println("login started");
            }

            @Override
            public void onLoginSuccess(String userToken) {
                System.out.println("Token returned : " + userToken);
            }

            @Override
            public void onInitializeFailed() {
                System.out.println("initialize failed...");
            }

            @Override
            public void onInitializeStarted() {
                System.out.println("initialize started...");
            }

            @Override
            public void onInitializeSuccess(UUID userID) {
                System.out.println("userID returned : " + userID);
            }
        });

        manager.getInitRunnable().run();

        assert(manager.getUserID() != null);

        manager.getLoginRunnable().run();

        assert(manager.getToken() != null);
    }
}
