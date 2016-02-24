package us.gravwith.android.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.auth.AWSCognitoIdentityProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import us.gravwith.android.Constants;
import us.gravwith.android.DataHandlingService;
import us.gravwith.android.util.Validate;

/**
 * Created by John C. Quinn on 1/4/16.
 *
 *
 */
public class AuthenticationManager {

    private final String TAG = AuthenticationManager.class.getSimpleName();

    private DataHandlingService mService;

    private static String token;
    private static String identityId;
    private static UUID userID;
    private final GravityAuthCallbackManager callbackManager;

    private final GravityIdentityProvider gravityProvider;
    private static CognitoCachingCredentialsProvider cognitoCachingProvider;

    private final String UNAUTH_ARN = "arn:aws:iam::581398785260:role/Cognito_gravity_sandbox_poolUnauth_Role";
    private final String AUTH_ARN = "arn:aws:iam::581398785260:role/gravity-user-role";

    /**
     * interface LoginListener
     *
     * this interface is used as a high-level abstraction of the login process.
     *
     * if authentication is successful for both gravity and amazon, fire success
     *
     * if authentication fails, and is unrecoverable, fire fail
     */
    public interface LoginListener {
        void onLoginFailed();
        void onLoginSuccess();
    }

    private static List<LoginListener> loginListeners = new LinkedList<>();


    public static void addLoginStatusListener(LoginListener listener) {
        loginListeners.add(listener);
    }

    public static void clearAuthenticationStatusListeners() {
        loginListeners.clear();
    }

    public AuthenticationManager(final DataHandlingService executionService) {
        mService = executionService;
        callbackManager = new GravityAuthCallbackManager(this);

        gravityProvider = new GravityIdentityProvider(AWSConfiguration.DEVELOPER_AUTHENTICATION_ACCOUNT_ID,
                AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID,
                AWSConfiguration.AMAZON_COGNITO_REGION);

        cognitoCachingProvider = new CognitoCachingCredentialsProvider(
                executionService,
                gravityProvider,
                UNAUTH_ARN,
                AUTH_ARN
        );

        callbackManager.setAuthenticationStatusListener(
                new GravityAuthCallbackManager.authenticationStatusListener() {

                    @Override
                    public void onInitializeFailed() {
                        for (LoginListener listener : loginListeners) {
                            listener.onLoginFailed();
                        }
                    }

                    @Override
                    public void onInitializeStarted() {
                    }

                    @Override
                    public void onInitializeSuccess(UUID id, String identId) {
                        userID = id;
                        identityId = identId;

                        Log.i(TAG, "saving new id : " + id.toString() + " in sharedPreferences");
                        SharedPreferences p = executionService.getApplicationContext()
                                .getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                                        Context.MODE_PRIVATE);
                        p.edit().putString(Constants.KEY_USER_ID, id.toString()).apply();
                        p.edit().putString(Constants.KEY_IDENTITY_ID, identityId).apply();
                    }

                    @Override
                    public void onLoginFailed() {
                        for (LoginListener listener : loginListeners) {
                            listener.onLoginFailed();
                        }
                    }

                    @Override
                    public void onLoginStarted() {
                    }

                    @Override
                    public void onLoginSuccess(String userToken) {
                        Log.i(TAG, "successfully retrieved token : " + userToken);
                        token = userToken;

                        for (LoginListener listener : loginListeners) {
                            listener.onLoginSuccess();
                        }
                    }
                }
        );
    }

/***************************************************************************************************
 *  ACCESSORS & MUTATORS
 */
    public boolean loadUserID(Context c) {
        SharedPreferences p = c.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        boolean idFound = p.contains(Constants.KEY_USER_ID);

        if (idFound) {
            userID = UUID.fromString(p.getString(Constants.KEY_USER_ID, null));
            identityId = p.getString(Constants.KEY_IDENTITY_ID, null);

            Log.i(TAG,"loaded userID : " + userID + " : from sharedPreferences");
        }

        return idFound;
    }

    public static UUID getCurrentUserID() {
        return userID;
    }

    public static String getCurrentIdentityId() {
        return identityId;
    }

    public static String getCurrentAccessToken() {
        return token;
    }

    /**
     * @return the Cognito credentials provider.
     */
    public static CognitoCachingCredentialsProvider getCredentialsProvider() {
        return cognitoCachingProvider;
    }

    public LoginRunnable getLoginRunnable(){
        return new LoginRunnable(callbackManager);
    }

    public InitializeUserRunnable getInitRunnable(){
        return new InitializeUserRunnable(callbackManager);
    }

/***************************************************************************************************
 * DRIVERS
**/

    public static void createNewUser(AuthenticationManager manager) {
        manager.mService.submitToConnectionPool(new InitializeUserRunnable(manager.callbackManager));
    }

    public static void loginUser(AuthenticationManager manager) {
        manager.mService.submitToConnectionPool(new LoginRunnable(manager.callbackManager));
    }

    private void loginWithAmazon() {

        Log.i(TAG, "now attempting to login with amazon");

        AWSSecurityTokenServiceClient client;

        client = new AWSSecurityTokenServiceClient(cognitoCachingProvider);

        AssumeRoleWithWebIdentityRequest request
                = new AssumeRoleWithWebIdentityRequest();

        request.setProviderId(gravityProvider.getIdentityPoolId());
        request.setWebIdentityToken(AuthenticationManager.getCurrentAccessToken());
        request.setRoleSessionName(userID.toString());
        request.setRoleArn(AUTH_ARN);

        AssumeRoleWithWebIdentityResult result = client.assumeRoleWithWebIdentity(request);

        Log.v(TAG,"assumed sessionToken");
        Log.v(TAG,result.getCredentials().getSessionToken());

    }

}
