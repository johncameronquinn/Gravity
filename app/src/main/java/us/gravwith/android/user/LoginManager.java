package us.gravwith.android.user;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by John C. Quinn on 1/4/16.
 *
 *
 */
public class LoginManager {

    private static List<LoginStatusListener> loginListeners = new LinkedList<>();

    public interface LoginStatusListener {
        void onLoginSuccess();
        void onLoginFailed();
        void onLoginStarted();
    }

    public static void addLoginListener(LoginStatusListener listener) {
        loginListeners.add(listener);
    }

    public static void clearLoginListeners() {
        loginListeners.clear();
    }

    public static int INVALID_STATE_RETURNED = 1337;

    public static void handleLoginState(int state) {
        switch (state) {
            case LoginRunnable.LOGIN_STARTED:
                for (LoginStatusListener listener : loginListeners){
                    listener.onLoginStarted();
                }
                break;
            case LoginRunnable.LOGIN_FAILED:
                for (LoginStatusListener listener : loginListeners){
                    listener.onLoginFailed();
                }
                break;
            case LoginRunnable.LOGIN_SUCCESS:
                for (LoginStatusListener listener : loginListeners){
                    listener.onLoginSuccess();
                }
                break;
        }
    }

    public static void SignIn() {

    }

}
