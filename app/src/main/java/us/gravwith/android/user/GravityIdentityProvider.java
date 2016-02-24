package us.gravwith.android.user;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.regions.Regions;

/**
 * Created by John C. Quinn on 1/4/16.
 */
public class GravityIdentityProvider extends AWSAbstractCognitoDeveloperIdentityProvider {

    /** Log tag. */
    private static final String TAG = GravitySignInProvider.class.getSimpleName();

    /** Facebook's callback manager. */
    //private CallbackManager facebookCallbackManager;

    /** User's name. */
    private String userName;

    /** User's image Url. */
    private String userImageUrl;

    public GravityIdentityProvider(String accountId, String identityPoolId, Regions region) {
        super(accountId, identityPoolId, region);
        // Initialize any other objects needed here.
    }

    // Return the developer provider name which you choose while setting up the
    // identity pool in the &COG; Console

    @Override
    public String getProviderName() {
        Log.i(TAG,"entering getProviderName()...");
        return AWSConfiguration.DEVELOPER_AUTHENTICATION_SANDBOX_PROVIDER_ID;
    }

    // Use the refresh method to communicate with your backend to get an
    // identityId and token.

    @Override
    public String refresh() {
        Log.i(TAG,"entering refresh...");

        // Override the existing token
        //setToken(null);

        // Get the identityId and token by making a call to your backend
        // (Call to your backend)

        // Call the update method with updated identityId and token to make sure
        // these are ready to be used from Credentials Provider.

        update(identityId, token);
        return token;
    }

    // If the app has a valid identityId return it, otherwise get a valid
    // identityId from your backend.
    @Override
    public String getIdentityId() {
        Log.i(TAG, "entering getIdentityId");

        // Load the identityId from the cache
        identityId = AuthenticationManager.getCurrentIdentityId();

        if (identityId == null) {
            // Call to your backend
            Log.i(TAG,"identity ID was null");
            return null;
        } else {
            return identityId;
        }

    }


}