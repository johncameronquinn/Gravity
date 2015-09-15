package com.jokrapp.android;

import android.provider.BaseColumns;

/**
 * class 'SQLiteDbContact'
 *
 * Created by John C. Quinn on 5/21/15.
 */

public final class SQLiteDbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public SQLiteDbContract() {
    }

    /* Inner class that defines the table contents */
    public static abstract class LocalEntry implements BaseColumns {
        public static final String TABLE_NAME = "local_entries";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_FROM_USER = "userID";
        public static final String COLUMN_NAME_WEIGHT = "weight";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_LATITUDE = "lat";
        public static final String COLUMN_NAME_LONGITUDE = "lng";
        public static final String COLUMN_NAME_FILEPATH = "filepath";
    }

    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message_entries";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_FROM_USER = "userID";
        public static final String COLUMN_NAME_FILEPATH = "filepath";
    }

    public static abstract class LiveThreadEntry implements BaseColumns {
        public static final String TABLE_NAME = "live_entries";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_THREAD_ID = "threadID";
    }

    public static abstract class LiveThreadInfoEntry implements BaseColumns {
        public static final String TABLE_NAME = "live_info_entries";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME_THREAD_ID = "threadID";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "desc";
        public static final String COLUMN_NAME_UNIQUE = "posters";
        public static final String COLUMN_NAME_REPLIES = "replies";
        public static final String COLUMN_NAME_FILEPATH = "filepath";
    }


    public static abstract class LiveReplies implements BaseColumns {
        public static final String TABLE_NAME = "live_replies";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME_THREAD_ID = "threadID";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "desc";
        public static final String COLUMN_NAME_FILEPATH = "filepath";
    }


}

