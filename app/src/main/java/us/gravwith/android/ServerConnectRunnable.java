package us.gravwith.android;

import android.os.*;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * class 'ServerConnectRunnable'
 *
 * @author John C. Quinn
 * created on 11/14/15
 * last modified : 11/18/15
 *
 * A single runnable task used to establish a connection to the server.
 * Utilitzes {@link ServerTask} to manage necessary data.
 */
public class ServerConnectRunnable implements Runnable {

    interface ServerConnectMethods {

        void handleServerConnectState(int state);

        Bundle getDataBundle();

        String getSessionToken();

        String getURLPath();

        InputStream getCAInput();

        void setTaskThread(Thread connectThread);

        void setServerConnection(HttpsURLConnection serverConnection);

        void setResponseCode(int code);
    }

    private final ServerConnectMethods mTask;

    private final boolean VERBOSE = true;

    private final String TAG = "ServerConnectRunnable";

    private final int NUMBER_OF_CONNECT_TRIES = 5;

    // Constants for indicating the state of the decode
    static final int CONNECT_STATE_FAILED = -1;
    static final int CONNECT_STATE_STARTED = 0;
    static final int CONNECT_STATE_COMPLETED = 1;


    private final int SERVER_SOCKET = 443; //does not change
    private final String CONNECTION_PROTOCOL = "https";
    private final int READ_TIMEOUT = 10000;
    private final int CONNECT_TIMEOUT = 20000;

    private final int BASE_RE_ATTEMPT_DELAY = 1000;

    private static final String SERVER_URL = "gravitybackend.ddns.net";
    private static final String SERVER_URL_SANDBOX = "dev-gravity.ddns.net";

    public ServerConnectRunnable (ServerConnectMethods methods){
        mTask = methods;
    }

    @Override
    public void run() {
        mTask.setTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        HttpsURLConnection conn = null;

        SSLContext sslContext = null;

       /* try {

            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            InputStream caInput = mTask.getCAInput();

            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
        } catch (CertificateException e) {
            Log.e(TAG,"certificate exception...",e);
        } catch (NoSuchAlgorithmException ex) {
            Log.e(TAG,"no such algoirthm......",ex);
        } catch (KeyStoreException exs) {
            Log.e(TAG,"KeyStoreException......",exs);
        } catch (KeyManagementException exm) {
            Log.e(TAG,"KeyManagementException......",exm);
        } catch (IOException eio) {
            Log.e(TAG,"IOException......",eio);
        }*/

        //trust all hosts //todo remove this, implement our custom CA
        trustAllHosts();

/*        if (sslContext == null) {
            Log.e(TAG, "failed to create proper ssl context... cannot connect.");
            mTask.handleServerConnectState(CONNECT_STATE_STARTED);
            mTask.handleServerConnectState(CONNECT_STATE_FAILED);
            mTask.setTaskThread(null);
            return;
        }*/

        int responseCode = -1;

       /*
        * uses a flag to track success
        * This method assumes that an error WILL be thrown if a server failure occurs
        */
        boolean success = true;

        try {
                mTask.handleServerConnectState(CONNECT_STATE_STARTED);

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (VERBOSE) Log.v(TAG, "creating to " + mTask.getURLPath());
                URL url;

                try {

                    if (BuildConfig.DEBUG) {
                        Log.i(TAG,"Connecting to Sandbox Server...");
                        url = new URL(
                                CONNECTION_PROTOCOL,
                                SERVER_URL_SANDBOX,
                                SERVER_SOCKET,
                                mTask.getURLPath()
                        );
                    } else {
                        url = new URL(
                                CONNECTION_PROTOCOL,
                                SERVER_URL,
                                SERVER_SOCKET,
                                mTask.getURLPath()
                        );
                    }

                } catch (MalformedURLException e) {
                    Log.e(TAG, "MalformedURL provided...", e);
                    mTask.handleServerConnectState(CONNECT_STATE_FAILED);
                    return;
                }

                int attempt = 0;

                /*
                 * Attempts to create and set the connection to the server
                 * continues to retry with a delay of 1 * 2^n seconds, where n is the number
                 * of previous attempts. Times out and reports after 5 attempts.
                 */
                do {
                    Log.i(TAG,"connecting to server...");

                    try {
                        success = true;

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
                        conn.setRequestProperty("X-Client-SessionToken",mTask.getSessionToken());
                        conn.setUseCaches(false);

                        //set custom TrustManager to trust our CA
                        //conn.setSSLSocketFactory(sslContext.getSocketFactory());

                    } catch (ConnectException e) {
                        Log.e(TAG, "Failed to open a connection to the server...", e);
                        success = false;
                        responseCode = getResponseCode(conn);
                    } catch (ProtocolException e) {
                        Log.e(TAG, "ProtocolException when trying to open a connection to the " +
                                "server...", e);
                        success = false;
                        responseCode = getResponseCode(conn);
                    } catch (SocketException e) {
                        Log.e(TAG, "SocketException...", e);
                        success = false;
                        responseCode = getResponseCode(conn);
                    } catch (IOException e) {
                        Log.e(TAG,
                                "IOException when trying to open a connection to the server...",
                                e);
                        success = false;
                        responseCode = getResponseCode(conn);
                    } finally {
                        if (attempt > 0) {
                            Double delay = BASE_RE_ATTEMPT_DELAY * Math.pow(2, attempt);
                            if (VERBOSE) Log.v(TAG,"thread sleeping for " + delay + " seconds...");
                            Thread.sleep(delay.longValue());
                        }
                        attempt++;
                    }
                } while (!success && attempt < NUMBER_OF_CONNECT_TRIES);

            } catch (InterruptedException e) {
                Log.e(TAG,"interruptedException occurred...",e);
            } finally {
                mTask.setServerConnection(conn);
                if (success) {
                    mTask.handleServerConnectState(CONNECT_STATE_COMPLETED);
                } else {
                    mTask.handleServerConnectState(CONNECT_STATE_FAILED);
                    mTask.setResponseCode(responseCode);
                }
                Thread.interrupted();
            }

        mTask.setTaskThread(null);
    }

    private int getResponseCode(HttpsURLConnection sConn) {
        int responseCode = -1;
        try {
            responseCode = sConn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG,"error getting response code...");
        }
        return responseCode;
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
