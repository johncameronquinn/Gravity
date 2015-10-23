package com.jokrapp.android;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Network;
import android.net.Uri;
import android.os.*;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.cognito.exceptions.NetworkException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jokrapp.android.util.LogUtils;

/**
 * Author/Copyright John C. Quinn, All Rights Reserved.
 * date 4/26/2015
 * <p/>
 * service 'DataHandlingService'
 * <p/>
 * asynchronously serves requests to download new images from the server
 * 'black boxed' - proves the server a gps location, and number of images requested
 * and receives requested number, then stores them locally
 * <p/>
 * when no images are stored, a higher priority is given to the request, due to the user
 * currently being at "0" images
 * <p/>
 * Communication is managed via a broadcastManager
 */
public class DataHandlingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, TransferListener {
    private static final String TAG = "DataHandlingService";
    private static final boolean VERBOSE = true;
    private static final boolean ALLOW_DUPLICATES = false;

    private boolean isLocalRequesting = false;


    /**
     * NETWORK COMMUNICATION
     */
    private static GoogleApiClient mGoogleApiClient;
    private final int SERVER_SOCKET = 80; //does not change
    private final String CONNECTION_PROTOCOL = "http";
    private final int READ_TIMEOUT = 10000;
    private final int CONNECT_TIMEOUT = 20000;


    private final String UPLOAD_LOCAL_POST_PATH = "/local/upload/"; //does not change
    private final String GET_LOCAL_POST_PATH = "/local/get/"; //does not change
    private final String SEND_LOCAL_MESSAGE_PATH = "/message/upload/";
    private final String GET_LOCAL_MESSAGES_PATH = "/message/get/"; //does not change
    private final String BLOCK_LOCAL_USER_PATH = "/moderation/block/";
    private final String REPORT_USER_PATH = "/moderation/report/";
    private final String INITIALIZE_USER_PATH = "/security/create/";
    private final String CREATE_LIVE_THREAD_PATH = "/live/upload/"; //does not change
    private final String GET_LIVE_THREAD_LIST = "/live/get/"; //does not change
    private final String REPLY_LIVE_THREAD_PATH = "/reply/upload/"; //does not change
    private final String GET_LIVE_THREAD_REPLIES = "/reply/get/"; //does not change

    /**
     * S3 INFO
     */
    private final String BUCKET_NAME = "launch-zone";


    private Tracker mTracker;

    private static String serverAddress = "jokrbackend.ddns.net"; //changes when resolved
    private static UUID userID;

    /**
     * RESPONSE CODES
     */
    private final int RESPONSE_SUCCESS = 200;
    private final int RESPONSE_UNPROCESSABLE_ENTITY = 422;
    private final int RESPONSE_TOO_MANY_REQUESTS = 429;
    private final int RESPONSE_BLOCK_CONFLICT = 409;
    private final int RESPONSE_UNAUTHORIZED = 401;
    private final int RESPONSE_NOT_FOUND = 404;

    /**
     * JSON TAGS
     */
    private final String FROM_USER = "from";
    private final String USER_ID = "id";
    private final String THREAD_ID = "threadID";
    private final String TO = "to";
    private final String IMAGE_URL = "url";
    private final String TITLE = "title";
    private final String TEXT = "text";
    private final String NAME = "name";

    /**
     * DATA HANDLING
     */
    private static Location mLocation;

    private static ArrayList<String> imagesSeen = new ArrayList<>();
    private static final String IMAGESSEEN_KEY = "images";
    private final String ISFIRSTRUN_KEY = "firstrun";
    private final String UUID_KEY = "uuidkey";

    private static final int OLDEST_ALLOWED_IMAGE = 3600 * 1000; // two hours

    /**
     *  AWS (AMAZON WEB SERVICE S3)
     */
    static TransferUtility transferUtility;
    HashMap<Integer,Bundle> pendingMap = new HashMap<>();
    private final String PENDING_TRANSFER_TYPE = "what";


    /**
     * method 'onCreate'
     * <p/>
     * lifeCycle method, called when the service is created, runs in the main process
     */
    @Override
    public void onCreate() {
        if (VERBOSE)
            Log.v(TAG, "entering onCreate...");

        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
        boolean isFirstRun = settings.getBoolean(ISFIRSTRUN_KEY, true);

        if (isFirstRun) {
            Log.d(TAG, "the app is opening for the first time, generating a user ID");
          /*  SharedPreferences.Editor editor = settings.edit();
            editor.putString(UUID_KEY, userID.toString());
            editor.putBoolean(ISFIRSTRUN_KEY, false);
            editor.apply();*/

            try {
                mMessenger.send(Message.obtain(null, new initializeUserWithServer()));
            } catch (RemoteException e) {
                Log.e(TAG, "initializing user with server failed...", e);
            }

        } else {
            Log.d(TAG, "loading userID from storage...");
            userID = UUID.fromString(settings.getString(UUID_KEY, null));
            sendSignOnEvent(userID.toString());
        }

        if (!ALLOW_DUPLICATES) {


            if (VERBOSE)
                Log.v(TAG, "Duplicates are not allowed, loading imagesSeen");

            Set<String> loaded = settings.getStringSet(IMAGESSEEN_KEY, null);

            if (loaded == null) {
                Log.d(TAG, "loaded set was null, exiting onCreate...");
                return;
            }

            imagesSeen = new ArrayList<>(loaded);
            if (VERBOSE) {
                Log.v(TAG, "imagesSeen array loaded from SharedPreferences, printing...");
                for (String i : imagesSeen) { //print all sharedpreferences entry
                    Log.v(TAG, i);
                }
            }

            //todo this should use a more secure system involving amazon cognito
            initializeTransferUtility();



        } else {
            if (VERBOSE)
                Log.v(TAG, "Duplicates are allowed, not loaded imagesSeen");
            imagesSeen = new ArrayList<>();
        }

        if (VERBOSE)
            Log.v(TAG, "exiting onCreate...");
    }


    //@Override
    //public int onStartCommand(Intent intent, int flags, int startId) {
    //    Log.i("LocalService", "Received start id " + startId + ": " + intent);
    //    // We want this service to continue running until it is explicitly
    //    // stopped, so return sticky.
    //    return START_STICKY;
    // }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) {
            Log.v(TAG, "entering onDestroy...");
            Log.v(TAG, "saving ImagesSeen to sharedPreferences, size: " + imagesSeen.size());
        }


        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);

        settings.edit().putStringSet(IMAGESSEEN_KEY, new HashSet<>(imagesSeen)).apply();

        Log.v(TAG, "exiting onDestroy...");
    }

    public void initializeTransferUtility() {
        if (VERBOSE) Log.v(TAG,"entering initializeTransferUtility...");

        transferUtility =
                new TransferUtility(
                        new AmazonS3Client(
                                new BasicAWSCredentials(
                                        "AKIAIZ42NH277ZC764XQ",
                                        "pMYCGMq+boy6858OfITL4CTXWgdkVbVreyROHckG"
                                )
                        ),getApplicationContext()
                );

        if (VERBOSE) Log.v(TAG,"exiting initializeTransferUtility...");
    }

    public class initializeUserWithServer implements Runnable {

        private Thread currentThread;

        @Override
        public void run() {
            if (VERBOSE) {
                Log.v(TAG, "enter InitializeUser...");
            }
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            currentThread = Thread.currentThread();

            try {

                HttpURLConnection conn = (HttpURLConnection) new URL(CONNECTION_PROTOCOL, serverAddress, SERVER_SOCKET, INITIALIZE_USER_PATH).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-Client-UserID", "");
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

                if (VERBOSE) {
                    LogUtils.printMapToVerbose(asMap, TAG);
                }

                userID = UUID.fromString(String.valueOf(asMap.get("id")));

                SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(UUID_KEY, userID.toString());
                editor.putBoolean(ISFIRSTRUN_KEY, false);
                editor.apply();
            } catch (Exception e) {
                Log.d(TAG, "Error executing initialize user...", e);
                //Toast.makeText(getApplicationContext(),"Error initializing new user with the server...",Toast.LENGTH_SHORT).show();
            }

            if (userID == null) {
                Log.e(TAG, "userID was not successfully retreived... trying again...");
                return;
            }
            sendSignOnEvent(userID.toString());

            if (VERBOSE) {
                Log.v(TAG, "exiting InitializeUser...");
            }
        }

    }


    private void createLocalPost(Bundle b) {
        createLocalPost(b.getString(Constants.KEY_S3_KEY), mLocation, b.getString(Constants.KEY_TEXT, ""));
    }

    /***********************************************************************************************
     *
     *   IMAGE SENDING
     *
     */
    /**
     * Handle action send images in the provided background thread with the provided
     * parameters.
     * <p/>
     * the client sends the latitude and longitude stored in the location passed in the arguments
     * and the image stored at the filepath provided
     * the sending process uses URLConnection to create the connection
     * the apache commons library and jackson's json parser are both utilized
     * <p/>
     * the server marks the time the image is received, and gives the image an ID
     *
     * @param imageKey file name of the location of the image to send
     * @param location  current location of the device
     */
    private void createLocalPost(final String imageKey,
                                 final Location location,
                                 final String text) {
        Log.d(TAG, "entering handleActionSendImages...");

        new Thread(new Runnable() {
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);


                if (location == null) {
                    Log.e(TAG, "location was null, not sending image");
                    return;
                }
                Log.d(TAG, "grabbing stored lat and long...");
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                if (VERBOSE) {
                    Log.v(TAG, "grabbing stored lat and long...");
                    Log.d(TAG, "Latitude: " + lat + " Longitude: " + lon);
                }

               // String image = loadImageForTransit(imageData);
                //todo not sure which method is best to read from image to byte array
                //read the image, send to server

                int status = -1; //status of the image sent

                HttpURLConnection conn;
                try {
                    conn = connectToServer(UPLOAD_LOCAL_POST_PATH);
                } catch (ConnectException e) {
                    Log.e(TAG,"failed to connect to the server... quitting...");
                    throw new RuntimeException();
                }


                try {

                    JsonFactory jsonFactory = new JsonFactory();
                    JsonGenerator jGen = jsonFactory.
                            createGenerator(conn.getOutputStream()); //tcp connection to server

                    jGen.writeStartObject();
                    jGen.writeNumberField("latitude", lat);
                    jGen.writeNumberField("longitude", lon);
                    jGen.writeStringField(Constants.KEY_TEXT, text);
                    jGen.writeStringField(IMAGE_URL, imageKey);
                    jGen.writeEndObject();
                    jGen.flush();
                    jGen.close();

                    handleResponseCode(conn.getResponseCode(), replyMessenger);

                    deleteFile(imageKey.substring(imageKey.lastIndexOf("/") + 1, imageKey.length()));
            /*if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "server returned successful response" + responseCode);
                Log.d(TAG, "now deleting image from internal storage...");
                rMessenger.send(Message.obtain(null, MainActivity.MSG_SUCCESS_LOCAL));

            }*/

                } catch (IOException e) {
                    Log.d(TAG, "IOException");
                    Log.e(TAG, "Error sending image to server", e);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending image to server", e);
                }

                /**
                 * Report status back to the main thread
                 */

                Log.d(TAG, "exiting handleActionSendImages...");

            }
        }).start();

    }

    /***********************************************************************************************
     *
     *   IMAGE RECEIVING
     *
     */
    /**
     * method 'requestLocalPosts'
     * <p/>
     * requests images from the server
     * <p/>
     * first sends - the current client location, the count desired
     * and finally an array of images already seen.
     * <p/>
     * the client then waits for a reply of images, saving each one
     * - the reply comes in the form of a JSONArray, with position 0 as the count
     * of images incoming. Each image and associated data is its own json objec
     * the array is decoded, and the objects are saved to separate files in the internal storage
     * based on ID of the image.
     *
     * @param numberOfImages image count to request
     * @param location       current location of the client
     */
    private void requestLocalPosts(int numberOfImages, Location location) {
        Log.d(TAG, "entering requestImages...");

        if (mLocation == null) {
            Log.e(TAG, "Location was null in requestImages... canceling...");
            return;
            //todo readd request to message queue
        }

        if (numberOfImages == 0) {
            Log.e(TAG, "zero images requested, exiting");
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        Log.d(TAG, "requesting " + numberOfImages + " images, with lat " + lat + " and lng " + lon);

        int responseCode = 0;


        HttpURLConnection conn;
        try {
            conn = connectToServer(GET_LOCAL_POST_PATH);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }

        try {
            //continuing...
            Log.d(TAG, "now writing with JacksonsJSON");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());

            jGen.writeStartObject();
            jGen.writeNumberField("latitude", lat);
            jGen.writeNumberField("longitude", lon);
            jGen.writeNumberField("count", numberOfImages);


            String[] imageSeenArray = new String[imagesSeen.size()];
            imagesSeen.toArray(imageSeenArray);
            jGen.writeFieldName("seen");
            jGen.writeStartArray(imagesSeen.size());
            if (imagesSeen != null) {
                for (int i = 0; i < imagesSeen.size(); i++) {
                    jGen.writeNumber(imageSeenArray[i]);
                }
            }
            jGen.writeEndArray();

            jGen.writeEndObject();
            jGen.flush();
            jGen.close(); //closes the conn.getOutputStream as well

            try {
                saveIncomingJsonArray(MSG_REQUEST_LOCAL_POSTS, conn, null);
                handleResponseCode(conn.getResponseCode(), replyMessenger);
            } catch (IOException e) {
                Log.e(TAG,"error saving incoming json...");
                //todo handle this
            }

            //awesome, now get the final respnose
            responseCode = conn.getResponseCode();
            conn.disconnect();

            Log.d(TAG, "server returned successful response");
            if (responseCode == HttpURLConnection.HTTP_OK) {
            } else {
                Log.w(TAG, "sending failed, response: " + responseCode);
                //todo Notify UI thread
            }


        } catch (IOException e) {
            Log.d(TAG, "IOException");
            Log.d(TAG, "response code " + responseCode);

            Log.e(TAG, "error requesting images", e);
        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(),"Exception! response code: " + responseCode,Toast.LENGTH_LONG).show();
            Log.d(TAG, "response code " + responseCode);
            Log.e(TAG, "Error sending image to server", e);
        }

        Log.d(TAG, "exiting requestImages...");
    }

    private void requestLocalPostsAsync(final int numberOfImages, final Location location) {
        if (isLocalRequesting) {
            if (VERBOSE)
                Log.v(TAG, "requestLocal is already currently requesting from the server, canceling...");
            return;
        }
        if (VERBOSE) Log.v(TAG, "requestLocal is not currently requesting... starting...");
        isLocalRequesting = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                requestLocalPosts(numberOfImages, location);
                isLocalRequesting = false;
            }
        }).start();
    }


    /**
     * method 'sendImageMessage'
     * <p/>
     * sends a message to a specific user specified by UUID
     *
     * @param filePath      the location the image is stored which is to be sent
     * @param messageTarget the UUID of the user to send the message to
     */
    private void sendImageMessage(String filePath, String messageTarget) {
        if (VERBOSE) {
            Log.v(TAG, "entering sendImageMessage...");
        }

        if (VERBOSE) Log.v(TAG, "creating url...");


        HttpURLConnection conn;
        try {
            conn = connectToServer(SEND_LOCAL_MESSAGE_PATH);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }


        int responseCode = -1;

        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            if (VERBOSE) {
                Log.v(TAG, "sending Image message:");
                Log.v(TAG, "to : " + messageTarget);
                Log.v(TAG, "filePath : " + filePath);
            }

            jGen.writeStartObject();
            jGen.writeStringField(TO, messageTarget);
            jGen.writeStringField(IMAGE_URL, filePath);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }


        if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "server returned successful response" + responseCode);
            Log.d(TAG, "now deleting image from internal storage...");
            deleteFile(filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length()));
        } else {
        }
    }

    public void sendImageMessageAsync(Bundle b) {
        sendImageMessageAsync(b.getString(Constants.KEY_S3_KEY), b.getString(Constants.MESSAGE_TARGET));
    }

    public void sendImageMessageAsync(final String filePath, final String messageTarget) {
        if (VERBOSE) {
            Log.v(TAG,"entering sendImageMessageAsync with: " + filePath + ", " + messageTarget);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                sendImageMessage(filePath, messageTarget);
            }
        }).start();
    }

    /**
     * method 'requestLocalMessages'
     * <p/>
     * sends the current UUID to the server, querying if any new messages are available.
     * <p/>
     * all incoming images are saved to the contentprovider's "message_entries" table
     */
    private void requestLocalMessages() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLocalMessages...");
        }

        HttpURLConnection conn;
        try {
            conn = connectToServer(GET_LOCAL_MESSAGES_PATH);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }



        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        }

        try {
            saveIncomingJsonArray(MSG_REQUEST_MESSAGES, conn, null);
        } catch (IOException e) {
            Log.e(TAG, "error receiving and storing messages...", e);
        }

        try {
            handleResponseCode(conn.getResponseCode(), replyMessenger);
        } catch (IOException e) {
            Log.e(TAG, "error handling responsecode...", e);
        }

        if (VERBOSE) Log.v(TAG, "exiting requestLocalMessages...");
    }

    public void requestLocalMessagesAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestLocalMessages();
            }
        }).start();
    }

    /**
     * method 'blockLocalUser'
     * <p/>
     * sends a request to the server, to add a user to this client's blocklist
     *
     * @param userToBlock stringified UUID of the user to block
     */
    public void blockLocalUser(String userToBlock) {
        if (VERBOSE) {
            Log.v(TAG, "enter blockLocalUser...");
        }
        Log.d(TAG, "Received a request to block user " + userToBlock);


        HttpURLConnection conn;
        try {
            conn = connectToServer(BLOCK_LOCAL_USER_PATH);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }


        int responseCode;
        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "from: " + userID.toString());
                Log.v(TAG, "blocking: " + userToBlock);
            }
            jGen.writeStartObject();
            jGen.writeStringField("block", userToBlock);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        }


        if (VERBOSE) {
            Log.v(TAG, "exiting blockLocalUser...");
        }
    }

    public void blockLocalUserAsync(final String userToBlock) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                blockLocalUser(userToBlock);
            }
        }).start();
    }

    /**
     * method 'createLiveThread'
     * <p/>
     * opens a connection to the server, and sends the information required to create a thread in
     * live
     *
     * @param name        of the poster
     * @param title       the title of the thread
     * @param description the description of the thread
     * @param filePath    the path of the thread image
     */
    private void createLiveThread(String name, String title, String description, String filePath) {
        if (VERBOSE) {
            Log.v(TAG, "entering createLiveThread...");
        }


        HttpURLConnection conn;
        try {
            conn = connectToServer(CREATE_LIVE_THREAD_PATH);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }

        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            if (VERBOSE) {
                Log.v(TAG, "sending thread to server:");
                Log.v(TAG, "from : " + userID.toString());
                Log.v(TAG, "title : " + title);
                Log.v(TAG, "name : " + name);
                Log.v(TAG, "text : " + description);
                Log.v(TAG, "filePath : " + filePath);
            }

            jGen.writeStartObject();
            jGen.writeStringField(TITLE, title);
            jGen.writeStringField(NAME, name);
            jGen.writeStringField(TEXT, description);
            jGen.writeStringField(IMAGE_URL, filePath);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            handleResponseCode(conn.getResponseCode(), replyMessenger);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
        deleteFile(filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length()));
    }
    private void createLiveThreadAsync(final Bundle b) {
        if (VERBOSE) {
            Log.v(TAG, "entering createLiveThreadAsync...");
            com.jokrapp.android.util.LogUtils.printBundle(b, TAG);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                createLiveThread(
                        b.getString("name"),
                        b.getString("title"),
                        b.getString("description"),
                        b.getString(Constants.KEY_S3_KEY)
                );
            }
        }).start();

        if (VERBOSE) Log.v(TAG,"exiting createLiveThreadAsync...");
    }

    private void createLiveThreadAsync(final String name, final String title, final String description, final String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                createLiveThread(name, title, description, filePath);
            }
        }).start();
    }


    /**
     * method 'replyToLiveThread'
     * <p/>
     * opens a connection to the server, and sends the information to reply to a thread in live
     *
     * @param threadID    id of the thread to reply to
     * @param name        of the poster
     * @param description the description of the thread
     * @param filePath    the path of the thread image
     */
    private void replyToLiveThread(int threadID, String name, String description, String filePath) {
        if (VERBOSE) Log.v(TAG, "creating live thread...");

        String image = null;
        if (filePath != null) {
            image = loadImageForTransit(filePath);
            if (image == null) {
                Log.e(TAG, "image failed to load for transit...");
                return;
            }
        }

        HttpURLConnection conn;
        try {
            conn = connectToServer(REPLY_LIVE_THREAD_PATH);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }

        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            if (VERBOSE) {
                Log.v(TAG, "sending thread to server:");
                Log.v(TAG, "from : " + userID.toString());
                Log.v(TAG, "thread number : " + threadID);
                Log.v(TAG, "name : " + name);
                Log.v(TAG, "text : " + description);
                Log.v(TAG, "filePath : " + filePath);
            }

            jGen.writeStartObject();
            jGen.writeNumberField(THREAD_ID, threadID);
            jGen.writeStringField(NAME, name);
            jGen.writeStringField(TEXT, description);
            jGen.writeStringField("url", image);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            handleResponseCode(conn.getResponseCode(), replyMessenger);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }

        if (filePath != null) {
            deleteFile(filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length()));
        }
    }


    private void replyToLiveThreadAsync(final int threadID, final String name, final String description, final String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                replyToLiveThread(threadID, name, description, filePath);
            }
        }).start();
    }


    /**
     * method 'requestLocalMessages'
     * <p/>
     * requests the current thread list from the server
     */
    private void requestLiveThreads() {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveThreads...");
        }

        HttpURLConnection conn;
        try {
            conn = connectToServer(GET_LIVE_THREAD_LIST);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }


        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "from : " + userID.toString());
            }

            jGen.writeStartObject();
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        }

        try {
            saveIncomingJsonArray(MSG_REQUEST_LIVE_THREADS, conn, null);
        } catch (IOException e) {
            Log.d(TAG, "error receiving and storing messages...", e);
        }

        try {
            handleResponseCode(conn.getResponseCode(), replyMessenger);
        } catch (IOException e) {
            Log.e(TAG, "error handling responsecode...", e);
        }

        if (Constants.LOGD)
            Log.d(TAG, "Notifying client that thread list has been requested loading...");
        try {
            replyMessenger.send(Message.obtain(null, MainActivity.MSG_LIVE_REFRESH_DONE));
        } catch (RemoteException e) {
            Log.e(TAG, "error notifying client...", e);
        }


        if (VERBOSE) Log.v(TAG, "exiting requestLiveThreads...");
    }

    private void requestLiveThreadsAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestLiveThreads();
            }
        }).start();
    }

    /**
     * method 'requestLiveReplies'
     * <p/>
     * requests the current reply list from the server
     */
    private void requestLiveReplies(int threadID) {
        if (VERBOSE) {
            Log.v(TAG, "enter requestLiveReplies...");
        }

        HttpURLConnection conn;
        try {
            conn = connectToServer(GET_LIVE_THREAD_REPLIES);
        } catch (ConnectException e) {
            Log.e(TAG,"failed to connect to the server... quitting...");
            throw new RuntimeException();
        }

        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "thread id: " + threadID);
            }

            jGen.writeStartObject();
            jGen.writeNumberField(THREAD_ID, threadID);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        }

        /**pass thread id**/
        Bundle b = new Bundle();
        b.putString(THREAD_ID, String.valueOf(threadID));
        try {
            saveIncomingJsonArray(MSG_REQUEST_REPLIES, conn, b);
        } catch (IOException e) {
            Log.e(TAG, "error receiving and storing messages...", e);
        }

        try {
            handleResponseCode(conn.getResponseCode(), replyMessenger);
        } catch (IOException e) {
            Log.e(TAG, "error handling responsecode...", e);
        }

        if (VERBOSE) Log.v(TAG, "exiting requestLiveReplies...");
    }

    private void requestLiveRepliesAsync(final int threadID) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestLiveReplies(threadID);
            }
        }).start();
    }

    private void saveIncomingJsonArray(int where, URLConnection conn, Bundle extradata) throws IOException {
        if (VERBOSE) Log.v(TAG,"entering saveIncomingJsonArray...");


        JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());
        ObjectMapper objectMapper = new ObjectMapper();

        ArrayList<LinkedHashMap<String,Object>>jsonArray =
                objectMapper.readValue(jParser,ArrayList.class);

        if (!jsonArray.isEmpty()) {
            LinkedHashMap<String,Object> map;

            if (VERBOSE)
                Log.v(TAG, "printing verbose output...");

            for (int i = 0; i < jsonArray.size(); i++) {
                map = jsonArray.get(i);

                /*
                 TAMPERING
                  - this is where we tamper with the incoming data based on where it is intended
                 */


                Bundle b = new Bundle();
                switch (where) {

                    case MSG_REQUEST_REPLIES:
                        if (VERBOSE) Log.v(TAG,"saving json from replies");

                        map.put(SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID, extradata.getString(THREAD_ID));
                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_REPLIES_DIRECTORY);
                        b.putInt(PENDING_TRANSFER_TYPE, MSG_REQUEST_REPLIES);
                    case MSG_REQUEST_LOCAL_POSTS:
                        if (VERBOSE) Log.v(TAG,"saving json from local/");

                        //swap id for _ID, to allow listview loading, and add the thread ID
                        map.put(SQLiteDbContract.LiveReplies.COLUMN_ID, map.remove("id"));

                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LOCAL_DIRECTORY);
                        break;
                    case MSG_REQUEST_LIVE_THREADS:
                        if (VERBOSE) Log.v(TAG,"saving json from live");

                        map.put(SQLiteDbContract.LiveThreadEntry.COLUMN_ID, map.remove("order"));
                        map.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_THREAD_ID, map.remove("id"));
                        b.putInt(PENDING_TRANSFER_TYPE, MSG_REQUEST_LIVE_THREADS);
                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LIVE_DIRECTORY);
                        break;

                    case MSG_REQUEST_MESSAGES:
                        if (VERBOSE) Log.v(TAG,"saving json from message");
                        map.put(SQLiteDbContract.MessageEntry.COLUMN_ID, map.remove("id"));
                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_MESSAGE_DIRECTORY);
                        b.putInt(PENDING_TRANSFER_TYPE, MSG_REQUEST_MESSAGES);
                        break;


                }
                if (map.containsKey("url")) {

                    String cont = (String)map.get("url");
                    if (VERBOSE)Log.d(TAG,"url is: " + cont);

                    if (!cont.equals("")) {
                        if (cont.contains("http://")) {
                            map.put("url",downloadImageFromURL(cont));
                            Log.i(TAG,"this was a url, and not from AWS... lets just get it");
                        } else {
                            //b.putString("url", cont);
                            //b.putString(Constants.KEY_S3_KEY, cont);
                            //downloadImageFromS3(b);
                        }
                    }
                }

                if (VERBOSE) {
                    LogUtils.printMapToVerbose(map, TAG);
                }

                //parcels map and reads as contentValues set, then passes to the content provider for insertion
                android.os.Parcel myParcel = android.os.Parcel.obtain();
                myParcel.writeMap(map);
                myParcel.setDataPosition(0);
                android.content.ContentValues values = android.content.ContentValues.CREATOR.createFromParcel(myParcel);



                switch (where) {

                    case MSG_REQUEST_REPLIES:
                        //swap id for _ID, to allow listview loading, and add the thread ID

                        getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_REPLY_THREAD_LIST, values);
                        break;

                    case MSG_REQUEST_LIVE_THREADS:
                        getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LIVE, values);
                        break;

                    case MSG_REQUEST_LOCAL_POSTS:
                        getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LOCAL, values);
                        break;

                    case MSG_REQUEST_MESSAGES:
                        getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_MESSAGE, values);
                        break;

                }
            }
        } else {
            Log.v(TAG, "nothing was supplied...");
        }

        jParser.close();

        if (VERBOSE) Log.v(TAG,"exiting saveIncomingJsonArray...");
    }


    /***********************************************************************************************
     * SERVICE - ACTIVITY COMMUNICATION
     */

    class ServiceHandlerThread extends HandlerThread {
        private IncomingHandler incomingHandler;

        ServiceHandlerThread(DataHandlingService parent) {
            super("ServiceHandlerThread");
            start();
            incomingHandler = new IncomingHandler(getLooper()).setParent(parent);
        }

        public IncomingHandler getIncomingHandler() {
            return incomingHandler;
        }
    }

    private Messenger replyMessenger;

    public void setReplyMessenger(Messenger messenger) {
        replyMessenger = messenger;
    }

    static final int MSG_BUILD_CLIENT = 1;

    static final int MSG_CONNECT_CLIENT = 2;

    static final int MSG_DISCONNECT_CLIENT = 3;

    static final int MSG_REQUEST_CONSTANT_UPDATES = 4;

    static final int MSG_SEND_IMAGE = 5;
    static final int MSG_SEND_MESSAGE = 19;

    static final int MSG_REQUEST_LOCAL_POSTS = 6;

    static final int MSG_RESOLVE_HOST = 7;

    static final int MSG_DELETE_IMAGE = 8;

    static final int MSG_REQUEST_MESSAGES = 9;

    static final int MSG_BLOCK_USER = 10;

    static final int MSG_CREATE_THREAD = 11;

    static final int MSG_REPLY_TO_THREAD = 12;

    static final int MSG_REQUEST_LIVE_THREADS = 13;

    static final int MSG_REQUEST_REPLIES = 15;

    static final int MSG_SET_CALLBACK_MESSENGER = 16;

    static final int MSG_DOWNLOAD_IMAGE = 18;

    /**
     * class 'IncomingHandler'
     * <p/>
     * Handler of incoming messages from clients.
     * location updates are managed in the current thread, sending and receiving images
     * is handled in a new thread
     * <p/>
     * references the parent service (weakly) in order to prevent memory leaks
     */
    class IncomingHandler extends Handler {
        WeakReference<DataHandlingService> irs;

        public IncomingHandler setParent(DataHandlingService parent) {
            irs = new WeakReference<>(parent);
            return this;
        }

        public IncomingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "enter handleMessage");
            Bundle data;

            /**
             * ANALYTICS REPORTING SWITCH, if message is greater than analytics switch, pass and exit
             */
            if (msg.what >= Constants.ANALYTICS) {
                if (Constants.LOGD) Log.d(TAG, "Received analytics event, passing...");
                reportAnalyticsEvent(msg.what, msg.getData().getString("name"));
                return;
            }

            switch (msg.what) {
                case MSG_BUILD_CLIENT:
                    irs.get().buildGoogleApiClient();
                    break;

                case MSG_CONNECT_CLIENT:
                    Log.d(TAG, "connecting client");
                    if (mGoogleApiClient == null) {
                        irs.get().buildGoogleApiClient();
                    }
                    mGoogleApiClient.connect();
                    break;

                case MSG_DISCONNECT_CLIENT:
                    mGoogleApiClient.disconnect();
                    break;

                case MSG_REQUEST_CONSTANT_UPDATES:
                    irs.get().createLocationRequest(msg.arg1, msg.arg2);
                    break;

                case MSG_SEND_IMAGE:
                    Log.d(TAG, "request to send an image received");

                    //store the type of request this is in the bundle object
                    data = msg.getData();

                    String messageTarget = data.getString(Constants.MESSAGE_TARGET);
                    //Log.d(TAG, "image is stored at " + filePath);
                    if (messageTarget == null) {
                        Log.d(TAG, "posting to local...");
                        data.putInt(PENDING_TRANSFER_TYPE, MSG_SEND_IMAGE);
                        data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LOCAL_DIRECTORY);
                        //irs.get().createLocalPost(filePath, mLocation);
                    } else {
                        Log.d(TAG, "sending Message to user: " + messageTarget);
                        data.putInt(PENDING_TRANSFER_TYPE, MSG_SEND_MESSAGE);
                        data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_MESSAGE_DIRECTORY);
                        //irs.get().sendImageMessageAsync(filePath, messageTarget, msg.arg1);
                    }

                    uploadImageToS3(data);
                    break;

                case MSG_REQUEST_LOCAL_POSTS:
                    Log.d(TAG, "received a message to request images.");

                    int count = msg.getData().getInt(Constants.IMAGE_COUNT);
                    irs.get().requestLocalPostsAsync(count, mLocation);

                    break;

                case MSG_RESOLVE_HOST:
                    Log.d(TAG, "request to resolve host received");
                    String address = msg.getData().getString("hostname");

                    if (VERBOSE) {
                        Log.v(TAG, "attempting to resolve: " + address);
                    }

                    try {
                        InetAddress addr = InetAddress.getByName(address);
                        serverAddress = address;
                        Log.i(TAG, "Resolved address is: " + addr.getHostAddress());
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "DNS resolution failed", e);
                    }

                    break;

                case MSG_DELETE_IMAGE:
                    Log.d(TAG, "received a message to delete image from database");

                    String s = msg.getData().getString(Constants.IMAGE_FILEPATH);
                    throw new NotYetConnectedException();

                case MSG_REQUEST_MESSAGES:
                    Log.d(TAG, "received a message to message a user");
                    irs.get().requestLocalMessagesAsync();
                    break;

                case MSG_BLOCK_USER:
                    Log.d(TAG, "received a message to block a user");
                    irs.get().blockLocalUserAsync(msg.getData().getString(Constants.MESSAGE_TARGET));
                    break;

                case MSG_CREATE_THREAD:
                    Log.d(TAG, "received a message to create a thread");

                    data = msg.getData();
                    data.putInt(PENDING_TRANSFER_TYPE, MSG_CREATE_THREAD);
                    data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LIVE_DIRECTORY);
                    uploadImageToS3(data);
                    break;

                case MSG_REPLY_TO_THREAD:
                    Log.d(TAG, "received a message to create a thread");

                    data = msg.getData();
                    if (data.containsKey(Constants.KEY_S3_KEY)){
                        if (Constants.LOGD) Log.d(TAG,"contained an image filepath," +
                                "uploading the image there to s3 first...");

                        data.putInt(PENDING_TRANSFER_TYPE, MSG_REPLY_TO_THREAD);
                        data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_REPLIES_DIRECTORY);
                        uploadImageToS3(data);
                    } else {
                        if (Constants.LOGD) Log.d(TAG,"no image filepath was provided," +
                                " this must be a text reply, so uploading straight to the server.");
                        irs.get().replyToLiveThreadAsync(msg.arg1,
                                data.getString("name"),
                                data.getString("description"),
                                data.getString("filePath")
                        );
                    }

                    break;

                case MSG_REQUEST_LIVE_THREADS:
                    Log.d(TAG, "received a message to request the thread list");
                    irs.get().requestLiveThreadsAsync();
                    break;

                case MSG_REQUEST_REPLIES:
                    Log.d(TAG, "received a message to request replies.");
                    requestLiveRepliesAsync(msg.arg1);

                    break;

                case MSG_SET_CALLBACK_MESSENGER:
                    Log.d(TAG, "setting callback messenger...");
                    irs.get().setReplyMessenger(msg.replyTo);
                    break;

                case MSG_DOWNLOAD_IMAGE:
                    Log.d(TAG, "received a message to download an image...");

                    data = msg.getData();
                    data.putInt(PENDING_TRANSFER_TYPE, msg.what);
                    downloadImageFromS3(data);

                    break;

                default:
                    super.handleMessage(msg);
            }


            Log.d(TAG, "exit handleMessage...");
        }
    }


    //final Messenger mMessenger = new Messenger(new IncomingHandler().setParent(this));
    final Messenger mMessenger = new Messenger(new ServiceHandlerThread(this).getIncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "client is binding to the Service");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "client is unbinding from the Service");
        replyMessenger = null;
        return super.onUnbind(intent);
    }

    /**********************************************************************************************
     * Helper Methods
     * <p/>
     * these methods are created to perform common tasks throughout the service
     */
    /**
     * method 'loadImageForTransit'
     * <p/>
     * loads bitmap at selected filePath from storage, gzips and base64's it, so that it may be
     * sent as part of a JSON object
     *
     * @param filePath location of the image to load
     * @return processed image ready to be sent to the server
     */
    public String loadImageForTransit(String filePath) {
        if (VERBOSE) {
            Log.v(TAG, "loading image for transit from filePath " + filePath);
        }
        String image = null;
        try {
            //create the data into an input stream
            InputStream is = new FileInputStream(filePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GzipCompressorOutputStream gos = new GzipCompressorOutputStream(bos);


            byte[] readData = IOUtils.toByteArray(is);

            gos.write(readData);
            gos.flush();
            gos.close();

            image = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);

        } catch (IOException e) {
            Log.e(TAG, "file was not found", e);
        }
        return image;
    }

    /**
     * method 'saveIncomingImage'
     *
     * @param fileName file name for the image to be saved to
     * @param theImage the base64'd stringified image
     * @return whether or not the save was successful
     */
    public boolean saveIncomingImage(String fileName, String theImage) {
        if (VERBOSE) Log.v(TAG, "Saving incoming with file name: " + fileName);

        try {
            FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            if (VERBOSE) {
                Log.v(TAG, "saving image to file " + fos.getFD());
            }

            byte[] data = theImage.getBytes();
            ByteArrayInputStream byis = new ByteArrayInputStream(data);
            Base64InputStream bos = new Base64InputStream(byis, Base64.DEFAULT);
            GzipCompressorInputStream gzis = new GzipCompressorInputStream(bos);
            IOUtils.copy(gzis, fos);
            fos.flush();
            fos.close();

            if (VERBOSE) Log.v(TAG, "validating image...");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);
            if (options.outWidth == -1 && options.outHeight == -1) {
                Log.e(TAG, "incoming image was not valid...");
                deleteFile(fileName);
                return false;
            }

        } catch (IOException e) {
            Log.e(TAG, "error saving incoming image to internal storage...", e);
            return false;

        }

        if (VERBOSE) Log.v(TAG, "image successfully saved");
        return true;
    }

    /**
     * method 'saveLiveImage'
     */
    private boolean saveLiveImage(int imageID, String theImage) {
        return saveIncomingImage(Uri.withAppendedPath(FireFlyContentProvider
                .CONTENT_URI_LIVE, String.valueOf(imageID)), theImage);
    }


    /**
     * method 'saveIncomingImage'
     *
     * @param destination the location uri for the image to be stored
     * @param theImage    the base64'd stringified image
     * @return whether or not the save was successful
     */
    public boolean saveIncomingImage(Uri destination, String theImage) {
        if (VERBOSE) Log.v(TAG, "Saving incoming to uri" + destination.toString());

        try {
            FileOutputStream fos = (FileOutputStream) getContentResolver()
                    .openOutputStream(destination, "w");
            if (VERBOSE) {
                Log.v(TAG, "saving image to file " + fos.getFD());
            }

            byte[] data = theImage.getBytes();
            ByteArrayInputStream byis = new ByteArrayInputStream(data);
            Base64InputStream bos = new Base64InputStream(byis, Base64.DEFAULT);
            GzipCompressorInputStream gzis = new GzipCompressorInputStream(bos);
            IOUtils.copy(gzis, fos);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            Log.e(TAG, "error saving incoming image to internal storage...", e);
            return false;

        }

        if (VERBOSE) Log.v(TAG, "image successfully saved");
        return true;
    }

    /**
     * method 'openConnectionToURL'
     * <p/>
     * opens and sets the parameters to the given URL
     *
     * @param url the url to connect to
     * @return the prepared URL for input and output, or null if an error occurred
     */
    private HttpURLConnection openConnectionToURL(URL url) throws IOException {

        URLConnection urlconn;

        urlconn = url.openConnection();


        if (VERBOSE) Log.v(TAG, "connection opened to " + urlconn.toString());

        HttpURLConnection conn = (HttpURLConnection) urlconn;
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);


        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Client-UserID", userID.toString());
        conn.setUseCaches(false);


        return conn;
    }


    private HttpURLConnection connectToServer(String serverPath) throws ConnectException {
        return connectToServer(serverPath,0);
    }


    /**
     * method 'connectToServer'
     *
     * attempts to open up a connection to the server with the provided path
     *
     * @param serverPath the path to connect to
     * @return the open connection, ready for JSON transmission
     */
    private HttpURLConnection connectToServer(String serverPath, int attempt) throws ConnectException {
        if (VERBOSE) {
            Log.v(TAG,"attempting to connect to: " + serverPath + " attempt " + attempt);
        }

        final int MAX_CONNECTION_ATTEMPTS = 5;

        attempt = attempt + 1;

        HttpURLConnection conn;

        URL url;
        try {
            url = createURLtoServer(serverPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException();
        }

        try {
            conn = openConnectionToURL(url);
        } catch (IOException e) {
            Log.e(TAG,"attempt failed, attempt number: " + attempt,e);
            if (attempt <= MAX_CONNECTION_ATTEMPTS) {
                Double delay = (Math.pow(attempt, 2) * 1000);

                Log.w(TAG," waiting " + delay + " milliseconds before retry...");
                try {
                    Thread.sleep(delay.longValue());
                } catch (InterruptedException es) {
                    throw new NetworkOnMainThreadException();
                }

                return connectToServer(serverPath, attempt + 1);
            } else {
                Log.e(TAG, "attempt count exceeded maximum allowed: " + attempt + " cancelling.");
                throw new ConnectException();
            }


        }

        if (VERBOSE) {
            Log.v(TAG,"connection opened successfully :-)");
        }
        return conn;
    }


    /**
     * method "createURLtoServer"
     * <p/>
     * creates a URL to the server with the provided directory
     * this is used because all the urls have the same protocol address and socket
     *
     * @param path the serverDirectory path to point to
     * @return the created URL
     * @throws MalformedURLException
     */
    private URL createURLtoServer(String path) throws MalformedURLException {
        if (VERBOSE) Log.v(TAG, "creating to " + path);
        return new URL(CONNECTION_PROTOCOL, serverAddress, SERVER_SOCKET, path);
    }

    private boolean handleResponseCode(int responseCode, Messenger replyMessenger) {
        return handleResponseCode(responseCode,replyMessenger,new Bundle());
    }

    /**
     * method "handleResponseCode"
     *
     * @param responseCode
     * @param replyMessenger
     * @return success - whether or not to retry to request
     */
    private boolean handleResponseCode(int responseCode, Messenger replyMessenger, Bundle extradata) {
        boolean success = true;
        if (VERBOSE) Log.v(TAG, "handling responseCode: " + responseCode);
        Message msg;
        try {

            switch (responseCode) {
                case RESPONSE_SUCCESS:
                    //let the user know
                    msg = Message.obtain(null, MainActivity.MSG_SUCCESS);
                    msg.setData(extradata);
                    replyMessenger.send(msg);
                    break;

                case RESPONSE_BLOCK_CONFLICT:
                    //Why did this happen?
                    Log.i(TAG, "Block already existed in the database... quitting...");
                    break;

                case RESPONSE_TOO_MANY_REQUESTS:
                    replyMessenger.send(Message.obtain(null, MainActivity.MSG_TOO_MANY_REQUESTS));
                    break;

                case RESPONSE_UNPROCESSABLE_ENTITY:
                    Log.i(TAG, "something bad happened.... fix this...");
                    break;

                case RESPONSE_UNAUTHORIZED:
                    Log.i(TAG, "Unauthorized...");

                    mMessenger.send(Message.obtain(null, new initializeUserWithServer()));
                    replyMessenger.send(Message.obtain(null, MainActivity.MSG_UNAUTHORIZED));

                    break;

                case RESPONSE_NOT_FOUND:
                    msg = Message.obtain(null, MainActivity.MSG_NOT_FOUND);
                    msg.setData(extradata);

                    break;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error sending message to Mainactivity...", e);
            success = false;
        }

        return success;
    }

    /***********************************************************************************************
     * LOCATION HANDLING
     *
     */
    /**
     * method 'buildGoogleApiClient'
     * <p/>
     * convenience method
     */
    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "enter buildGoogleApiClient...");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * method 'createLocationRequest'
     * <p/>
     * convenience method
     */
    protected void createLocationRequest(int interval, int fastest) {
        if (interval == 0 && fastest == 0) {
            removeLocationRequest();
            return;
        }

        Log.d("Location Services", "setting location updates with high accuracy.");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastest);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    public void removeLocationRequest() {
        Log.d("Location Services", "removing location updates...");
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    /**
     * method 'onConnected'
     * <p/>
     * called upon successful connection to google maps api client
     *
     * @param connectionHint //todo no idea
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "connected successfully...");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (VERBOSE) {
            if (mLocation != null) {
                Toast.makeText(this, "Latitude is: " + mLocation.getLatitude() + " Longitude is"
                        + mLocation.getLongitude(), Toast.LENGTH_LONG).show();
            } else {
                Log.d("Location Services", "a null Location was returned");
            }
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "connection failed...");
        int result = connectionResult.getErrorCode();

        //todo this could be better
        if (result == ConnectionResult.SERVICE_MISSING ||
                result == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                result == ConnectionResult.SERVICE_UPDATING) {
            Toast.makeText(this, "Google play services is missing or out of date", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "connection to location services failed... " + result, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended...");

        if (VERBOSE) {
            Toast.makeText(this, "Location connection suspended", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "new location aquired...");
        mLocation = location;
        if (VERBOSE) {
            Log.v(TAG, "new location coordinates: " + location.getLatitude() + " " + location.getLongitude());
            Toast.makeText(getApplicationContext(), "onLocationChanged called", Toast.LENGTH_SHORT).show();
        }
    }

    /***********************************************************************************************
     *
     *  IMAGE STORAGE
     *
     */
    /**
     * internal file filter for deleting all stored image files
     */
    public class ImageFileFilter implements FileFilter {
        private final String[] okFileExtensions =
                new String[]{"jpg", "bmp", "png"};

        public boolean accept(File file) {
            for (String extension : okFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * copies content from source file to destination file
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }

    }

    /***************************************************************************************************
     * ANALYTICS
     */
    private void sendSignOnEvent(String userID) {
        Log.i(TAG, "sending login event for user: " + userID.toString());

        mTracker = ((AnalyticsApplication) getApplication()).getDefaultTracker();
        mTracker.set("&uid", userID.toString());
        mTracker.send(new HitBuilders.EventBuilder().setCategory("UX").setAction("User Sign In").build());
    }

    /**
     * method 'sendScreenName'
     * <p/>
     * reports a screen view event to Google Analytics
     *
     * @param name name of the screen to be sent
     */
    private void sendScreenImageName(String name) {
        if (VERBOSE) Log.v(TAG, "Sending screen event for screen name: " + name);
        //   Toast.makeText(this,name,Toast.LENGTH_SHORT).show();
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    /**
     * method 'reportAnalyticsEvent'
     * <p/>
     * reports an event to Google analytics.
     * <p/>
     * {@link com.jokrapp.android.LiveFragment.onLiveFragmentInteractionListener}
     * {@link com.jokrapp.android.LocalFragment.onLocalFragmentInteractionListener}
     * Called by these linked interface callbacks.
     *
     * @param event which kind of even to report
     * @param name  additional info to be reported (this will maybe be a Bundle)
     */
    public void reportAnalyticsEvent(int event, String name) {
        if (VERBOSE)
            Log.v(TAG, "entering reportAnalyticsEvent with event: " + event + " and name " + name);

        switch (event) {
            case Constants.LIVE_THREADVIEW_EVENT:
                sendScreenImageName(name);
                break;
            case Constants.LOCAL_BLOCK_EVENT:

                break;

            case Constants.LOCAL_MESSAGE_EVENT:

                break;

            case Constants.FRAGMENT_VIEW_EVENT:
                mTracker.setScreenName(name);
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());
                break;
        }
    }


    /***************************************************************************************************
     * AWS INTEGRATION
     */

    public void uploadImageToS3(Bundle b) {
        if (VERBOSE) {
            Log.v(TAG,"entering uploadImageToS3...");
            LogUtils.printBundle(b,TAG);
        }

        if (transferUtility == null) {
            initializeTransferUtility();
        }


        File file = new File(getCacheDir(),b.getString(Constants.KEY_S3_KEY));

        if (Constants.LOGD)
            Log.d(TAG,"uploading image to s3 with key: " + b.getString(Constants.KEY_S3_KEY));
        Log.d(TAG, "uploading image from file" + file.getPath());

        TransferObserver observer = transferUtility.upload(
                BUCKET_NAME,     /* The bucket to upload to */
                b.getString(Constants.KEY_S3_DIRECTORY) + "/" + b.getString(Constants.KEY_S3_KEY), /* The key for the uploaded object */
                file /* The file where the data to upload exists */
        );

        pendingMap.put(observer.getId(), b);
        observer.setTransferListener(this);


        if (VERBOSE) {
            Log.v(TAG,"exiting uploadImageToS3...");
        }
    }

    public void downloadImageFromS3(Bundle b) {
        if (VERBOSE) {
            Log.v(TAG,"entering downloadImageFromS3...");
            LogUtils.printBundle(b,TAG);
        }

        if (transferUtility == null) {
            initializeTransferUtility();
        }

        if (b.getString(Constants.KEY_S3_KEY)==null){
            return;
        }
        File file = new File(getCacheDir(),b.getString(Constants.KEY_S3_KEY));

        if (Constants.LOGD) {
            Log.d(TAG, "downloading image from  s3 with key: " + b.getString(Constants.KEY_S3_KEY));
            Log.d(TAG, "downloading image from file" + file.getPath());
        }

        TransferObserver observer = transferUtility.download(
                BUCKET_NAME,     /* The bucket to upload to */
                b.getString(Constants.KEY_S3_DIRECTORY) + "/"
                        + b.getString(Constants.KEY_S3_KEY),/* The key for the uploaded object */
                file /* The file where the data to upload exists */
        );


        pendingMap.put(observer.getId(), b);
        observer.setTransferListener(this);


        if (VERBOSE) {
            Log.v(TAG,"exiting downloadImageFromS3...");
        }
    }

    public String downloadImageFromURL(String url) {
        if (VERBOSE) {
            Log.v(TAG,"entering downloadImageFromURL with url: " + url);
        }

        try {
            InputStream is = (InputStream) new URL(url).getContent();
            byte[] array = IOUtils.toByteArray(is);
            UUID name = UUID.nameUUIDFromBytes(array);

            File f = new File(getCacheDir(),name.toString());

            Log.i(TAG,"writing image url to path " + f.toString());
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));
            os.write(array);
            os.flush();
            os.close();

            if (VERBOSE) {
                Log.v(TAG,"exiting downloadImageFromURL with name: " + name);
            }
            return name.toString();
        } catch (MalformedURLException e) {
            Log.e(TAG,"invalid URL... what!?");
        } catch (IOException e ){
            Log.e(TAG,"ioException");
        }

        if (VERBOSE) {
            Log.v(TAG,"error occured, exiting downladImageFromURL with null...");
        }
        return null;
    }


    /**
     * method 'onStateChanged'
     *
     * callback from the high level AWS TransferObserver
     *
     * currently, sending and receiving are handled very differently in regards to static content
     *
     * for sending, the dynamic content is only sent to the server upon success confirmation from
     * AWS
     *
     * for receiving, as soon as the dynamic content is received, the static is also received.
     *
     * @param id
     * @param state
     */
    @Override
    public void onStateChanged(int id, TransferState state) {
        if (VERBOSE) {
            Log.v(TAG,"AWS TransferState: " + state.name() + " for id " + id);
        }

        switch (state) {

            case CANCELED:
            case UNKNOWN:
            case FAILED:
                Toast.makeText(getApplicationContext(),
                        "s3 transfer failed... removing content",
                        Toast.LENGTH_SHORT)
                        .show();

                //remove pending dynamic data
                pendingMap.remove(id);

                break;

            case COMPLETED:
                if (VERBOSE) {
                    Log.v(TAG,"static content successfully transferred");
                }
                //get the data pending data for this transfer ID
                Bundle data = pendingMap.get(id);
                //Depending on what the pending post was... handle it
                if (data == null) {
                    Log.e(TAG,"incoming bundle was null");
                    break;
                } else if (VERBOSE){
                    LogUtils.printBundle(data,TAG);
                }

                switch (data.getInt(PENDING_TRANSFER_TYPE,-1)) {
                    case MSG_SEND_IMAGE:

                        createLocalPost(data);
                        break;

                    case MSG_CREATE_THREAD:

                        createLiveThreadAsync(data);
                        break;

                    case MSG_SEND_MESSAGE:

                        sendImageMessageAsync(data);
                        break;

                    case MSG_REQUEST_LIVE_THREADS:

                        break;

                    case MSG_REQUEST_LOCAL_POSTS:

                        break;

                    case MSG_REQUEST_REPLIES:

                        break;

                    case MSG_REQUEST_MESSAGES:

                        break;

                    case MSG_DOWNLOAD_IMAGE:

                        if (VERBOSE) Log.d(TAG,"image finished downloading... broadcasting...");

                        Intent intent;

                        switch (data.getString(Constants.KEY_S3_DIRECTORY,"")) {
                            case Constants.KEY_S3_LIVE_DIRECTORY:
                                intent = new Intent(Constants.ACTION_IMAGE_LIVE_LOADED);
                                intent.putExtras(data);
                                sendBroadcast(intent);
                                break;

                            case Constants.KEY_S3_LOCAL_DIRECTORY:
                                intent = new Intent(Constants.ACTION_IMAGE_LOCAL_LOADED);
                                intent.putExtras(data);
                                sendBroadcast(intent);
                                break;


                            case Constants.KEY_S3_MESSAGE_DIRECTORY:
                                intent = new Intent(Constants.ACTION_IMAGE_MESSAGE_LOADED);
                                intent.putExtras(data);
                                sendBroadcast(intent);
                                break;



                            case Constants.KEY_S3_REPLIES_DIRECTORY:
                                intent = new Intent(Constants.ACTION_IMAGE_REPLY_LOADED);
                                intent.putExtras(data);
                                sendBroadcast(intent);
                                break;


                            default:
                                Log.e(TAG, "no directory was provided, not broadcasting");
                                break;
                        }
                        break;


                    default:
                        Log.e(TAG,"no dynamic transfer type was provided... discarding...");
                        pendingMap.remove(id);

                }

                pendingMap.remove(id);

                break;
        }

        // do something
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        int percentage = (int) (bytesCurrent / bytesTotal * 100);
        if (VERBOSE) {
            Log.v(TAG,id + " : " + percentage + "%");
        }

        //Display percentage transfered to user
    }

    @Override
    public void onError(int id, Exception ex) {
        Toast.makeText(getApplicationContext(), "error occured for transfer: " + id,Toast.LENGTH_SHORT).show();
        Log.e(TAG, "entering onError...", ex);
    }


}