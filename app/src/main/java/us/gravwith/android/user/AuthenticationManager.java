package us.gravwith.android.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.mobile.user.IdentityManager;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import us.gravwith.android.Constants;
import us.gravwith.android.DataHandlingService;

/**
 * Created by John C. Quinn on 1/4/16.
 *
 *
 */
public class AuthenticationManager implements IdentityManager.IdentityHandler {

    private final String TAG = AuthenticationManager.class.getSimpleName();

    private DataHandlingService mService;

    private static String token;
    private static UUID userID;

    private final AuthenticationCallbackManager callbackManager;

    public AuthenticationManager(final DataHandlingService executionService) {
        mService = executionService;
        callbackManager = new AuthenticationCallbackManager(this);

        AuthenticationCallbackManager
                .addAuthenticationStatusListener(new AuthenticationCallbackManager
                        .authenticationStatusListener() {

                    @Override
                    public void onInitializeFailed() {

                    }

                    @Override
                    public void onInitializeStarted() {

                    }

                    @Override
                    public void onInitializeSuccess(UUID id) {
                        userID = id;

                        Log.i(TAG, "saving new id : " + id.toString() + " in sharedPreferences");
                        SharedPreferences p = executionService.getApplicationContext()
                                .getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                                        Context.MODE_PRIVATE);
                        p.edit().putString(Constants.KEY_USER_ID, id.toString()).apply();
                    }

                    @Override
                    public void onLoginFailed() {

                    }

                    @Override
                    public void onLoginStarted() {

                    }

                    @Override
                    public void onLoginSuccess(String userToken) {
                        Log.i(TAG, "successfully retrieved token : " + userToken);
                        token = userToken;
                    }
                });
    }

    public boolean loadUserID(Context c) {
        SharedPreferences p = c.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        boolean idFound = p.contains(Constants.KEY_USER_ID);

        if (idFound) {
            userID = UUID.fromString(p.getString(Constants.KEY_USER_ID, null));

            Log.i(TAG,"loaded userID : " + userID + " : from sharedPreferences");
        }

        return idFound;
    }

    public static UUID getCurrentUserID() {
        return userID;
    }

    public static String getCurrentAccessToken() {
        return token;
    }

    public static void createNewUser(AuthenticationManager manager) {
        manager.mService.submitToConnectionPool(new InitializeUserRunnable(manager.callbackManager));
    }

    public static void loginUser(AuthenticationManager manager) {
        manager.mService.submitToConnectionPool(new LoginRunnable(manager.callbackManager));
    }

    public LoginRunnable getLoginRunnable(){
        return new LoginRunnable(callbackManager);
    }

    public InitializeUserRunnable getInitRunnable(){
        return new InitializeUserRunnable(callbackManager);
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
