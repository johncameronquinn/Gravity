package us.gravwith.android.user;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.amazonaws.mobile.user.signin.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.logging.Logger;

import us.gravwith.android.util.LogUtils;
import us.gravwith.android.util.Utility;

/**
 * Created by John C. Quinn on 1/4/16.
 */
public class GravitySignInProvider implements IdentityProvider {

    /** Log tag. */
    private static final String TAG = GravitySignInProvider.class.getSimpleName();

    /** Facebook's callback manager. */
    //private CallbackManager facebookCallbackManager;

    /** User's name. */
    private String userName;

    /** User's image Url. */
    private String userImageUrl;

    /**
     * Constuctor. Intitializes the SDK and debug logs the app KeyHash that must be set up with
     * the facebook backend to allow login from the app.
     *
     * @param context the context.
     */
    public GravitySignInProvider(final Context context) {

        /*if (!FacebookSdk.isInitialized()) {
            Log.d(LOG_TAG, "Initializing Facebook SDK...");
            FacebookSdk.sdkInitialize(context);
            Utils.logKeyHash(context);
        }*/
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "Gravity";
    }

    /** {@inheritDoc} */
    @Override
    public String getCognitoLoginKey() {
        return AWSConfiguration.DEVELOPER_AUTHENTICATION_SANDBOX_PROVIDER_ID;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserSignedIn() {
        return AuthenticationManager.getCurrentAccessToken() != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getToken() {
        /*AccessToken accessToken = getSignedInToken();
        if (accessToken != null) {
            return accessToken.getToken();
        }
        return null;*/

        return AuthenticationManager.getCurrentAccessToken();
    }

    /** {@inheritDoc} */
    @Override
    public void signOut() {
        clearUserInfo();
    }

    private void clearUserInfo() {
        userName = null;
        userImageUrl = null;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserName() {
        return userName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserImageUrl() {
        return userImageUrl;
    }

    /** {@inheritDoc} */
    public void reloadUserInfo() {
        clearUserInfo();
        if (!isUserSignedIn()) {
            return;
        }
        Log.e(TAG,"not implemented at this moment...");
    }

}
