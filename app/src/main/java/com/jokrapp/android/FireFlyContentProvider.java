package com.jokrapp.android;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.NotYetConnectedException;

/**
 * class 'FireFlyContentProvider'
 *
 * contentProvider that manages all storage for this application
 */
public class FireFlyContentProvider extends ContentProvider {

    private static String AUTHORITY = "com.jokrapp.android.provider";
    private static String TAG = "FireFlyContentProvider";


    private static final String LOCAL_BASE_PATH = "local";

    private static final String MESSAGE_BASE_PATH = "message";

    private static final String LIVE_BASE_PATH = "live";

    private static final String LIVE_INFO_BASE_PATH = "liveinfo";

    private static final String REPLY_BASE_PATH = "replies";

    private static final String REPLY_INFO_BASE_PATH = "replyinfo";



    public static final Uri CONTENT_URI_LOCAL = Uri.parse("content://" + AUTHORITY
            + "/" + LOCAL_BASE_PATH);

    public static final Uri CONTENT_URI_MESSAGE = Uri.parse("content://" + AUTHORITY
            + "/" + MESSAGE_BASE_PATH);

    public static final Uri CONTENT_URI_LIVE_THREAD_LIST = Uri.parse("content://" + AUTHORITY
            + "/" + LIVE_BASE_PATH);

    public static final Uri CONTENT_URI_LIVE_THREAD_INFO = Uri.parse("content://" + AUTHORITY
            + "/" + LIVE_INFO_BASE_PATH);

    public static final Uri CONTENT_URI_REPLY_THREAD_LIST = Uri.parse("content://" + AUTHORITY
            + "/" + REPLY_BASE_PATH);

    public static final Uri CONTENT_URI_REPLY_THREAD_INFO = Uri.parse("content://" + AUTHORITY
            + "/" + REPLY_INFO_BASE_PATH);


    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/images";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/image";


    SQLiteDbHelper database;


    private static final int LOCAL = 10;
    private static final int LOCAL_ID = 20;
    private static final int LIVE = 30;
    private static final int LIVE_ID = 40;
    private static final int LIVE_INFO = 50;
    private static final int LIVE_INFO_ID = 60;
    private static final int REPLIES = 70;
    private static final int REPLIES_ID = 80;
    private static final int REPLIES_INFO = 90;
    private static final int REPLIES_INFO_ID = 100;
    private static final int MESSAGE = 110;
    private static final int MESSAGE_ID = 120;


    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, LOCAL_BASE_PATH, LOCAL);
        sURIMatcher.addURI(AUTHORITY, LOCAL_BASE_PATH + "/#", LOCAL_ID);

        sURIMatcher.addURI(AUTHORITY, MESSAGE_BASE_PATH, MESSAGE);
        sURIMatcher.addURI(AUTHORITY, MESSAGE_BASE_PATH + "/#", MESSAGE_ID);

        sURIMatcher.addURI(AUTHORITY, LIVE_BASE_PATH, LIVE);
        sURIMatcher.addURI(AUTHORITY, LIVE_BASE_PATH + "/#", LIVE_ID);

        sURIMatcher.addURI(AUTHORITY, LIVE_INFO_BASE_PATH, LIVE_INFO);
        sURIMatcher.addURI(AUTHORITY, LIVE_INFO_BASE_PATH + "/#", LIVE_INFO_ID);

        sURIMatcher.addURI(AUTHORITY, REPLY_BASE_PATH, REPLIES);
        sURIMatcher.addURI(AUTHORITY, REPLY_INFO_BASE_PATH + "/#", REPLIES_ID);

    }


    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {

        throw new NotYetConnectedException();
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int uritype = sURIMatcher.match(uri);

        /*
         Selects the director to open the file to
         */
        String directory;
        switch (uritype) {
            case LOCAL_ID:
                Log.d(TAG,"LOCAL_ID called");
                directory = LOCAL_BASE_PATH;
                break;

            case LIVE_INFO_ID:
                Log.d(TAG,"LIVE_INFO_ID called");
                directory = LIVE_INFO_BASE_PATH;
                break;

            case MESSAGE_ID:
                Log.d(TAG,"MESSAGE_ID called");
                directory = MESSAGE_BASE_PATH;
                break;

            case REPLIES_ID:
                Log.d(TAG,"REPLY_ID called");
                directory = REPLY_INFO_BASE_PATH;
                break;

            default:
                Log.d(TAG,"uriType is " + uritype);
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }


        File root = getContext().getCacheDir();
        File path = new File(root, directory);
        path.mkdirs();
        File file = new File(path,uri.getLastPathSegment());

        int imode = 0;
        if (mode.contains("w")) {
            imode |= ParcelFileDescriptor.MODE_WRITE_ONLY;
            if (!file.exists()) {
                try {
                    Log.d(TAG,"creating new file at " + file.toString());
                    file.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG,"error creating new file at " + file.toString(),e);
                }
            } else {
                try {
                    Log.d(TAG,"deleting and recreating file at " + file.toString()); //todo is this necessary
                    file.delete();
                    file.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG,"error creating new file at " + file.toString(),e);
                }
            }
        }
        if (mode.contains("r"))
            imode |= ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode.contains("+"))
            imode |= ParcelFileDescriptor.MODE_APPEND;

        Log.d(TAG,"opening file to " + file.toString() + " with mode " + mode);
        return ParcelFileDescriptor.open(file, imode);
    }



    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG,"handling query for " + uri.toString());
        // Using SQLiteQueryBuilder instead of query() method

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        int uriType = sURIMatcher.match(uri);

        switch (uriType) {
            case LOCAL:
                Log.d(TAG,"LOCAL called");
                queryBuilder.setTables(SQLiteDbContract.LocalEntry.TABLE_NAME);
                break;
            case LOCAL_ID:
                Log.d(TAG,"LOCAL_ID called");
                // adding the ID to the original query
                queryBuilder.appendWhere(SQLiteDbContract.LocalEntry.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;

            case MESSAGE:
                Log.d(TAG,"MESSAGE called");
                queryBuilder.setTables(SQLiteDbContract.MessageEntry.TABLE_NAME);
                break;
            case MESSAGE_ID:
                Log.d(TAG, "MESSAGE_ID called");

                queryBuilder.appendWhere(SQLiteDbContract.MessageEntry.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;

            case LIVE:
                Log.d(TAG,"LIVE called");
                queryBuilder.setTables(SQLiteDbContract.LiveThreadEntry.TABLE_NAME);
                break;
            case LIVE_ID:
                Log.d(TAG, "LIVE_ID called");
                queryBuilder.appendWhere(SQLiteDbContract.LiveThreadEntry.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;

            case LIVE_INFO:
                Log.d(TAG,"LIVE_INFO called");
                queryBuilder.setTables(SQLiteDbContract.LiveThreadInfoEntry.TABLE_NAME);
            break;
            case LIVE_INFO_ID:
                Log.d(TAG, "LIVE_INFO_ID called");

                queryBuilder.appendWhere(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;

            case REPLIES:
                Log.d(TAG,"REPLY_INFO called");
                queryBuilder.setTables(SQLiteDbContract.LiveReplies.TABLE_NAME);
                break;
            case REPLIES_ID:
                queryBuilder.appendWhere(SQLiteDbContract.LiveReplies.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG,"handling delete from " + uri.toString());

        // Implement this to handle requests to delete one or more rows.
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted;

        String id;

        switch (uriType) {

            case LOCAL:
                rowsDeleted = sqlDB.delete(SQLiteDbContract.LocalEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case LOCAL_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LocalEntry.TABLE_NAME,
                            SQLiteDbContract.LocalEntry.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LocalEntry.TABLE_NAME,
                            SQLiteDbContract.LocalEntry.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;

            case MESSAGE:
                rowsDeleted = sqlDB.delete(SQLiteDbContract.MessageEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case MESSAGE_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.MessageEntry.TABLE_NAME,
                            SQLiteDbContract.MessageEntry.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.MessageEntry.TABLE_NAME,
                            SQLiteDbContract.MessageEntry.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;

            case LIVE:
                rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveThreadEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;

            case LIVE_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveThreadEntry.TABLE_NAME,
                            SQLiteDbContract.LiveThreadEntry.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveThreadEntry.TABLE_NAME,
                            SQLiteDbContract.LiveThreadEntry.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;

            case LIVE_INFO:
                rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveThreadInfoEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;

            case LIVE_INFO_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveThreadInfoEntry.TABLE_NAME,
                            SQLiteDbContract.LiveThreadInfoEntry.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveThreadInfoEntry.TABLE_NAME,
                            SQLiteDbContract.LiveThreadInfoEntry.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;

            case REPLIES:
                rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveReplies.TABLE_NAME, selection,
                        selectionArgs);
                break;

            case REPLIES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveReplies.TABLE_NAME,
                            SQLiteDbContract.LiveReplies.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SQLiteDbContract.LiveReplies.TABLE_NAME,
                            SQLiteDbContract.LiveReplies.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new NotYetConnectedException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case LOCAL:
                id = sqlDB.insert(SQLiteDbContract.LocalEntry.TABLE_NAME, null, values);
                break;

            case MESSAGE:
                id = sqlDB.insert(SQLiteDbContract.MessageEntry.TABLE_NAME, null, values);
                break;

            case LIVE:
                //if there is a thread at this specific thread's position, delete it first
                //sqlDB.delete(SQLiteDbContract.LiveThreadEntry.TABLE_NAME,
                  //      "_id" + "=?",
                    //   new String[]{values.getAsString(SQLiteDbContract.LiveThreadEntry.COLUMN_ID)});

                String table = SQLiteDbContract.LiveThreadEntry.TABLE_NAME;
                String whereClause = "1";
                String[] whereArgs = new String[] { String.valueOf(values.getAsString(SQLiteDbContract.LiveThreadEntry.COLUMN_ID)) };
                //Log.d(TAG, "num returned..." + sqlDB.delete(table, whereClause, whereArgs));
               // sqlDB.execSQL("DELETE FROM live_entries WHERE _id = ?" + values.getAsString(SQLiteDbContract.LiveThreadEntry.COLUMN_ID));
                //Log.d(TAG,"Running delete for " + values.getAsString(SQLiteDbContract.LiveThreadEntry.COLUMN_ID));

                Log.d(TAG,"deleting" + sqlDB.delete(table,whereClause,null) + " rows...");
                id = sqlDB.insert(SQLiteDbContract.LiveThreadEntry.TABLE_NAME,null,values);

       //         String sql = "INSERT OR REPLACE INTO " + table + " (";
         //       for (String i : whereArgs) {
           //         sql = sql + i +", ";
             //   }
               // sql = sql + ") VALUES (";



               // sqlDB.execSQL(sql);
                break;

            case LIVE_INFO:
                //delete all threads with the specified thread ID prior to insertion
                sqlDB.delete(SQLiteDbContract.LiveThreadInfoEntry.TABLE_NAME,
                        SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_THREAD_ID + "=?",
                        new String[]{
                                values.getAsString(SQLiteDbContract.LiveThreadInfoEntry.COLUMN_NAME_THREAD_ID)
                        });

                //now insert
                id = sqlDB.insert(SQLiteDbContract.LiveThreadInfoEntry.TABLE_NAME, null, values);
                break;

            case REPLIES:
                id = sqlDB.insert(SQLiteDbContract.LiveReplies.TABLE_NAME, null, values);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(uriType + "/" + id);
    }

    @Override
    public boolean onCreate() {
        database = new SQLiteDbHelper(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) { int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case LOCAL:
                rowsUpdated = sqlDB.update(SQLiteDbContract.LocalEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case LOCAL_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(SQLiteDbContract.LocalEntry.TABLE_NAME,
                            values,
                            SQLiteDbContract.LocalEntry.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(SQLiteDbContract.LocalEntry.TABLE_NAME,
                            values,
                            SQLiteDbContract.LocalEntry.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
