package us.gravwith.android.user;

import android.util.Log;

import com.amazonaws.mobile.user.IdentityManager;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import us.gravwith.android.DataHandlingService;

/**
 * Created by John C. Quinn on 1/4/16.
 *
 *
 */
public class LoginManager implements IdentityManager.IdentityHandler, LoginRunnable.LoginUserMethods,
        InitializeUserRunnable.InitializeUserMethods {

    private final String TAG = LoginManager.class.getSimpleName();

    private DataHandlingService mService;

    private Thread mLoginThread;

    private String token;
    private UUID userID;

    public LoginManager(DataHandlingService executionService) {
        mService = executionService;
    }

    private static List<authenticationStatusListener> loginListeners = new LinkedList<>();

    public interface authenticationStatusListener {
        void onInitializeFailed();
        void onInitializeStarted();
        void onInitializeSuccess(UUID userID);
        void onLoginFailed();
        void onLoginStarted();
        void onLoginSuccess(String userToken);
    }

    public static void addAuthenticationStatusListener(authenticationStatusListener listener) {
        loginListeners.add(listener);
    }

    public static void clearAuthenticationStatusListeners() {
        loginListeners.clear();
    }

    public static int INVALID_STATE_RETURNED = 1337;

    @Override
    public void handleLoginState(int state) {

        int status;
        switch (state) {
            case LoginRunnable.LOGIN_STARTED:
                status = LOGIN_STARTED;
                break;
            case LoginRunnable.LOGIN_FAILED:
                status = LOGIN_FAILED;
                break;
            case LoginRunnable.LOGIN_SUCCESS:
                status = LOGIN_SUCCESS;
                break;
            default:
                throw new RuntimeException("invalid login status returned");
        }
        notifyListenersOfAuthStatus(status);
    }

    public void handleInitializeState(int state) {
        int outState;

        switch (state) {
            case InitializeUserRunnable.GET_UUID_FAILED:
                Log.d(TAG, "initialize user failed...");
                outState = INITIALIZE_FAILED;
                break;

            case InitializeUserRunnable.GET_UUID_STARTED:
                Log.d(TAG,"initialize user started...");
                outState = INITIALIZE_STARTED;
                break;

            case InitializeUserRunnable.GET_UUID_SUCCESS:
                Log.d(TAG, "initialize user success...");
                outState = INITIALIZE_SUCCESS;
                break;

            default :
                throw new RuntimeException("invalid initialize state returned");
        }

        notifyListenersOfAuthStatus(outState);
    }



    private final int LOGIN_STARTED = 0;
    private final int LOGIN_FAILED =  1;
    private final int LOGIN_SUCCESS = 2;
    private final int INITIALIZE_STARTED = 3;
    private final int INITIALIZE_FAILED = 4;
    private final int INITIALIZE_SUCCESS = 5;


    private int notifyListenersOfAuthStatus(int status) {

        //determine which method to call
        int numOfListeners = 0;

        switch (status) {

            case INITIALIZE_FAILED:
                for (authenticationStatusListener listener : loginListeners){
                    listener.onInitializeFailed();
                    numOfListeners++;
                }
                break;

            case INITIALIZE_STARTED:
                for (authenticationStatusListener listener : loginListeners){
                    listener.onInitializeStarted();
                    numOfListeners++;
                }
                break;

            case INITIALIZE_SUCCESS:
                for (authenticationStatusListener listener : loginListeners){
                    listener.onInitializeSuccess(userID);
                    numOfListeners++;
                }
                break;

            case LOGIN_FAILED:
                for (authenticationStatusListener listener : loginListeners){
                    listener.onLoginFailed();
                    numOfListeners++;
                }
                break;

            case LOGIN_STARTED:
                for (authenticationStatusListener listener : loginListeners){
                    listener.onLoginStarted();
                    numOfListeners++;
                }
                break;

            case LOGIN_SUCCESS:
                for (authenticationStatusListener listener : loginListeners){
                    listener.onLoginSuccess(token);
                    numOfListeners++;
                }
                break;
        }

        return numOfListeners;
    }

    @Override
    public String getLoginUrlPath() {
        return "/security/login/";
    }

    @Override
    public String getInitializeUrlPath() {
        return "/security/create/";
    }

    @Override
    public void setTaskThread(Thread thread) {
        mLoginThread = thread;
    }

    @Override
    public void setUserToken(String returnedToken) {
        Log.i(TAG,"setting token to : " + returnedToken);
        token = returnedToken;
    }

    @Override
    public void setUserID(UUID userID) {
        Log.i(TAG,"setting token to : " + userID.toString());
        this.userID = userID;
    }

    @Override
    public UUID getUserID() {
        return userID;
    }

    public String getToken() {
        return token;
    }

    public void tryLogin(ExecutorService executor) {
        executor.execute(new LoginRunnable(this));
    }

    public void tryInitialize(ExecutorService executor) {
        executor.execute(new InitializeUserRunnable(this));
    }

    public LoginRunnable getLoginRunnable(){
        return new LoginRunnable(this);
    }

    public InitializeUserRunnable getInitRunnable(){
        return new InitializeUserRunnable(this);
    }

    public static void SignIn() {

    }

    @Override
    public void handleIdentityID(String identityId) {
        Log.i(TAG,"setting userID to : " + identityId);
    }

    @Override
    public void handleError(Exception exception) {
        Log.e(TAG,"Error getting cognito identiy ID",exception);
    }
}
