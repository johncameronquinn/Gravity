package com.jokrapp.android;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.*;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokrapp.android.SQLiteDbContract.LocalEntry;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Author/Copyright John C. Quinn, All Rights Reserved.
 * date 4/26/2015
 *
 * service 'DataHandlingService'
 *
 * asynchronously serves requests to download new images from the server
 * 'black boxed' - proves the server a gps location, and number of images requested
 * and receives requested number, then stores them locally
 *
 * when no images are stored, a higher priority is given to the request, due to the user
 * currently being at "0" images
 *
 * Communication is managed via a broadcastManager
 *
 *
 */
public class DataHandlingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "DataHandlingService";
    private static final boolean VERBOSE = true;
    private static final boolean ALLOW_DUPLICATES = false;

    private boolean isLocalRequesting = false;


    /** NETWORK COMMUNICATION
     */
    private static GoogleApiClient mGoogleApiClient;
    private final int SERVER_SOCKET = 80; //does not change
    private final String CONNECTION_PROTOCOL = "http";
    private final int READ_TIMEOUT = 10000;
    private final int CONNECT_TIMEOUT = 20000;
    private final String UPLOAD_LOCAL_POST_PATH = "/UploadLocalPost/"; //does not change
    private final String GET_LOCAL_POST_PATH = "/GetLocalPosts/"; //does not change
    private final String SEND_LOCAL_MESSAGE_PATH = "/SendLocalMessage/";
    private final String BLOCK_LOCAL_USER_PATH = "/BlockLocalUser/";
    private final String INITIALIZE_USER_PATH = "/InitializeUser/";
    private final String GET_LOCAL_MESSAGES_PATH = "/GetLocalMessages/"; //does not change
    private final String CREATE_LIVE_THREAD_PATH = "/CreateLiveThread/"; //does not change
    private final String REPLY_LIVE_THREAD_PATH = "/ReplyToLiveThread/"; //does not change
    private final String GET_LIVE_THREAD_LIST = "/GetLiveThreadList/"; //does not change
    private final String GET_LIVE_THREAD_INFO = "/GetLiveThreadInfo/"; //does not change


    private static String serverAddress = "130.211.113.250"; //changes when resolved
    private static UUID userID;

    /** JSON TAGSnnnnnnn
     */
    private final String FROM_USER = "from";
    private final String ROTATION = "rotation";
    private final String USER_ID = "id";
    private final String THREAD_ID = "threadID";
    private final String TO = "to";
    private final String IMAGE = "image";
    private final String TITLE = "title";
    private final String TEXT = "text";
    private final String NAME = "name";

    /** DATA HANDLING
     */
    private static Location mLocation;

    private static ArrayList<String>imagesSeen = new ArrayList<>();
    private static final String IMAGESSEEN_KEY = "images";
    private final String ISFIRSTRUN_KEY = "firstrun";
    private final String UUID_KEY = "uuidkey";

    private static final int OLDEST_ALLOWED_IMAGE = 3600* 1000; // two hours

    /**
     * method 'onCreate'
     *
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
        boolean isFirstRun = settings.getBoolean(ISFIRSTRUN_KEY,true);

        if (isFirstRun) {
            Log.d(TAG,"the app is opening for the first time, generating a user ID");
          /*  SharedPreferences.Editor editor = settings.edit();
            editor.putString(UUID_KEY, userID.toString());
            editor.putBoolean(ISFIRSTRUN_KEY, false);
            editor.apply();*/
            new Thread(new initializeUserWithServer()).start();

        } else {
            Log.d(TAG,"loading userID from storage...");
            userID = UUID.fromString(settings.getString(UUID_KEY,null));
        }

        if (VERBOSE) {
            Log.v(TAG,"Listing stored image files...");
            File[] files = getFilesDir().listFiles(new DataHandlingService.ImageFileFilter());

            File root = Environment.getExternalStorageDirectory();
            File outDirectory = new File(root.getAbsolutePath(),"JokrSavedImages");
            outDirectory.mkdirs();

            for (File i : files) {
                Log.v(TAG, i.toString());

                String fullPath = i.toString();
                String fileName = fullPath.substring(fullPath.lastIndexOf('/')+1);

                try {
                    copyFile(i,new File(outDirectory,fileName));
                } catch (IOException e) {
                    Log.e(TAG,"Error copying file from " + i.toString() + " to " + outDirectory.toString(),e);
                }
            }
            Log.v(TAG,"done listing files...");
        }


        if (!ALLOW_DUPLICATES) {


            if (VERBOSE)
                Log.v(TAG,"Duplicates are not allowed, loading imagesSeen");

            Set<String> loaded = settings.getStringSet(IMAGESSEEN_KEY, null);

            if (loaded == null) {
                Log.d(TAG, "loaded set was null, exiting onCreate...");
                return;
            }

            imagesSeen = new ArrayList<>(loaded);
            if (VERBOSE) {
                Log.v(TAG, "imagesSeen array loaded from SharedPreferences, printing...");
                for (String i : imagesSeen) { //print all sharedpreverences entry
                    Log.v(TAG, i);
                }
            }
        } else {
            if (VERBOSE)
                Log.v(TAG,"Duplicates are allowed, not loaded imagesSeen");
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
            Log.v(TAG,"entering onDestroy...");
            Log.v(TAG, "saving ImagesSeen to sharedPreferences, size: " + imagesSeen.size());
        }


        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);

        settings.edit().putStringSet(IMAGESSEEN_KEY,new HashSet<>(imagesSeen)).apply();

        Log.v(TAG, "exiting onDestroy...");
    }

    public class initializeUserWithServer implements Runnable {

        private Thread currentThread;

        @Override
        public void run() {
            if (VERBOSE) {
                Log.v(TAG,"enter InitializeUser...");
            }
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            currentThread = Thread.currentThread();

            Log.d(TAG,"Initializing user " + userID);

           /* URL url = null;
            try {
                url = createURLtoServer(INITIALIZE_USER_PATH);
            } catch (MalformedURLException e) {
                Log.e(TAG,"MalformedURL",e);
                return;
            }
            URLConnection urlconn;
            try {
                urlconn = url.openConnection();
            } catch (IOException e) {
                Log.e(TAG,"error opening connection to url... exiting....");
                //todo readd to message queue
                return;
            }

            if (VERBOSE) Log.v(TAG,"connection opened to " + urlconn.toString());

            HttpURLConnection conn = (HttpURLConnection) urlconn;
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setConnectTimeout(CONNECT_TIMEOUT);

            try {
                conn.setRequestMethod("GET");
            } catch (ProtocolException e) {
                Log.e(TAG,"failed to set protocol ot post... exiting...");
                //todo readd to message quere
                return;
            }
            conn.setUseCaches(false);
            Log.d(TAG,"Opening connection to url" + url);
            if (conn == null) {
                return;
            }

            /*
            try {
                if (VERBOSE) Log.v(TAG,"opening outputStream to send JSON...");
                JsonFactory jsonFactory = new JsonFactory();
                JsonGenerator jGen = jsonFactory.
                        createGenerator(conn.getOutputStream());
                if (VERBOSE) {
                    Log.v(TAG, "id: " + userID.toString());
                }
                jGen.writeStartObject();
                jGen.writeStringField(USER_ID, userID.toString());
                jGen.writeEndObject();
                jGen.flush();
                jGen.close();

                Log.d(TAG,"response message: " + conn.getResponseMessage());
            } catch (IOException e) {
                Log.e(TAG,"error handling JSON",e);
            }*/

            try {
                userID =UUID.fromString(executeHttpGet());

                SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(UUID_KEY, userID.toString());
                editor.putBoolean(ISFIRSTRUN_KEY, false);
                editor.apply();
            } catch (Exception e) {
                Log.d(TAG,"Error executing initialize user...",e);
            }

            if (VERBOSE) {
                Log.v(TAG,"exiting InitializeUser...");
            }
        }

    }

    public String executeHttpGet() throws Exception {
        BufferedReader in = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI("http://130.211.113.250/InitializeUser/"));
            HttpResponse response = client.execute(request);
            in = new BufferedReader
                    (new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String page = sb.toString();
            System.out.println(page);
            Log.d(TAG,"Page returned" + page);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String,String> asMap = objectMapper.readValue(page, Map.class);
            return String.valueOf(asMap.get("id"));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /***********************************************************************************************
     *
     *   IMAGE SENDING
     *
     */
    /**
     * Handle action send images in the provided background thread with the provided
     * parameters.
     *
     * the client sends the latitude and longitude stored in the location passed in the arguments
     * and the image stored at the filepath provided
     * the sending process uses URLConnection to create the connection
     * the apache commons library and jackson's json parser are both utilized
     *
     * the server marks the time the image is received, and gives the image an ID
     *
     * @param imageData file name of the location of the image to send
     * @param location current location of the device
     */
    private void postImageToLocal(final String imageData,
                                        final Location location,
                                        final Messenger rMessenger,
                                        final int orientation) {
        Log.d(TAG, "entering handleActionSendImages...");

        new Thread(new Runnable() {
            public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);


        if (location == null) {
            Log.e(TAG,"location was null, not sending image");
            return;
        }
        Log.d(TAG,"grabbing stored lat and long...");
        double lat = location.getLatitude();
        double lon = location.getLongitude();

         if (VERBOSE) {
            Log.v(TAG,"grabbing stored lat and long...");
            Log.d(TAG, "Latitude: " + lat + " Longitude: " + lon);
        }

        String image = loadImageForTransit(imageData);
        //todo not sure which method is best to read from image to byte array
    //read the image, send to server

        int status = -1; //status of the image sent
        int responseCode = -1;
        URL url = null;
        try {
            url = createURLtoServer(UPLOAD_LOCAL_POST_PATH);
        } catch (MalformedURLException e) {
            Log.e(TAG,"error creating url to post to local", e);
        }
        HttpURLConnection conn = openConnectionToURL(url);

        if (conn == null || image == null) {
            //todo broadcast this
            Log.d(TAG,"failed to prepare to post to local");
            return;
        }



        try {
            //create the data into an input stream
          //  InputStream is = new FileInputStream(imageData);
            /**
             * create byte array output stream
             *
             * wrap in gzip stream
             *
             * wrap in b64 stream
             *
             * - byte by byte, will convert, then zip
             */

            //copy data from input stream to output stream
/*
            byte[] readData = IOUtils.toByteArray(is);


            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GzipCompressorOutputStream gos = new GzipCompressorOutputStream(bos);

            gos.write(readData);
            gos.flush();
            gos.close();

            String image = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);*/



            //Log.d(TAG, "base64 encoded size is " + readData.length);
            //read data into a byte array
            //readData = IOUtils.toByteArray(new FileInputStream(imageData)); //todo using apache dependancy
            //Log.d(TAG, "byteArray size is " + readData.length);

            //ByteArrayOutputStream jsonStream = new ByteArrayOutputStream(readData.length);


            //String dataString = jsonStream.toString();

            //Log.d(TAG, dataString);
/*
            HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            HttpResponse response;
            JSONObject json = new JSONObject();*/


            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            jGen.writeStartObject();
            jGen.writeStringField(FROM_USER, userID.toString());
            jGen.writeNumberField("latitude", lat);
            jGen.writeNumberField("longitude",lon);
            jGen.writeStringField(IMAGE, image);
            jGen.writeNumberField(ROTATION, orientation);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "server returned successful response" + responseCode);
                Log.d(TAG, "now deleting image from internal storage...");
                    rMessenger.send(Message.obtain(null, MainActivity.MSG_SUCCESS_LOCAL));
                deleteFile(imageData.substring(imageData.lastIndexOf("/") + 1, imageData.length()));
            }

        } catch (IOException e) {
            Log.d(TAG, "IOException");
            Log.e(TAG,"Error sending image to server", e);
        } catch (Exception e) {
            Log.e(TAG, "Error sending image to server", e);
        }

        /**
         * Report status back to the main thread
         */

        Log.d(TAG, "Server response: " + responseCode);

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
     *
     * requests images from the server
     *
     * first sends - the current client location, the count desired
     * and finally an array of images already seen.
     *
     * the client then waits for a reply of images, saving each one
     * - the reply comes in the form of a JSONArray, with position 0 as the count
     * of images incoming. Each image and associated data is its own json object
     * the array is decoded, and the objects are saved to separate files in the internal storage
     * based on ID of the image.
     *
     * @param numberOfImages image count to request
     * @param location current location of the client
     */
    private void requestLocalPosts(int numberOfImages, Location location, Messenger rMessenger) {
        Log.d(TAG,"entering requestImages...");

        if (mLocation == null) {
            Log.e(TAG, "Location was null in requestImages... canceling...");
            return;
            //todo readd request to message queue
        }

        if (numberOfImages == 0) {
            Log.e(TAG,"zero images requested, exiting");
            return;
        }

          double lat = location.getLatitude();
          double lon = location.getLongitude();

        Log.d(TAG,"requesting " + numberOfImages + " images, with lat " + lat + " and lng " +lon);

        int responseCode = 0;

        URL url = null;
        try {
            url = createURLtoServer(GET_LOCAL_POST_PATH);
        } catch (MalformedURLException e) {
            Log.d(TAG,"error creating url to get local posts",e);
        }
        HttpURLConnection conn = openConnectionToURL(url);

        if (conn == null ) {
            //todo broadcast this
            return;
        }

        try {
            //continuing...
            Log.d(TAG, "now writing with JacksonsJSON");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.createGenerator(conn.getOutputStream());

            jGen.writeStartObject();
            jGen.writeStringField("from",userID.toString());
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


            //good, now
            responseCode = 2;


            Log.d(TAG, "response message is: " + conn.getResponseMessage());

            JsonParser jParser = jsonFactory.createParser(conn.getInputStream());

            int id;
            String from;
            int time;
            int weight;
            String image;

            if (jParser.nextValue() == JsonToken.START_ARRAY) {
                while (jParser.nextValue() != JsonToken.END_ARRAY) {
                    Log.d(TAG, "incoming image.");

                    ContentValues values = new ContentValues();

                    jParser.nextValue();
                    id = jParser.getIntValue();
                    jParser.nextValue();
                    from = jParser.getValueAsString();
                    jParser.nextValue();
                    time = Integer.valueOf(jParser.getText());
                    jParser.nextValue();
                    lat = jParser.getDoubleValue();
                    jParser.nextValue();
                    lon = jParser.getDoubleValue();
                    jParser.nextValue();
                    image = jParser.getValueAsString();
                    jParser.nextValue();
                    weight = jParser.getValueAsInt(-1);


                    //save image data to disk
                    String fileName = "IMG_" + id + ".jpg"; //todo storing full filePaths is more logical
                    boolean isValid = saveIncomingImage(fileName,image);

                    if (isValid) {
                        if (!ALLOW_DUPLICATES) {
                            if (VERBOSE) {
                                Log.v(TAG,"adding " + id + " to imagesSeen");
                            }
                            imagesSeen.add(String.valueOf(id));
                        }
                        //         Bundle toSend = new Bundle();
                        values.put(LocalEntry.COLUMN_ID,id);
                        values.put(LocalEntry.COLUMN_FROM_USER,from);
                        values.put(LocalEntry.COLUMN_NAME_TIME,time);
                        values.put(LocalEntry.COLUMN_NAME_LATITUDE,lat);
                        values.put(LocalEntry.COLUMN_NAME_LONGITUDE,lon);
                        values.put(LocalEntry.COLUMN_NAME_FILEPATH,fileName);
                        values.put(LocalEntry.COLUMN_NAME_WEIGHT,weight);

                        if (VERBOSE) {
                            Log.v(" ","Image ID: " + id);
                            Log.v(" ", "from user: " + from);
                            Log.v(" ","Time: " + time);
                            Log.v(" ","Latitude: " + lat);
                            Log.v(" ","Longitude: " + lon);
                            Log.v(" ","Image: " + fileName);
                            Log.v(" ","Weight: " + weight);
                        }

                        try {
                            getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LOCAL,
                                    values);
                        } catch (Exception e) {
                            Log.e(TAG,"error inserting data to database, requesting another...");
                            requestLocalPosts(1,location,rMessenger);
                        }
                    } else {
                        Log.e(TAG,"incoming image was not saved properly...");
                        //todo handle this
                    }

                    jParser.nextValue();
                }
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
            //Toast.makeText(getApplicationContext(),"IOException! response code: " + responseCode,Toast.LENGTH_LONG).show();
            Log.e(TAG, "error requesting images", e);
        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(),"Exception! response code: " + responseCode,Toast.LENGTH_LONG).show();
            Log.d(TAG, "response code " + responseCode);
            Log.e(TAG, "Error sending image to server", e);
        }

        Log.d(TAG, "exiting requestImages...");
    }

    private void requestLocalPostsAsync(final int numberOfImages,final Location location,final Messenger rMessenger) {
        if (isLocalRequesting) {
            if (VERBOSE) Log.v(TAG,"requestLocal is already currently requesting from the server, canceling...");
            return;
        }
        if (VERBOSE) Log.v(TAG,"requestLocal is not currently requesting... starting...");
        isLocalRequesting = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                requestLocalPosts(numberOfImages, location, rMessenger);
                isLocalRequesting = false;
            }
        }).start();
    }


    /**
     * method 'sendImageMessage'
     *
     * sends a message to a specific user specified by UUID
     *
     * @param filePath the location the image is stored which is to be sent
     * @param messageTarget the UUID of the user to send the message to
     * @param orientation whether or not the server should rotate the image
     */
    private void sendImageMessage(String filePath, String messageTarget, int orientation) {
        if (VERBOSE) {
            Log.v(TAG,"entering sendImageMessage...");
        }


        //load image from file
        String image = loadImageForTransit(filePath);
        if (image == null) {
            Log.e(TAG,"image was not process properly for transit");
            return;
        }

        if (VERBOSE) Log.v(TAG,"creating url...");

        URL url = null;
        try {
            url = createURLtoServer(SEND_LOCAL_MESSAGE_PATH);
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURL",e);
            return;
        }

        HttpURLConnection conn = openConnectionToURL(url);
        if (conn == null) {
            //todo broadcast this
            return;
        }

        int responseCode = -1;

        try {

            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream()); //tcp connection to server

            if (VERBOSE) {
                Log.v(TAG, "sending Image message:");
                Log.v(TAG, "from : " + userID.toString());
                Log.v(TAG, "to : " + messageTarget);
                Log.v(TAG, "filePath : " + filePath);
            }

            jGen.writeStartObject();
            jGen.writeStringField(FROM_USER, userID.toString());
            jGen.writeStringField(TO,messageTarget);
            jGen.writeStringField(IMAGE,image);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG,"IOException",e);
        }


        if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "server returned successful response" + responseCode);
            Log.d(TAG, "now deleting image from internal storage...");
            deleteFile(filePath.substring(filePath.lastIndexOf("/")+1,filePath.length()));
        } else {
            Log.e(TAG, "Sending failed for " + url.toString() +
                    " response code: " + responseCode);
        }
    }

   public void sendImageMessageAsync(final String filePath, final String messageTarget, final int orientation) {
       new Thread(new Runnable() {
           @Override
           public void run() {
               android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
               sendImageMessage(filePath, messageTarget, orientation);
           }
       }).start();
   }

    /**
     * method 'requestLocalMessages'
     *
     * sends the current UUID to the server, querying if any new messages are available.
     *
     * all incoming images are saved to the contentprovider's "message_entries" table
     */
    private void requestLocalMessages() {
       if (VERBOSE) {
           Log.v(TAG,"enter requestLocalMessages...");
       }

       URL url = null;
       try {
           url = createURLtoServer(GET_LOCAL_MESSAGES_PATH);
       } catch (MalformedURLException e) {
           Log.e(TAG,"MalformedURL",e);
           return;
       }

       HttpURLConnection conn = openConnectionToURL(url);
        if (conn == null) {
            //todo broadcast this
            return;
        }

       try {
           if (VERBOSE) Log.v(TAG,"opening outputStream to send JSON...");
           JsonFactory jsonFactory = new JsonFactory();
           JsonGenerator jGen = jsonFactory.
                   createGenerator(conn.getOutputStream());
           if (VERBOSE) {
               Log.v(TAG, "from : " + userID.toString());
           }

           jGen.writeStartObject();
           jGen.writeStringField("from", userID.toString());
           jGen.writeEndObject();
           jGen.flush();
           jGen.close();

       } catch (IOException e) {
           Log.e(TAG,"error handling JSON",e);
       }

       try {
           JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());
           int time;
           String from;
           String image;

           if (jParser.nextValue() == JsonToken.START_ARRAY) {
               while (jParser.nextValue() != JsonToken.END_ARRAY) {
                   Log.d(TAG, "incoming image.");

                   ContentValues values = new ContentValues();
                   jParser.nextValue();
                   time = jParser.getValueAsInt();
                   jParser.nextValue();
                   from = jParser.getValueAsString();
                   jParser.nextValue();
                   image = jParser.getValueAsString();

                   //save image data to disk

                   String filePath = getFilesDir() + "/" + "IMG_" + time + ".jpg";
                   boolean isValid = saveIncomingImage(filePath,image);

                   if (isValid) {
                       if (VERBOSE) Log.v(TAG,"image is valid. saving...");

                       values.put(SQLiteDbContract.MessageEntry.COLUMN_NAME_TIME, time);
                       values.put(SQLiteDbContract.MessageEntry.COLUMN_FROM_USER, from);
                       values.put(SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH, filePath);

                       if (VERBOSE) {
                           Log.v(" ", "timestamp: " + time);
                           Log.v(" ", "from user: " + from);
                           Log.v(" ", "image saved to: " + filePath);
                       }
                       getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_MESSAGE, values);
                   }
                   else {
                       Log.e(TAG,"message image failed to decode properly, notifying server...");

                       // This is not an image file.
                       //todo re-request images
                   }

                   jParser.nextValue();
               }
           } else {
               if (VERBOSE) Log.v(TAG,"no messages returned...");
           }
       } catch (IOException e) {
         Log.d(TAG, "error receiving and storing messages...", e);
       }

       if (VERBOSE) Log.v(TAG,"exiting requestLocalMessages...");
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
     *
     * sends a request to the server, to add a user to this client's blocklist
     *
     * @param userToBlock stringified UUID of the user to block
     */
    public void blockLocalUser(String userToBlock){
        if (VERBOSE) {
            Log.v(TAG,"enter blockLocalUser...");
        }
        Log.d(TAG,"Received a request to block user " + userToBlock);

        URL url;
        try {
            url = createURLtoServer(BLOCK_LOCAL_USER_PATH);
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURL",e);
            return;
        }
        HttpURLConnection conn = openConnectionToURL(url);
        if (conn == null) {
            //todo broadcast this
            return;
        }


        int responseCode;
        try {
            if (VERBOSE) Log.v(TAG,"opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "from: " + userID.toString());
                Log.v(TAG, "blocking: " + userToBlock);
            }
            jGen.writeStartObject();
            jGen.writeStringField("from",userID.toString());
            jGen.writeStringField("block", userToBlock);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        }


        if (VERBOSE) {
            Log.v(TAG,"exiting blockLocalUser...");
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
     *
     * opens a connection to the server, and sends the information required to create a thread in
     * live
     *
     * @param name of the poster
     * @param title the title of the thread
     * @param description the description of the thread
     * @param filePath the path of the thread image
     */
    private void createLiveThread(String name, String title, String description, String filePath) {
        if (VERBOSE) Log.v(TAG,"creating live thread...");

        String image = loadImageForTransit(filePath);
        if (image == null) {
            Log.e(TAG,"image failed to load for transit...");
            return;
        }

        URL url = null;
        try {
            url = createURLtoServer(CREATE_LIVE_THREAD_PATH);
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURL",e);
            return;
        }

        if (VERBOSE) Log.v(TAG,"opening connection to server...");

        int responseCode = -1;

        HttpURLConnection conn = openConnectionToURL(url);
        if (conn == null ) {
            //todo broadcast this
            return;
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
            jGen.writeStringField(FROM_USER, userID.toString());
            jGen.writeStringField(TITLE,title);
            jGen.writeStringField(NAME,name);
            jGen.writeStringField(TEXT,description);
            jGen.writeStringField(IMAGE,image);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }


        try {
            String content = conn.getContent().toString();
            Log.d(TAG,"Content returned is : " + content);
        } catch (IOException e) {
            Log.e(TAG,"error getting connection content",e);
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "server returned successful response" + responseCode);
            Log.d(TAG, "now deleting image from internal storage...");

            deleteFile(filePath.substring(filePath.lastIndexOf("/")+1,filePath.length()));
        } else {
            Log.e(TAG, "Sending failed for " + url.toString() +
                    " response code: " + responseCode);
        }
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
     *
     * opens a connection to the server, and sends the information to reply to a thread in live
     *
     * @param threadID id of the thread to reply to
     * @param name of the poster
     * @param description the description of the thread
     * @param filePath the path of the thread image
     */
    private void replyToLiveThread(int threadID, String name, String description, String filePath) {
        if (VERBOSE) Log.v(TAG,"creating live thread...");

        String image = null;
        if (filePath != null) {
           image = loadImageForTransit(filePath);
            if (image == null) {
                Log.e(TAG,"image failed to load for transit...");
                return;
            }
        }

        URL url = null;
        try {
            url = createURLtoServer(REPLY_LIVE_THREAD_PATH);
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURL",e);
            return;
        }

        if (VERBOSE) Log.v(TAG,"opening connection to server...");

        int responseCode = -1;

        HttpURLConnection conn = openConnectionToURL(url);
        if (conn == null) {
            //todo this should broadcast
            return;
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
            jGen.writeStringField(FROM_USER, userID.toString());
            jGen.writeNumberField(THREAD_ID, threadID);
            jGen.writeStringField(NAME,name);
            jGen.writeStringField(TEXT,description);
            jGen.writeStringField(IMAGE,image);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            Log.e(TAG,"IOException",e);
        }


        if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "server returned successful response" + responseCode);
            Log.d(TAG, "now deleting image from internal storage...");
            deleteFile(filePath.substring(filePath.lastIndexOf("/")+1,filePath.length()));
        } else {
            Log.e(TAG, "Sending failed for " + url.toString() +
                    " response code: " + responseCode);
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
     *
     * requests the current thread list from the server
     */
    private void requestLiveThreads() {
        if (VERBOSE) {
            Log.v(TAG,"enter requestLiveThreads...");
        }

        URL url = null;
        try {
            url = createURLtoServer(GET_LIVE_THREAD_LIST);
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURL",e);
            return;
        }

        HttpURLConnection conn = openConnectionToURL(url);


        try {
            if (VERBOSE) Log.v(TAG,"opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "from : " + userID.toString());
            }

            jGen.writeStartObject();
            jGen.writeStringField("from",userID.toString());
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();

        } catch (IOException e) {
            Log.e(TAG,"error handling JSON",e);
        }

        try {
            JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());

            int order;
            int threadID;

            if (jParser.nextValue() == JsonToken.START_ARRAY) {
                while (jParser.nextValue() != JsonToken.END_ARRAY) {
                    Log.d(TAG, "incoming thread.");

                    ContentValues values = new ContentValues();
                    jParser.nextValue();
                    threadID = jParser.getValueAsInt();
                    jParser.nextValue();
                    order = jParser.getValueAsInt();
                    //save image data to disk

                    values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_ID, order);
                    values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_THREAD_ID, threadID);
                    if (VERBOSE) {
                        Log.v(" ", "order" + order);
                        Log.v(" ", "thread number: " + threadID);
                    }
                    getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LIVE_THREAD_LIST, values);

                    //todo will remove this
                    requestThreadInfo(threadID,order);

                    jParser.nextValue();
                }

            } else {
                if (VERBOSE) Log.v(TAG,"no messages returned...");
            }
        } catch (IOException e) {
            Log.d(TAG, "error receiving and storing messages...", e);
        }

        if (VERBOSE) Log.v(TAG,"exiting requestLiveThreads...");
    }

    private void requestLiveThreadsAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestLiveThreads();
            }
        }).start();
    }

    private void requestThreadInfo(int threadID, int position) {
        if (VERBOSE) Log.v(TAG, "requesting live thread info for thread number: " + threadID);
        URL url = null;
        try {
            url = createURLtoServer(GET_LIVE_THREAD_INFO);
        } catch (MalformedURLException e) {
            Log.e(TAG, "error creating url to get live thread info", e);
        }

        HttpURLConnection conn = openConnectionToURL(url);
        if (conn == null) {
            Log.e(TAG, "error opening connection to server... exiting...");
            return;
        }


        try {
            if (VERBOSE) Log.v(TAG, "opening outputStream to send JSON...");
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jGen = jsonFactory.
                    createGenerator(conn.getOutputStream());
            if (VERBOSE) {
                Log.v(TAG, "from : " + userID.toString());
                Log.v(TAG, "thread number : " + threadID);
            }

            jGen.writeStartObject();
            jGen.writeStringField(FROM_USER, userID.toString());
            jGen.writeNumberField(THREAD_ID, threadID);
            jGen.writeEndObject();
            jGen.flush();
            jGen.close();
        } catch (IOException e) {
            Log.e(TAG, "error handling JSON", e);
        }

        try {
            JsonParser jParser = new JsonFactory().createParser(conn.getInputStream());

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> asMap = objectMapper.readValue(jParser, Map.class);

            /**
             * title - thread title (can be empty)
             name - the name of the OP (can be empty)
             text - thread text content of the thread, the comment
             time - unix timestamp of when the thread was created
             replies - number of replies to this thread
             unique - number of unique posters who have posted in this thread
             image - the thread image
             */
/*            String title;
            String name;
            String text;
            int time;
            int replies;
            int unique;
            String image;

            Log.d(TAG, "incoming thread.");
            ContentValues values = new ContentValues();

                jParser.nextValue();
                jParser.nextValue();
                title = jParser.getValueAsString("Heaven is my favorite poster");
                jParser.nextValue();
                name = jParser.getValueAsString("anonymous");
                jParser.nextValue();
                text = jParser.getValueAsString("");
                jParser.nextValue();
                time = jParser.getValueAsInt(9001);
                jParser.nextValue();
                replies = jParser.getValueAsInt(1337);
                jParser.nextValue();
                unique = jParser.getValueAsInt(0);
                jParser.nextValue();
                image = jParser.getValueAsString();*/


            //save image data to disk
            if (VERBOSE) {

                Log.v(TAG, "printing from objectmapper");
                for (Map.Entry<String, Object> entry : asMap.entrySet()) {
                    Log.v(TAG, "entry: " + entry.getKey() + " value: " + String.valueOf(entry.getValue()));
                }

               /* Log.v(TAG,"printing from variables...");
                Log.v(TAG, "title: " + title);
                Log.v(TAG, "thread id: " + threadID);
                Log.v(TAG, "time: " + time);
                Log.v(TAG, "name: " + name);
                Log.v(TAG, "description: " + text);
                Log.v(TAG, "number of replies: " + replies);
                Log.v(TAG, "number of unique posters: " + unique);
                Log.v(TAG, "filepath saved to: " + "not implemented!");
                Log.v(TAG," image string is : " + image);*/
            }

            // String filePath = getFilesDir() + "/" + "IMG_" + threadID + ".jpg";
            boolean isValid = true;
            /*if (image != null) {
                isValid = saveLiveImage(position, image);
            } else {
                Log.d(TAG, "incoming image was null...");
                isValid = false;
            }

            if (isValid) {
                Log.d(TAG,"Image was valid... inserting into database...");
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_TITLE, title);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_THREAD_ID, threadID);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_TIME, time);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_NAME, name);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_DESCRIPTION, text);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_REPLIES, replies);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_UNIQUE, unique);
                values.put(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_FILEPATH, "Not implemented!");

                getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LIVE_THREAD_INFO, values);
            }*/

        } catch (JsonMappingException e) {
            Log.e(TAG,"Error mapping data",e);
            //ObjectMapper objMap = new ObjectMapper();
            //Integer i = objMap.readValue(jsonParser,Integer.class);
        } catch (IOException e) {
            Log.e(TAG, "error receiving and storing messages...", e);
        }


        if (VERBOSE) Log.v(TAG,"exiting requestLiveThreads...");
    }


    private void requestThreadInfoAsync(final int threadID, final int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestThreadInfo(threadID, position);
            }
        }).start();
    }



    /***********************************************************************************************
     *
     *  SERVICE - ACTIVITY COMMUNICATION
     *
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

    static final int MSG_BUILD_CLIENT = 1;

    static final int MSG_CONNECT_CLIENT = 2;

    static final int MSG_DISCONNECT_CLIENT = 3;

    static final int MSG_REQUEST_CONSTANT_UPDATES = 4;

    static final int MSG_SEND_IMAGE = 5;

    static final int MSG_REQUEST_IMAGES= 6;

    static final int MSG_RESOLVE_HOST = 7;

    static final int MSG_DELETE_IMAGE = 8;

    static final int MSG_REQUEST_MESSAGES = 9;

    static final int MSG_BLOCK_USER = 10;

    static final int MSG_CREATE_THREAD = 11;

    static final int MSG_REPLY_TO_THREAD = 12;

    static final int MSG_REQUEST_THREAD_LIST = 13;

    static final int MSG_REQUEST_THREAD_INFO = 14;
    /**
     * class 'IncomingHandler'
     *
     * Handler of incoming messages from clients.
     * location updates are managed in the current thread, sending and receiving images
     * is handled in a new thread
     *
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
            Log.d(TAG,"enter handleMessage");
            Messenger replyMessenger = msg.replyTo;
            Bundle data;

            if (getLooper() == getMainLooper()) {
                Log.i(TAG, "this handler is on the main thread...");
            } else {
                Log.i(TAG, "this handler is in its own thread...");
            }

            switch (msg.what) {
                case MSG_BUILD_CLIENT:
                        irs.get().buildGoogleApiClient();
                    break;

                case MSG_CONNECT_CLIENT:
                    Log.d(TAG,"connecting client");
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
                    Log.d(TAG, "creating new thread to send image...");
                    data = msg.getData();
                    String filePath = data .getString(Constants.IMAGE_FILEPATH);
                    String messageTarget = data.getString(Constants.MESSAGE_TARGET);
                    Log.d(TAG, "image is stored at " + filePath);
                    if (messageTarget == null) {
                        Log.d(TAG,"posting to local...");
                        irs.get().postImageToLocal(filePath, mLocation, replyMessenger, msg.arg1);
                    } else {
                        Log.d(TAG,"sending Message to user: " + messageTarget);
                        irs.get().sendImageMessageAsync(filePath, messageTarget, msg.arg1);
                    }

                    break;

                case MSG_REQUEST_IMAGES:
                    Log.d(TAG, "received a message to request images.");

                    int count = msg.getData().getInt(Constants.IMAGE_COUNT);
                    irs.get().requestLocalPostsAsync(count, mLocation, replyMessenger);

                    break;

                case MSG_RESOLVE_HOST:
                    Log.d(TAG,"request to resolve host received");
                    String address = msg.getData().getString("hostname");

                        if (VERBOSE) {
                            Log.v(TAG, "attempting to resolve: " + address);
                        }

                    try {
                        InetAddress addr = InetAddress.getByName(address);
                        serverAddress = addr.getHostAddress();
                    } catch (UnknownHostException e) {
                        Log.e(TAG,"DNS resolution failed",e);
                    }

                    break;

                case MSG_DELETE_IMAGE:
                    Log.d(TAG, "received a message to delete image from database");

                    String s = msg.getData().getString(Constants.IMAGE_FILEPATH);
                    throw new NotYetConnectedException();

                case MSG_REQUEST_MESSAGES:
                    Log.d(TAG,"received a message to message a user");
                    irs.get().requestLocalMessagesAsync();
                    break;

                case MSG_BLOCK_USER:
                    Log.d(TAG,"received a message to block a user");
                    irs.get().blockLocalUserAsync(msg.getData().getString(Constants.MESSAGE_TARGET));
                    break;

                case MSG_CREATE_THREAD:
                    Log.d(TAG,"recevied a message to create a thread");

                    data = msg.getData();
                    String name = data.getString("name");
                    String title = data.getString("title");
                    String description = data.getString("description");
                    String path = data.getString("filePath");

                    irs.get().createLiveThreadAsync(name, title, description, path);
                    break;

                case MSG_REPLY_TO_THREAD:
                    Log.d(TAG,"recevied a message to create a thread");

                    data = msg.getData();
                    String namea = data.getString("name");
                    String desc = data.getString("description");
                    String fpath = data.getString("filePath");
                    irs.get().replyToLiveThreadAsync(msg.arg1, namea, desc, fpath);
                    break;

                case MSG_REQUEST_THREAD_LIST:
                    Log.d(TAG, "received a message to request the thread list");
                    irs.get().requestLiveThreadsAsync();
                    break;

                case MSG_REQUEST_THREAD_INFO:
                    Log.d(TAG, "received a message to request thread info");

                    break;

                default:
                    super.handleMessage(msg);
            }


            Log.d(TAG,"exit handleMessage...");
        }
    }


    //final Messenger mMessenger = new Messenger(new IncomingHandler().setParent(this));
    final Messenger mMessenger = new Messenger(new ServiceHandlerThread(this).getIncomingHandler());
    @Override
    public IBinder onBind(Intent intent) {
            return mMessenger.getBinder();
    }


    /**********************************************************************************************
     * Convenience methods
     *
     * these methods are created to perform common tasks throughout the service
     */

    /**
     * method 'loadImageForTransit'
     *
     * loads bitmap at selected filePath from storage, gzips and base64's it, so that it may be
     * sent as part of a JSON object
     *
     * @param filePath location of the image to load
     * @return processed image ready to be sent to the server
     */
    public String loadImageForTransit(String filePath) {
        if (VERBOSE) {
            Log.v(TAG,"loading image for transit from filePath " + filePath);
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
        if (VERBOSE) Log.v(TAG,"Saving incoming with file name: " + fileName);

        try {
            FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            if (VERBOSE) {
                Log.v(TAG,"saving image to file " + fos.getFD());
            }

            byte[] data = theImage.getBytes();
            ByteArrayInputStream byis = new ByteArrayInputStream(data);
            Base64InputStream bos = new Base64InputStream(byis, Base64.DEFAULT);
            GzipCompressorInputStream gzis = new GzipCompressorInputStream(bos);
            IOUtils.copy(gzis, fos);
            fos.flush();
            fos.close();

            if (VERBOSE) Log.v(TAG,"validating image...");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);
            if (options.outWidth == -1 && options.outHeight == -1) {
                Log.e(TAG,"incoming image was not valid...");
                deleteFile(fileName);
                return false;
            }

        } catch (IOException e) {
            Log.e(TAG,"error saving incoming image to internal storage...",e);
            return false;

        }

        if (VERBOSE) Log.v(TAG,"image successfully saved");
        return true;
    }

    /**
     * method 'saveLiveImage'
     */
    private boolean saveLiveImage(int imageID, String theImage) {
        return saveIncomingImage(Uri.withAppendedPath(FireFlyContentProvider
                .CONTENT_URI_LIVE_THREAD_INFO, String.valueOf(imageID)), theImage);
    }

    /**
     * method 'saveIncomingImage'
     *
     * @param destination the location uri for the image to be stored
     * @param theImage the base64'd stringified image
     * @return whether or not the save was successful
     */
    public boolean saveIncomingImage(Uri destination, String theImage) {
        if (VERBOSE) Log.v(TAG,"Saving incoming to uri" + destination.toString());

        try {
            FileOutputStream fos = (FileOutputStream)getContentResolver()
                    .openOutputStream(destination, "w");
            if (VERBOSE) {
                Log.v(TAG,"saving image to file " + fos.getFD());
            }

            byte[] data = theImage.getBytes();
            ByteArrayInputStream byis = new ByteArrayInputStream(data);
            Base64InputStream bos = new Base64InputStream(byis, Base64.DEFAULT);
            GzipCompressorInputStream gzis = new GzipCompressorInputStream(bos);
            IOUtils.copy(gzis, fos);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            Log.e(TAG,"error saving incoming image to internal storage...",e);
            return false;

        }

        if (VERBOSE) Log.v(TAG,"image successfully saved");
        return true;
    }

    /**
     * method 'openConnectionToURL'
     *
     * opens and sets the parameters to the given URL
     *
     * @param url the url to connect to
     * @return the prepared URL for input and output, or null if an error occurred
     */
    private HttpURLConnection openConnectionToURL(URL url) {
        URLConnection urlconn;

        try {
            urlconn = url.openConnection();
        } catch (IOException e) {
            Log.e(TAG,"error opening connection to url... exiting....");
            //todo readd to message queue
            return null;
        }

        if (VERBOSE) Log.v(TAG,"connection opened to " + urlconn.toString());

        HttpURLConnection conn = (HttpURLConnection) urlconn;
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);

        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            Log.e(TAG,"failed to set protocol ot post... exiting...");
            //todo readd to message quere
            return null;
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setUseCaches(false);

        return conn;
    }

    /**
     * method "createURLtoServer"
     *
     * creates a URL to the server with the provided directory
     * this is used because all the urls have the same protocol address and socket
     *
     * @param path the serverDirectory path to point to
     * @return the created URL
     * @throws MalformedURLException
     */
    private URL createURLtoServer(String path) throws MalformedURLException{
        if (VERBOSE) Log.v(TAG,"creating to " + path);
                return new URL(CONNECTION_PROTOCOL,serverAddress,SERVER_SOCKET,path);
    }

    /***********************************************************************************************
     * LOCATION HANDLING
     *
     */
    /**
     * method 'buildGoogleApiClient'
     *
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
     *
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
     *
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
            Toast.makeText(this,"Google play services is missing or out of date", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,"connection to location services failed... " + result, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended...");

        if (VERBOSE) {
            Toast.makeText(this,"Location connection suspended",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG,"new location aquired...");
        mLocation = location;
        if (VERBOSE) {
            Log.v(TAG,"new location coordinates: " + location.getLatitude() + " " + location.getLongitude());
            Toast.makeText(getApplicationContext(),"onLocationChanged called",Toast.LENGTH_SHORT).show();
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
    public class ImageFileFilter implements FileFilter
    {
        private final String[] okFileExtensions =
                new String[] {"jpg", "bmp", "png"};

        public boolean accept(File file)
        {
            for (String extension : okFileExtensions)
            {
                if (file.getName().toLowerCase().endsWith(extension))
                {
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


}


