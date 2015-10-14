package com.jokrapp.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

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

    private final static String AUTHORITY = "com.jokrapp.android.provider";
    private static String TAG = "FireFlyContentProvider";


    // Stores the MIME types served by this provider
    private static final SparseArray<String> sMimeTypes;



    private static final String LOCAL_BASE_PATH = "local";

    private static final String MESSAGE_BASE_PATH = "message";

    private static final String LIVE_BASE_PATH = "live";

    private static final String REPLY_BASE_PATH = "replies";

    private static final String REPLY_INFO_BASE_PATH = "replyinfo";

    // Indicates that the incoming query is for a picture URL
    public static final int IMAGE_URL_QUERY = 1;

    // Indicates that the incoming query is for a URL modification date
    public static final int URL_DATE_QUERY = 2;


    // Indicates an invalid content URI
    public static final int INVALID_URI = -1;


    public static final Uri CONTENT_URI_LOCAL = Uri.parse("content://" + AUTHORITY
            + "/" + LOCAL_BASE_PATH);

    public static final Uri CONTENT_URI_MESSAGE = Uri.parse("content://" + AUTHORITY
            + "/" + MESSAGE_BASE_PATH);

    public static final Uri CONTENT_URI_LIVE = Uri.parse("content://" + AUTHORITY
            + "/" + LIVE_BASE_PATH);

    public static final Uri CONTENT_URI_REPLY_THREAD_LIST = Uri.parse("content://" + AUTHORITY
            + "/" + REPLY_BASE_PATH);

    public static final Uri CONTENT_URI_REPLY_THREAD_INFO = Uri.parse("content://" + AUTHORITY
            + "/" + REPLY_INFO_BASE_PATH);


    /**
     * Picture table content URI
     */
    public static final Uri PICTUREURL_TABLE_CONTENTURI =
            Uri.parse("content://" + AUTHORITY + "/" + SQLiteDbContract.StashEntry.PICTUREURL_TABLE_NAME);

    public static final Uri DATE_TABLE_CONTENTURI =
    Uri.parse("content://" + AUTHORITY + "/" + SQLiteDbContract.StashEntry.DATE_TABLE_NAME);


    SQLiteDbHelper database;


    private static final int LOCAL = 10;
    private static final int LOCAL_ID = 20;
    private static final int LIVE = 30;
    private static final int LIVE_ID = 40;
    private static final int REPLIES = 70;
    private static final int REPLIES_ID = 80;
    private static final int REPLIES_INFO = 90;
    private static final int REPLIES_INFO_ID = 100;
    private static final int MESSAGE = 110;
    private static final int MESSAGE_ID = 120;


    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        sMimeTypes = new SparseArray<String>();

        // Creates an object that associates content URIs with numeric codes


        sURIMatcher.addURI(AUTHORITY, LOCAL_BASE_PATH, LOCAL);
        sURIMatcher.addURI(AUTHORITY, LOCAL_BASE_PATH + "/#", LOCAL_ID);

        sURIMatcher.addURI(AUTHORITY, MESSAGE_BASE_PATH, MESSAGE);
        sURIMatcher.addURI(AUTHORITY, MESSAGE_BASE_PATH + "/#", MESSAGE_ID);

        sURIMatcher.addURI(AUTHORITY, LIVE_BASE_PATH, LIVE);
        sURIMatcher.addURI(AUTHORITY, LIVE_BASE_PATH + "/#", LIVE_ID);

        sURIMatcher.addURI(AUTHORITY, REPLY_BASE_PATH, REPLIES);
        sURIMatcher.addURI(AUTHORITY, REPLY_INFO_BASE_PATH + "/#", REPLIES_ID);


        // Adds a URI "match" entry that maps picture URL content URIs to a numeric code
        sURIMatcher.addURI(
                AUTHORITY,
                SQLiteDbContract.StashEntry.PICTUREURL_TABLE_NAME,
                IMAGE_URL_QUERY);

        // Adds a URI "match" entry that maps modification date content URIs to a numeric code
        sURIMatcher.addURI(
                AUTHORITY,
                SQLiteDbContract.StashEntry.DATE_TABLE_NAME,
                URL_DATE_QUERY);

        // Specifies a custom MIME type for the picture URL table
        sMimeTypes.put(
                IMAGE_URL_QUERY,
                "vnd.android.cursor.dir/vnd." +
                        AUTHORITY + "." +
                        SQLiteDbContract.StashEntry.PICTUREURL_TABLE_NAME);

        // Specifies the custom MIME type for a single modification date row
        sMimeTypes.put(
                URL_DATE_QUERY,
                "vnd.android.cursor.item/vnd."+
                        AUTHORITY + "." +
                        SQLiteDbContract.StashEntry.DATE_TABLE_NAME);
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

            case LIVE_ID:
                Log.d(TAG,"LIVE_ID called");
                directory = LIVE_BASE_PATH;
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



        SQLiteDatabase db =database.getReadableDatabase();//todo remove this

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


            case REPLIES:
                Log.d(TAG,"REPLY_INFO called");
                queryBuilder.setTables(SQLiteDbContract.LiveReplies.TABLE_NAME);
                break;
            case REPLIES_ID:
                queryBuilder.appendWhere(SQLiteDbContract.LiveReplies.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;

                        // If the query is for a picture URL
            case IMAGE_URL_QUERY:

                // Does the query against a read-only version of the database
                Cursor returnCursor = db.query(
                        SQLiteDbContract.StashEntry.PICTUREURL_TABLE_NAME,
                        projection,
                        null, null, null, null, null);

                // Sets the ContentResolver to watch this content URI for data changes
                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;


            // If the query is for a modification date URL
            case URL_DATE_QUERY:

                returnCursor = db.query(
                        SQLiteDbContract.StashEntry.DATE_TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                // No notification Uri is set, because the data doesn't have to be watched.
                return returnCursor;


            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase dba = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(dba, projection, selection,
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

                id = sqlDB.insertWithOnConflict(SQLiteDbContract.LiveThreadEntry.TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_REPLACE);
                break;


            case REPLIES:

                id = sqlDB.insertWithOnConflict(SQLiteDbContract.LiveReplies.TABLE_NAME, null, values,SQLiteDatabase.CONFLICT_IGNORE);
                break;

            // For the modification date table
            case URL_DATE_QUERY: //todo better melding required

                // Creates a writeable database or gets one from cache
                SQLiteDatabase localSQLiteDatabase = database.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                id = localSQLiteDatabase.insert(
                        SQLiteDbContract.StashEntry.DATE_TABLE_NAME,
                        SQLiteDbContract.StashEntry.DATA_DATE_COLUMN,
                        values
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (-1 != id) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(id));
                } else {

                    throw new SQLiteException("Insert error:" + uri);
                }
            case IMAGE_URL_QUERY:

                throw new IllegalArgumentException("Insert: Invalid URI" + uri);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(uriType + "/" + id);
    }

    /**
     * Implements bulk row insertion using
     * {@link SQLiteDatabase#insert(String, String, ContentValues) SQLiteDatabase.insert()}
     * and SQLite transactions. The method also notifies the current
     * {@link android.content.ContentResolver} that the {@link android.content.ContentProvider} has
     * been changed.
     * @see android.content.ContentProvider#bulkInsert(Uri, ContentValues[])
     * @param uri The content URI for the insertion
     * @param insertValuesArray A {@link android.content.ContentValues} array containing the row to
     * insert
     * @return The number of rows inserted.
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] insertValuesArray) {

        // Decodes the content URI and choose which insert to use
        switch (sURIMatcher.match(uri)) {

            // picture URLs table
            case IMAGE_URL_QUERY:

                // Gets a writeable database instance if one is not already cached
                SQLiteDatabase localSQLiteDatabase = database.getWritableDatabase();

                /*
                 * Begins a transaction in "exclusive" mode. No other mutations can occur on the
                 * db until this transaction finishes.
                 */
                localSQLiteDatabase.beginTransaction();

                // Deletes all the existing rows in the table
                localSQLiteDatabase.delete(SQLiteDbContract.StashEntry.PICTUREURL_TABLE_NAME, null, null);

                // Gets the size of the bulk insert
                int numImages = insertValuesArray.length;

                // Inserts each ContentValues entry in the array as a row in the database
                for (int i = 0; i < numImages; i++) {

                    localSQLiteDatabase.insert(SQLiteDbContract.StashEntry.PICTUREURL_TABLE_NAME,
                            SQLiteDbContract.StashEntry.IMAGE_URL_COLUMN, insertValuesArray[i]);
                }

                // Reports that the transaction was successful and should not be backed out.
                localSQLiteDatabase.setTransactionSuccessful();

                // Ends the transaction and closes the current db instances
                localSQLiteDatabase.endTransaction();
                localSQLiteDatabase.close();

                /*
                 * Notifies the current ContentResolver that the data associated with "uri" has
                 * changed.
                 */

                getContext().getContentResolver().notifyChange(uri, null);

                // The semantics of bulkInsert is to return the number of rows inserted.
                return numImages;

            // modification date table
            case URL_DATE_QUERY:

                // Do inserts by calling SQLiteDatabase.insert on each row in insertValuesArray
                return super.bulkInsert(uri, insertValuesArray);

            case INVALID_URI:

                // An invalid URI was passed. Throw an exception
                throw new IllegalArgumentException("Bulk insert -- Invalid URI:" + uri);

        }

        return -1;

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


            // A picture URL content URI
            case URL_DATE_QUERY:

                // Creats a new writeable database or retrieves a cached one
                SQLiteDatabase localSQLiteDatabase = database.getWritableDatabase();

                // Updates the table
                int rows = localSQLiteDatabase.update(
                        SQLiteDbContract.StashEntry.DATE_TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (true) {
                // 0 != rows) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {

                    // FIXME: 9/24/15
                    Log.e(TAG,"no rows were updated. What the flock is going on.");

                    break;
                    //throw new SQLiteException("Update error:" + uri);
                }

            case IMAGE_URL_QUERY:

                throw new IllegalArgumentException("Update: Invalid URI: " + uri);


            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
