package com.jokrapp.android;

import java.util.Locale;

/**
 * @author John C. Quinn
 *
 */

public final class Constants {
    //modes
    public static boolean client_only_mode = false;


    // Defines a custom Intent action

    public static final String IMAGE_COUNT = "count";
    public static final String MESSAGE_TARGET = "msg";
    public static final String IMAGE_FILEPATH = "filepath";

    public static final String SERVER_URL = "jokrbackend.ddns.net";


    public static final int MAX_STORED = 5; //max images stored in databse
    public static final int IDEAL_WIDTH = 1280;
    public static final int IDEAL_HEIGHT = 720;

    public static final String SHARED_PREFERENCES_NAME = "settings";


    public static final String ACTION_IMAGE_LOADED = "com.jokrapp.android.IMAGELOADED";
    public static final String ACTION_IMAGE_LIVE_LOADED = "com.jokrapp.android.live.LOADED";
    public static final String ACTION_IMAGE_REPLY_LOADED = "com.jokrapp.android.reply.LOADED";
    public static final String ACTION_IMAGE_REPLY_THUMBNAIL_LOADED = "com.jokrapp.android.reply.thumbnail.LOADED";
    public static final String ACTION_IMAGE_LOCAL_LOADED = "com.jokrapp.android.local.LOADED";
    public static final String ACTION_IMAGE_MESSAGE_LOADED = "com.jokrapp.android.message.LOADED";
    public static final String LOCAL_KEY_PATH = "pkl";

    /** threadSample
     */
    // Set to true to turn on verbose logging
    public static final boolean LOGV = true;

    // Set to true to turn on debug logging
    public static final boolean LOGD = true;

    // Custom actions

    public static final String ACTION_VIEW_IMAGE =
            "com.jokrapp.android.ACTION_VIEW_IMAGE";

    public static final String ACTION_ZOOM_IMAGE =
            "com.jokrapp.android.ACTION_ZOOM_IMAGE";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "com.jokrapp.android.BROADCAST";

    // Fragment tags
    public static final String PHOTO_FRAGMENT_TAG =
            "com.jokrapp.android.PHOTO_FRAGMENT_TAG";

    public static final String THUMBNAIL_FRAGMENT_TAG =
            "com.jokrapp.android.THUMBNAIL_FRAGMENT_TAG";

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "com.jokrapp.android.STATUS";

    // Defines the key for the log "extra" in an Intent
    public static final String EXTENDED_STATUS_LOG = "com.jokrapp.android.LOG";

    // Defines the key for storing fullscreen state
    public static final String EXTENDED_FULLSCREEN =
            "com.jokrapp.android.EXTENDED_FULLSCREEN";

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
    public static final String KEY_ANALYTICS_LABEL = "label";
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

    public static final String ANALYTICS_CATEGORY_ERROR = "error";
}
