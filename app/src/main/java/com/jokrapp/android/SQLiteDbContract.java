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
        public static final String COLUMN_FROM_USER = "fromUser";
        public static final String COLUMN_NAME_WEIGHT = "weight";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_RESPONSE_ARN = "arn";
        public static final String COLUMN_NAME_FILEPATH = "url";
    }

    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message_entries";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_FROM_USER = "fromUser";
        public static final String COLUMN_RESPONSE_ARN = "arn";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_FILEPATH = "url";
    }

    public static abstract class LiveEntry implements BaseColumns {
        public static final String TABLE_NAME = "live_entries";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME_THREAD_ID = "threadID";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "text";
        public static final String COLUMN_NAME_UNIQUE = "uniq";
        public static final String COLUMN_NAME_REPLIES = "replies";
        public static final String COLUMN_NAME_FILEPATH = "url";
    }


    public static abstract class LiveReplies implements BaseColumns {
        public static final String TABLE_NAME = "live_replies";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME_THREAD_ID = "threadID";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "text";
        public static final String COLUMN_NAME_FILEPATH = "url";
    }

    public static abstract class StashEntry implements BaseColumns {
        public static final String ROW_ID = BaseColumns._ID;

        public static final String PICTUREURL_TABLE_NAME = "PictureUrlData";
        public static final String IMAGE_THUMBURL_COLUMN = "ThumbUrl";
        public static final String IMAGE_THUMBNAME_COLUMN = "ThumbUrlName";

        public static final String IMAGE_URL_COLUMN = "ImageUrl";
        public static final String IMAGE_PICTURENAME_COLUMN = "ImageName";

        public static final String DATE_TABLE_NAME = "DateMetadataData";
        public static final String DATA_DATE_COLUMN = "DownloadDate";
    }



}
