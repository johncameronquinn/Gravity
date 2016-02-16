package us.gravwith.android.user;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import android.util.Log;

import us.gravwith.android.BuildConfig;
import us.gravwith.android.Constants;
import us.gravwith.android.util.LogUtils;
import us.gravwith.android.util.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by John C. Quinn on 11/9/15.
 *
 * class 'InitializeUserRunnable'
 *
 * connects to the server, and attempts to get a userID, for use in future connections.
 * requires {@link InitializeUserRunnable.InitializeUserMethods} to be
 * implemented in the calling class.
 *
 * passes the state, success or fail, back to its caller
 */
public class InitializeUserRunnable implements Runnable {

    private String TAG = InitializeUserRunnable.class.getSimpleName();

    private final int SERVER_SOCKET = 443; //does not change
    private final String CONNECTION_PROTOCOL = "https";
    private final int READ_TIMEOUT = 10000;
    private final int CONNECT_TIMEOUT = 20000;

    private static final String SERVER_URL = "gravitybackend.ddns.net";
    private static final String SERVER_URL_SANDBOX = "dev-gravity.ddns.net";


    static final int GET_UUID_FAILED = -1;
    static final int GET_UUID_STARTED = 0;
    static final int GET_UUID_SUCCESS = 1;

    public interface InitializeUserMethods {

        String getInitializeUrlPath();

        void handleInitializeState(int state);

        void setUserID(UUID userID);

        void setTaskThread(Thread thread);

        void setInitResponseCode(int code);
    }

    final InitializeUserMethods mService;

    public InitializeUserRunnable(InitializeUserMethods methods) {
        mService = methods;
    }


    @Override
    public void run() {
        if (Constants.AUTHENTICATIONV) {
            Log.v(TAG, "enter InitializeUser...");
        }

        mService.setTaskThread(Thread.currentThread());
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        if (Thread.interrupted()) {
            return;
        }

        HttpsURLConnection conn = null;
        int responseCode = -1;

        mService.handleInitializeState(GET_UUID_STARTED);
        Log.w(TAG, "removing CA checking from host");
        trustAllHosts();

        if (Constants.AUTHENTICATIONV) Log.v(TAG, "creating to " + mService.getInitializeUrlPath());
        URL url;

        try {

            if (BuildConfig.DEBUG) {
                Log.i(TAG,"Connecting to Sandbox Server...");
                url = new URL(
                        CONNECTION_PROTOCOL,
                        SERVER_URL_SANDBOX,
                        SERVER_SOCKET,
                        mService.getInitializeUrlPath()
                );
            } else {
                url = new URL(
                        CONNECTION_PROTOCOL,
                        SERVER_URL,
                        SERVER_SOCKET,
                        mService.getInitializeUrlPath()
                );
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURL provided...", e);
            mService.handleInitializeState(GET_UUID_FAILED);
            return;
        }

        UUID userID = null;

        try {
            conn = (HttpsURLConnection) url.openConnection();

            //todo remove this, renable host verification
            conn.setHostnameVerifier(DO_NOT_VERIFY);

            //set client parameters
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);

            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            InputStream responseStream = conn.getInputStream();
            if (responseStream == null) {
                Log.e(TAG, "No input stream was retrieved from the connection... exiting...");
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> asMap = objectMapper.readValue(conn.getInputStream(), Map.class);
            userID = Utility.getUUIDfromStringWithoutHyphens(String.valueOf(asMap.get("id")));

            if (Constants.AUTHENTICATIONV) {
                LogUtils.printMapToVerbose(asMap, TAG);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing initialize user...", e);

        }

        if (conn != null) {
            try {
                responseCode = conn.getResponseCode();
                mService.setInitResponseCode(responseCode);
            } catch (IOException e) {
                Log.e(TAG,"error reading responseCode from object",e);
            }
        }
        mService.setInitResponseCode(responseCode);

        if (userID == null) {
            Log.e(TAG, "userID was not successfully retrieved...");
            mService.handleInitializeState(GET_UUID_FAILED);
        } else {
            Log.i(TAG,"userID retrieved : " + userID.toString());

            mService.setUserID(userID);
            mService.handleInitializeState(GET_UUID_SUCCESS);
        }

        mService.setTaskThread(null);
        Thread.interrupted();
        if (Constants.AUTHENTICATIONV) {
            Log.v(TAG, "exiting InitializeUser...");
        }
    }


    // always verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
