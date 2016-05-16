package us.gravwith.android;

import java.util.Locale;

/**
 * @author John C. Quinn
 *
 */

public final class Constants {

    /* these switches disable/enable functionality, and are for debugging ONLY */
    /**
     * this mode will instruct the client to loopback all posts to itself.
     * any content creation will be immediately inserted into its own contentprovider
     *
     * the background process is still started, but all requests will be ignored
     */
    public static boolean client_only_mode = false;

    /**
     * this mode will manually instruct the client not connect to the sandbox EC2 instance
     *
     * normally, every debug build is automatically sandboxed
     */
    public static boolean SANDBOX_MODE = false;


    // Defines a custom Intent action

    public static final String IMAGE_COUNT = "count";
    public static final String MESSAGE_TARGET = "msg";
    public static final String IMAGE_FILEPATH = "filepath";

    public static final int MAX_STORED = 5; //max images stored in databse
    public static final int IDEAL_WIDTH = 1280;
    public static final int IDEAL_HEIGHT = 720;

    public static final String SHARED_PREFERENCES_NAME = "settings";


    public static final String ACTION_IMAGE_LOADED = "us.gravwith.android.IMAGELOADED";
    public static final String ACTION_IMAGE_LIVE_LOADED = "us.gravwith.android.live.LOADED";
    public static final String ACTION_IMAGE_REPLY_LOADED = "us.gravwith.android.reply.LOADED";
    public static final String ACTION_IMAGE_REPLY_THUMBNAIL_LOADED = "us.gravwith.android.reply.thumbnail.LOADED";
    public static final String ACTION_IMAGE_LOCAL_LOADED = "us.gravwith.android.local.LOADED";
    public static final String ACTION_IMAGE_MESSAGE_LOADED = "us.gravwith.android.message.LOADED";
    public static final String LOCAL_KEY_PATH = "pkl";
    public static final String KEY_USER_ID = "userID";
    public static final String KEY_IDENTITY_ID = "identityId";
    public static final String KEY_CONTENT_ID = "id";

    public static final String STASH_GALLERY_DIRECTORY = "Stash_Gallery";

    public static final String IS_FROM_NOTIFICATION =  "isNotification";


    /** threadSample
     */
    // Set to true to turn on verbose logging
    public static final boolean LOGV = false;

    // Set to true to turn on debug logging
    public static final boolean LOGD = false;

    public static final boolean ANALYTICSV = false;

    public static final boolean AUTHENTICATIONV = false;

    // Custom actions

    public static final String ACTION_VIEW_IMAGE = "us.gravwith.android.ACTION_VIEW_IMAGE";

    public static final String ACTION_REMOVE_IMAGE = "us.gravwith.android.ACTION_REMOVE_IMAGE";

    public static final String ACTION_ZOOM_IMAGE = "us.gravwith.android.ACTION_ZOOM_IMAGE";


    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "com.gravwith.android.BROADCAST";

    // Fragment tags
    public static final String PHOTO_FRAGMENT_TAG =
            "com.gravwith.android.PHOTO_FRAGMENT_TAG";

    public static final String THUMBNAIL_FRAGMENT_TAG =
            "com.gravwith.android.THUMBNAIL_FRAGMENT_TAG";

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "com.gravwith.android.STATUS";

    // Defines the key for the log "extra" in an Intent
    public static final String EXTENDED_STATUS_LOG = "com.gravwith.android.LOG";

    // Defines the key for storing fullscreen state
    public static final String EXTENDED_FULLSCREEN =
            "com.gravwith.android.EXTENDED_FULLSCREEN";

    /*
     * A user-agent string that's sent to the HTTP site. It includes information about the device
     * and the build that the device is running.
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android "
            + android.os.Build.VERSION.RELEASE + ";"
            + Locale.getDefault().toString() + "; " + android.os.Build.DEVICE
            + "/" + android.os.Build.ID + ")";

    // Status values to broadcast to the Activity

    // The download is starting
    public static final int STATE_ACTION_STARTED = 0;

    // The background thread is connecting to the RSS feed
    public static final int STATE_ACTION_CONNECTING = 1;

    // The background thread is parsing the RSS feed
    public static final int STATE_ACTION_PARSING = 2;

    // The background thread is writing data to the content provider
    public static final int STATE_ACTION_WRITING = 3;

    // The background thread is done
    public static final int STATE_ACTION_COMPLETE = 4;

    // The background thread is doing logging
    public static final int STATE_LOG = -1;

    public static final CharSequence BLANK = " ";

    public static final String KEY_TEXT = "text";

    /**
     * S3 KEY TAGS
     */
    public static final String KEY_S3_KEY = "key";
    public static final String KEY_S3_DIRECTORY = "directory";
    public static final String KEY_S3_LIVE_DIRECTORY = "live";
    public static final String KEY_S3_LOCAL_DIRECTORY = "local";
    public static final String KEY_S3_STASH_DIRECTORY = "stash";
    public static final String KEY_S3_MESSAGE_DIRECTORY = "message";
    public static final String KEY_S3_REPLIES_DIRECTORY = "reply";

    public static final String KEY_PREVIEW_IMAGE = "preview_status";

    /**
     * ERROR HANDLING
     */

    public static final String RESPONSE_CODE = "responseCode";
    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String RESPONSE_BUNDLE = "responseBundle";

    public static final String ACTION_SERVER_ERROR = "us.gravwith.android.ACTION_SERVER_ERROR";

    /**
     * ANALYTICS
     */

    public static final int LOCAL_BLOCK_EVENT= 101;
    public static final int LOCAL_MESSAGE_EVENT= 102;
    public static final int LIVE_THREADVIEW_EVENT = 103;
    public static final int REPLY_VIEW_EVENT = 104;
    public static final int FRAGMENT_VIEW_EVENT = 105;

    public static final String KEY_ANALYTICS_CATEGORY = "category";
    public static final String KEY_ANALYTICS_ACTION = "action";
    public static final String KEY_ANALYTICS_RESOURCE = "resource";

    public static final String KEY_ANALYTICS_VALUE = "value";
    public static final String KEY_ANALYTICS_VARIABLE = "variable";

    public static final String ANALYTICS_CATEGORY_LIFECYCLE = "lifecycle";
    public static final String ANALYTICS_CATEGORY_LOCAL = "local";
    public static final String ANALYTICS_CATEGORY_LIVE = "live";
    public static final String ANALYTICS_CATEGORY_MESSAGE = "message";
    public static final String ANALYTICS_CATEGORY_REPLY = "reply";
    public static final String ANALYTICS_CATEGORY_CAMERA = "camera";

    public static final String ANALYTICS_LABEL_MESSAGE ="message";
    public static final String ANALYTICS_CATEGORY_SCREEN ="screen";
    public static final String ANALYTICS_ATTRIBUTE_USER_ID="userID";
    public static final String ANALYTICS_ATTRIBUTE_DEVICE_ID="androidID";
    public static final String ANALYTICS_ATTRIBUTE_SESSION_TOKEN="sessionToken";

    public static final String SCREEN_TITLE = "Screen Title";

    public static final String ANALYTICS_CATEGORY_ERROR = "error";
    public static final String ANALYTICS_ERROR_METHOD = "ErrorMethod";
    public static final String ANALYTICS_ERROR_MESSAGE = "ErrorMessage";
}
