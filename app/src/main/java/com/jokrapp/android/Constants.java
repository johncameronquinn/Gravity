package com.jokrapp.android;

import java.util.Locale;

/**
 * @author John C. Quinn
 *
 */

public final class Constants {
    // Defines a custom Intent action

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS =
            "com.example.android.threadsample.STATUS";

    public static final String IMAGE_COUNT = "count";
    public static final String MESSAGE_TARGET = "msg";
    public static final String IMAGE_FILEPATH = "filepath";


    public static final int MAX_STORED = 5; //max images stored in databse
    public static final int IDEAL_WIDTH = 1280;
    public static final int IDEAL_HEIGHT = 720;


    /*
     * A user-agent string that's sent to the HTTP site. It includes information about the device
     * and the build that the device is running.
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android "
            + android.os.Build.VERSION.RELEASE + ";"
            + Locale.getDefault().toString() + "; " + android.os.Build.DEVICE
            + "/" + android.os.Build.ID + ")";
}
