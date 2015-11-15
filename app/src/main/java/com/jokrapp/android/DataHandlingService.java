package com.jokrapp.android;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.analytics.HitBuilders;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
        GoogleApiClient.OnConnectionFailedListener, LocationListener, TransferListener,
        InitializeUserRunnable.initializeUserMethods {
    private static final String TAG = "DataHandlingService";
    private static final boolean VERBOSE = true;
    private static final boolean ALLOW_DUPLICATES = false;
    private boolean isLocalRequesting = false;

    /**
     * NETWORK COMMUNICATION
     */
    private static GoogleApiClient mGoogleApiClient;
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
    final String GET_LIVE_THREAD_REPLIES = "/reply/get/"; //does not change

    /**
     * S3 INFO
     */
    private final String BUCKET_NAME = "launch-zone";
    private MobileAnalyticsManager mTracker;
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
    static final String THREAD_ID = "threadID";
    private final String TO = "to";
    private final String IMAGE_URL = "url";
    private final String TITLE = "title";
    private final String TEXT = "text";
    private final String NAME = "name";

    /**
     * DATA HANDLING
     */
    private static Location mLocation;
    ////private static  = new ArrayList<>();
    private static List<String> imagesSeen = Collections.synchronizedList(new ArrayList<String>());
    private static final String IMAGESSEEN_KEY = "images";
    private final String ISFIRSTRUN_KEY = "firstrun";
    private final String UUID_KEY = "uuidkey";
    private static final int OLDEST_ALLOWED_IMAGE = 3600 * 1000; // two hours

    /**
     *  AWS (AMAZON WEB SERVICE S3)
     */
    static TransferUtility transferUtility;
    HashMap<Integer,Bundle> pendingMap = new HashMap<>();
    private final String REQUEST_TYPE = "what";

    /**
     * method 'onCreate'
     * <p/>
     * lifeCycle method, called when the service is created, runs in the main process
     */
    @Override
    public void onCreate() {
        if (VERBOSE) Log.v(TAG, "entering onCreate...");

        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        mTracker = ((AnalyticsApplication) getApplication()).getAnalyticsManager();

        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
        boolean isFirstRun = settings.getBoolean(ISFIRSTRUN_KEY, true);

        if (isFirstRun) {
            Log.d(TAG, "the app is opening for the first time, generating a user ID");
          /*  SharedPreferences.Editor editor = settings.edit();
            editor.putString(UUID_KEY, userID.toString());
            editor.putBoolean(ISFIRSTRUN_KEY, false);
            editor.apply();*/

            try {
                mMessenger.send(Message.obtain(null, new InitializeUserRunnable(this)));
            } catch (RemoteException e) {
                Log.e(TAG, "initializing user with server failed...", e);
            }

        } else {
            Log.d(TAG, "loading userID from storage...");
            userID = UUID.fromString(settings.getString(UUID_KEY, null));
            sendSignOnEvent(userID.toString());
        }

        if (!ALLOW_DUPLICATES) {

            if (VERBOSE) Log.v(TAG, "Duplicates are not allowed, loading imagesSeen");

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
            if (VERBOSE) Log.v(TAG, "Duplicates are allowed, not loaded imagesSeen");
            imagesSeen = new ArrayList<>();
        }

        if (VERBOSE) Log.v(TAG, "exiting onCreate...");
    }

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

    private Thread initializeUserThread;

    public void setUserID(UUID userID) {
        if (VERBOSE) Log.v(TAG,"entering setUserID : " + userID);

        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(UUID_KEY, userID.toString());
        editor.putBoolean(ISFIRSTRUN_KEY, false);
        editor.apply();

        DataHandlingService.userID = userID;

        sendSignOnEvent(userID.toString());

        if (VERBOSE) Log.v(TAG,"exiting setUserID...");
    }

    public void handleInitializeState(int state) {
        switch (state) {
            case InitializeUserRunnable.INITIALIZE_STARTED:
                Log.d(TAG,"initialize started...");
                break;

            case InitializeUserRunnable.INITIALIZE_FAILED:
                Log.d(TAG,"initialize failed!");
                break;

            case InitializeUserRunnable.INITIALIZE_SUCCESS:
                Log.d(TAG,"initialize success :)");
                break;
        }
    }

    public String getInitializeUserPath() {
        return INITIALIZE_USER_PATH;
    }

    public void setInitializeUserThread(Thread initializeUserThread) {
        this.initializeUserThread = initializeUserThread;
    }

    public String getRequestThreadsPath() {
        return GET_LIVE_THREAD_LIST;
    }

    public void setRequestRepliesThread(Thread thread) {
    }

    public List<String> getImagesSeen() {
        return imagesSeen;
    }


    private void createLocalPostAsync(final Bundle b) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                createLocalPost(b.getString(Constants.KEY_S3_KEY), mLocation, b.getString(Constants.KEY_TEXT, ""));
            }
        }).start();

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
    private synchronized void createLocalPost(final String imageKey,
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
     * method 'sendImageMessage'
     * <p/>
     * sends a message to a specific user specified by UUID
     *
     * @param filePath      the location the image is stored which is to be sent
     * @param messageTarget the UUID of the user to send the message to
     */
    private void sendImageMessage(String filePath, String messageTarget, String text) {
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
            jGen.writeStringField(Constants.KEY_TEXT, text);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            handleResponseCode(conn.getResponseCode(),replyMessenger);
            deleteFile(filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length()));
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
    }

    public void sendImageMessageAsync(Bundle b) {
        sendImageMessageAsync(b.getString(Constants.KEY_S3_KEY), b.getString(Constants.MESSAGE_TARGET), b.getString(Constants.KEY_TEXT, ""));
    }

    public void sendImageMessageAsync(final String filePath, final String messageTarget, final String text) {
        if (VERBOSE) {
            Log.v(TAG,"entering sendImageMessageAsync with: " + filePath + ", " + messageTarget);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                sendImageMessage(filePath, messageTarget, text);
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
     * @param data the incoming data
     */
    private void replyToLiveThread(Bundle data) {
        if (VERBOSE) Log.v(TAG, "creating reply to live thread...");

        int threadID = data.getInt("threadID");
        String name = data.getString("name");
        String description = data.getString("description");
        String filePath = data.getString(Constants.KEY_S3_KEY,"");

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
                Log.v(TAG, "sending reply to server:");
                Log.v(TAG, "from : " + userID.toString());
                Log.v(TAG, "thread number : " + threadID);
                Log.v(TAG, "name : " + name);
                Log.v(TAG, "text : " + description);
                Log.v(TAG, "url : " + filePath);
            }

            jGen.writeStartObject();
            jGen.writeNumberField(THREAD_ID, threadID);
            jGen.writeStringField(NAME, name);
            jGen.writeStringField(TEXT, description);
            jGen.writeStringField("url", filePath);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            handleResponseCode(conn.getResponseCode(), replyMessenger,data);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }

        if (filePath != null) {
            deleteFile(filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length()));
        }
    }


    private void replyToLiveThreadAsync(final Bundle indata) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                replyToLiveThread(indata);
            }
        }).start();
    }


    /**
     * method 'saveIncomingJsonArray'
     *
     * uses the provided connection to parse the incoming json object array into individual object
     * rows, and then insert those rows into their corresponding sqlite database
     *
     * @param where table to insert into
     * @param conn connection to parse from
     * @param extradata additional data that could be provided
     * @return amount of rows inserted
     * @throws IOException
     */
    public int saveIncomingJsonArray(int where, URLConnection conn, Bundle extradata) throws IOException {
        if (VERBOSE) Log.v(TAG,"entering saveIncomingJsonArray...");

        int rows;


        JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());
        ObjectMapper objectMapper = new ObjectMapper();

        ArrayList<LinkedHashMap<String,Object>>jsonArray =
                objectMapper.readValue(jParser,ArrayList.class);

        if (!jsonArray.isEmpty()) {
            LinkedHashMap<String,Object> map;

            if (VERBOSE)
                Log.v(TAG, "printing verbose output...");


            if (where == MSG_REQUEST_LIVE_THREADS) {

                if (VERBOSE) Log.v(TAG, "first empty the live table, then insert...");
                getContentResolver().delete(FireFlyContentProvider.CONTENT_URI_LIVE, null, null);

            }

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

                        map.put(SQLiteDbContract.LiveReplies.COLUMN_ID, map.remove("id"));
                        map.put(SQLiteDbContract.LiveReplies.COLUMN_NAME_THREAD_ID, extradata.getInt(THREAD_ID));
                        b.putInt(REQUEST_TYPE, MSG_REQUEST_REPLIES);
                        break;
                    case MSG_REQUEST_LOCAL_POSTS:
                        if (VERBOSE) Log.v(TAG,"saving json from local/");

                        //swap id for _ID, to allow listview loading, and add the thread ID
                        Object id = map.remove("id");
                        imagesSeen.add(String.valueOf(id));
                        map.put(SQLiteDbContract.LiveReplies.COLUMN_ID, id);
                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LOCAL_DIRECTORY);
                        break;
                    case MSG_REQUEST_LIVE_THREADS:
                        if (VERBOSE) Log.v(TAG,"saving json from live");

                        map.put(SQLiteDbContract.LiveThreadEntry.COLUMN_ID, map.remove("order"));
                        map.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_THREAD_ID, map.remove("id"));
                        b.putInt(REQUEST_TYPE, MSG_REQUEST_LIVE_THREADS);
                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LIVE_DIRECTORY);
                        break;

                    case MSG_REQUEST_MESSAGES:
                        if (VERBOSE) Log.v(TAG,"saving json from message");
                        map.put(SQLiteDbContract.MessageEntry.COLUMN_ID, map.remove("id"));
                        b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_MESSAGE_DIRECTORY);
                        b.putInt(REQUEST_TYPE, MSG_REQUEST_MESSAGES);
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

                            if (where == MSG_REQUEST_REPLIES) {
                                b.putString("url", cont);
                                b.putString(Constants.KEY_S3_KEY, cont+"s");
                                b.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_REPLIES_DIRECTORY);
                                downloadImageFromS3(b);
                            }
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
                        Log.d(TAG,"saving to replies...");
                        getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_REPLY_LIST, values);
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
            rows = jsonArray.size();
        } else {
            Log.v(TAG, "nothing was supplied...");
            rows = 0;
        }

        jParser.close();


        if (VERBOSE) Log.v(TAG,"exiting saveIncomingJsonArray... returning " + rows + "rows.");
        return  rows;
    }

    public void insert(Uri uri, ContentValues values) {
       getContentResolver().insert(uri, values);
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

    private Messenger imageResponseMessenger;

    public void setReplyMessenger(Messenger messenger) {
        replyMessenger = messenger;
    }

    public Messenger getReplyMessenger() {
        return replyMessenger;
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

    static final int MSG_REPORT_ANALYTICS= 100;

    static final int MSG_REPORT_ANALYTIC_TIMING = 101;

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

            if (Constants.client_only_mode) {
                Log.w(TAG,"client only mode is enabled, discarding request...");
                return;
            }

            ServerTask task;



            Bundle data = msg.getData();
            data.putInt(REQUEST_TYPE, msg.what);

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

                    String messageTarget = data.getString(Constants.MESSAGE_TARGET);
                    //Log.d(TAG, "image is stored at " + filePath);
                    if (messageTarget == null) {
                        Log.d(TAG, "posting to local...");
                        data.putInt(REQUEST_TYPE, MSG_SEND_IMAGE);
                        data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LOCAL_DIRECTORY);
                        //irs.get().createLocalPost(filePath, mLocation);
                    } else {
                        Log.d(TAG, "sending Message to user: " + messageTarget);
                        data.putInt(REQUEST_TYPE, MSG_SEND_MESSAGE);
                        data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_MESSAGE_DIRECTORY);
                        //irs.get().sendImageMessageAsync(filePath, messageTarget, msg.arg1);
                    }

                    uploadImageToS3(data);
                    break;

                case MSG_REQUEST_LOCAL_POSTS:
                    if (Constants.LOGD) Log.d(TAG, "received a message to request local posts");
                    task = new RequestLocalTask();
                    if (mLocation!=null) {
                        data.putDouble(SQLiteDbContract.LocalEntry.COLUMN_NAME_LONGITUDE, mLocation.getLongitude());
                        data.putDouble(SQLiteDbContract.LocalEntry.COLUMN_NAME_LATITUDE, mLocation.getLatitude());
                    }
                    task.initializeTask(irs.get(),data,userID);
                    new Thread(task.getServerConnectRunnable()).start();
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

                    String s = data.getString(Constants.IMAGE_FILEPATH);
                    throw new NotYetConnectedException();

                case MSG_REQUEST_MESSAGES:
                    Log.d(TAG, "received a message to message a user");
                    irs.get().requestLocalMessagesAsync();
                    break;

                case MSG_BLOCK_USER:
                    Log.d(TAG, "received a message to block a user");

                    task = new SendLocalBlockTask();
                    task.initializeTask(irs.get(),data,userID);
                    new Thread(task.getServerConnectRunnable()).start();
                    break;

                case MSG_CREATE_THREAD:
                    Log.d(TAG, "received a message to create a thread");

                    data.putInt(REQUEST_TYPE, MSG_CREATE_THREAD);
                    data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_LIVE_DIRECTORY);
                    uploadImageToS3(data);
                    break;

                case MSG_REPLY_TO_THREAD:
                    Log.d(TAG, "received a message to reply to a thread");

                    data.putInt(REQUEST_TYPE, MSG_REPLY_TO_THREAD);

                    if (data.getString(Constants.KEY_S3_KEY,"").equals("")){
                        if (Constants.LOGD) Log.d(TAG,"no image filepath was provided," +
                                " this must be a text reply, so uploading straight to the server.");
                        irs.get().replyToLiveThreadAsync(data);
                    } else {
                        if (Constants.LOGD) Log.d(TAG,"contained an image filepath," +
                            "uploading the image there to s3 first...");

                        data.putString(Constants.KEY_S3_DIRECTORY, Constants.KEY_S3_REPLIES_DIRECTORY);
                        uploadImageToS3(data);
                    }

                    break;

                case MSG_REQUEST_LIVE_THREADS:
                    Log.d(TAG, "received a message to request the thread list");

                    task = new RequestLiveTask();
                    task.initializeTask(irs.get(), data, userID);
                    new Thread(task.getServerConnectRunnable()).start();
                    break;

                case MSG_REQUEST_REPLIES:
                    Log.d(TAG, "received a message to request replies.");

                    data.putInt("threadID", msg.arg1);
                    task = new RequestRepliesTask();
                    task.initializeTask(irs.get(), data, userID);
                    new Thread(task.getServerConnectRunnable()).start();
                    break;

                case MSG_SET_CALLBACK_MESSENGER:
                    Log.d(TAG, "setting callback messenger...");
                    irs.get().setReplyMessenger(msg.replyTo);
                    break;

                case MSG_DOWNLOAD_IMAGE:
                    Log.d(TAG, "received a message to download an image...");

                    imageResponseMessenger = msg.replyTo;
                    downloadImageFromS3(data);

                    break;

                case MSG_REPORT_ANALYTICS:
                    if (Constants.LOGD) Log.d(TAG, "Received a message to report analytics events");
                    reportAnalyticsEvent(msg.getData());
                    break;

                case MSG_REPORT_ANALYTIC_TIMING:

                    if (Constants.LOGD) Log.d(TAG, "Received a message to report timing events");
                    reportAnalyticsTimingEvent(msg.getData());
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
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(20000);


        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        if (userID == null) {
            conn.setRequestProperty("X-Client-UserID", "");
        } else {
            conn.setRequestProperty("X-Client-UserID", userID.toString());
        }
        conn.setUseCaches(false);


        return conn;
    }


    public HttpURLConnection connectToServer(String serverPath) throws ConnectException {
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

    static final int CONNECTION_FAILED = 0;
    static final int CONNECTION_STARTED = 1;
    static final int CONNECTION_COMPLETED = 2;
    static final int REQUEST_FAILED = 3;
    static final int REQUEST_STARTED = 4;
    static final int TASK_COMPLETED = 5;

    /**
     * method 'handleDownloadState'
     *
     * handles the responses passed upwards from each respective serverTask
     *
     * @param state the state of the content being downloaded
     */
    public void handleDownloadState(int state, ServerTask task) {
        switch (state) {
            case CONNECTION_FAILED:
                Log.i(TAG, "Connection failed...");
                break;

            case CONNECTION_STARTED:
                Log.i(TAG,"Connection started...");
                break;

            case CONNECTION_COMPLETED:
                Log.i(TAG,"Connection completed...");
                new Thread(task.getRequestRunnable()).start();
                break;

            case REQUEST_FAILED:
                Log.i(TAG,"Request failed...");
                break;

            case REQUEST_STARTED:
                Log.i(TAG,"Request started...");
                break;

            case TASK_COMPLETED:
                Log.i(TAG,"Task completed... by name: " + task.toString());
                break;
        }

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
        return new URL("http", serverAddress, 80, path);
    }

    private boolean handleResponseCode(int responseCode, Messenger replyMessenger) {
        return handleResponseCode(responseCode,replyMessenger,new Bundle());
    }

    public boolean handleResponseCode(int code) {
        return handleResponseCode(code,replyMessenger,new Bundle());
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
                    msg.arg1 = extradata.getInt(REQUEST_TYPE,-1);
                    if (replyMessenger != null) {
                        replyMessenger.send(msg);
                    } else {
                        Log.e(TAG,"replyMessenger was null for Response_Success");
                    }
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

                    mMessenger.send(Message.obtain(null, new InitializeUserRunnable(this)));
                    replyMessenger.send(Message.obtain(null, MainActivity.MSG_UNAUTHORIZED));
                    break;

                case RESPONSE_NOT_FOUND:

                    msg = Message.obtain(null, MainActivity.MSG_NOT_FOUND);
                    msg.setData(extradata);
                    replyMessenger.send(msg);
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



    /***************************************************************************************************
     * ANALYTICS
     */
    private void sendSignOnEvent(String userID) {
        Log.i(TAG, "sending login event for user: " + userID.toString());
        mTracker.getEventClient()
                .recordEvent(mTracker
                        .getEventClient()
                        .createEvent(Constants.ANALYTICS_CATEGORY_LIFECYCLE)
                        .withAttribute(Constants.ANALYTICS_CATEGORY_MESSAGE, userID
                        ));
        mTracker.getEventClient().addGlobalAttribute(Constants.ANALYTICS_ATTRIBUTE_USER_ID,userID);


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
     */
    public void reportAnalyticsEvent(Bundle data) {
        if (VERBOSE) {
            Log.v(TAG, "entering reportAnalyticsEvent");
            LogUtils.printBundle(data, TAG);
        }

        AnalyticsEvent event = mTracker.getEventClient().createEvent(data.getString(Constants.KEY_ANALYTICS_CATEGORY));
        event.withAttribute(Constants.KEY_ANALYTICS_ACTION, data.getString(Constants.KEY_ANALYTICS_ACTION));

        if (data.containsKey(Constants.KEY_ANALYTICS_LABEL)) {
            event.withAttribute(Constants.KEY_ANALYTICS_LABEL,data.getString(Constants.KEY_ANALYTICS_LABEL));
        }

        if (data.containsKey(Constants.KEY_ANALYTICS_VALUE)) {
            event.withAttribute(Constants.KEY_ANALYTICS_VALUE, data.getString(Constants.KEY_ANALYTICS_VALUE));
        }

        mTracker.getEventClient().recordEvent(event);

        Log.v(TAG, "exiting reportAnalyticsEvent");
    }


    /**
     * method 'reportAnalyticsTimingEvent'
     * <p/>
     * reports a timing event to Google analytics.
     * <p/>
     * {@link com.jokrapp.android.LiveFragment.onLiveFragmentInteractionListener}
     * {@link com.jokrapp.android.LocalFragment.onLocalFragmentInteractionListener}
     * Called by these linked interface callbacks.
     *
     */
    public void reportAnalyticsTimingEvent(Bundle data) {
        if (VERBOSE) {
            Log.v(TAG, "entering reportAnalyticsEvent");
            LogUtils.printBundle(data, TAG);
        }

        HitBuilders.TimingBuilder event = new HitBuilders.TimingBuilder()
                .setCategory(data.getString(Constants.KEY_ANALYTICS_CATEGORY))
                .setValue(data.getLong(Constants.KEY_ANALYTICS_ACTION));

        if (data.containsKey(Constants.KEY_ANALYTICS_LABEL)) {
            event.setLabel(data.getString(Constants.KEY_ANALYTICS_LABEL));
        }

        if (data.containsKey(Constants.KEY_ANALYTICS_VARIABLE)) {

            event.setVariable(data.getString(Constants.KEY_ANALYTICS_VARIABLE));
        }

        //mTracker.send(event.build()); todo fix this

        Log.v(TAG, "exiting reportAnalyticsEvent");
    }


    /***************************************************************************************************
     * AWS INTEGRATION
     */

    public void uploadImageToS3(Bundle b) {
        if (VERBOSE) {
            Log.v(TAG,"entering uploadImageToS3...");
            LogUtils.printBundle(b, TAG);
        }

        if (transferUtility == null) {
            initializeTransferUtility();
        }


        String directory = b.getString(Constants.KEY_S3_DIRECTORY);

        File file = new File(getCacheDir(),b.getString(Constants.KEY_S3_KEY));

        if (Constants.LOGD)
            Log.d(TAG,"uploading image to s3 with key: " + b.getString(Constants.KEY_S3_KEY));
        Log.d(TAG, "uploading image from file" + file.getPath());

        TransferObserver observer = transferUtility.upload(
                BUCKET_NAME,     /* The bucket to upload to */
                directory + "/" + b.getString(Constants.KEY_S3_KEY), /* The key for the uploaded object */
                file /* The file where the data to upload exists */
        );


        pendingMap.put(observer.getId(), b);
        observer.setTransferListener(this);

        if (directory.equals(Constants.KEY_S3_REPLIES_DIRECTORY)) {
            Log.i(TAG,"now uploading reply thumbnail");
            pendingMap.remove(observer.getId());
            observer.cleanTransferListener();

             observer = transferUtility.upload(
                    BUCKET_NAME,     /* The bucket to upload to */
                    directory + "/" + b.getString(Constants.KEY_S3_KEY)+"s", /* The key for the uploaded object */
                    new File(getCacheDir(),b.getString(Constants.KEY_S3_KEY)+"s") /* The file where the data to upload exists */
            );
            b.putInt(REQUEST_TYPE,MSG_REPLY_TO_THREAD);
            pendingMap.put(observer.getId(), b);
            observer.setTransferListener(this);
        }

        if (VERBOSE) {
            Log.v(TAG,"exiting uploadImageToS3...");
        }
    }

    public void downloadImageFromS3(Bundle b) {
        if (VERBOSE) {
            Log.v(TAG, "entering downloadImageFromS3...");
            LogUtils.printBundle(b, TAG);
        }

        if (transferUtility == null) {
            initializeTransferUtility();
        }

        String directory = b.getString(Constants.KEY_S3_DIRECTORY,"");
        String key = b.getString(Constants.KEY_S3_KEY,"");

        if (key.equals("") || directory.equals("")){
            Log.e(TAG,"invalid parameters provided...");
            throw new RuntimeException("downloadImageFromS3 is missing parameters...");
        }

        File file = new File(getCacheDir(),key);

        if (Constants.LOGD) {
            Log.d(TAG, "downloading image from  s3 path:  " + directory + " / " + key);
            Log.d(TAG, "storing downloaded imaged at: " + file.getPath());
        }

        TransferObserver observer = transferUtility.download(
                BUCKET_NAME,     /* The bucket to upload to */
                directory + "/" + key,/* The key for the uploaded object */
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
        Bundle data;

        switch (state) {

            case CANCELED:
            case UNKNOWN:
            case FAILED:
                data = pendingMap.get(id);
                switch (data.getInt(REQUEST_TYPE,-1)) {
                    case MSG_DOWNLOAD_IMAGE:
                        if (VERBOSE) Log.d(TAG,"an image failed to download. notifying photoManager..");

                        Message msg = Message.obtain(null,PhotoManager.DOWNLOAD_FAILED);
                        msg.setData(data);

                        try {
                            imageResponseMessenger.send(msg);
                        } catch (RemoteException e) {
                            Log.e(TAG,"error responding that an image has finished downloading...");
                        }
                        break;


                    default:
                        Toast.makeText(getApplicationContext(),
                                "s3 transfer failed... removing content",
                                Toast.LENGTH_SHORT)
                                .show();
                }

                //remove pending dynamic data
                pendingMap.remove(id);

                break;

            case COMPLETED:
                if (VERBOSE) {
                    Log.v(TAG,"static content successfully transferred");
                }
                //get the data pending data for this transfer ID
                data = pendingMap.get(id);
                //Depending on what the pending post was... handle it
                if (data == null) {
                    Log.e(TAG,"incoming bundle was null");
                    break;
                } else if (VERBOSE){
                    LogUtils.printBundle(data,TAG);
                }

                Intent intent;

                switch (data.getInt(REQUEST_TYPE,-1)) {
                    case MSG_SEND_IMAGE:

                        createLocalPostAsync(data);
                        break;

                    case MSG_CREATE_THREAD:

                        createLiveThreadAsync(data);
                        break;

                    case MSG_SEND_MESSAGE:

                        sendImageMessageAsync(data);
                        break;

                    case MSG_REPLY_TO_THREAD:
                        Log.i(TAG,"reply to thread upload completed...");
                        replyToLiveThreadAsync(data);
                        break;

                    case MSG_REQUEST_LIVE_THREADS:

                        break;

                    case MSG_REQUEST_LOCAL_POSTS:

                        break;

                    case MSG_REQUEST_REPLIES:

                        Log.i(TAG,"notifying UI of Reply download finish");
                        intent = new Intent(Constants.ACTION_IMAGE_REPLY_THUMBNAIL_LOADED);
                        intent.putExtras(data);
                        sendBroadcast(intent);
                        break;

                    case MSG_REQUEST_MESSAGES:

                        break;

                    case MSG_DOWNLOAD_IMAGE:

                        if (VERBOSE) Log.d(TAG,"an image successfully downloaded. notifying photoManager..");

                        Message msg = Message.obtain(null,PhotoManager.DOWNLOAD_COMPLETE);
                        msg.setData(data);

                        if (imageResponseMessenger != null) {
                            try {
                                imageResponseMessenger.send(msg);
                            } catch (RemoteException e) {
                                Log.e(TAG, "error responding that an image has finished downloading...");
                            }
                        } else {
                            Log.e(TAG,"imageResponseMessenger was null, cannot send.");
                        }

                        break;


                    default:
                        Log.e(TAG, "no dynamic transfer type was provided... discarding...");
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
        Bundle b = pendingMap.remove(id);


        Log.i(TAG,"now discarding data for id : " + id);
        LogUtils.printBundle(b,TAG);

        Log.e(TAG, "exiting onError...", ex);
    }


}