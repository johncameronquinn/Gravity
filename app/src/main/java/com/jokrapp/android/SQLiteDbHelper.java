package com.jokrapp.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.jokrapp.android.SQLiteDbContract.LocalEntry;

import com.jokrapp.android.SQLiteDbContract.MessageEntry;

import com.jokrapp.android.SQLiteDbContract.LiveThreadEntry;

import com.jokrapp.android.SQLiteDbContract.LiveThreadInfoEntry;

import com.jokrapp.android.SQLiteDbContract.LiveReplies;

import com.jokrapp.android.SQLiteDbContract.StashEntry;


/**
 * Author/Copyright John C. Quinn All Rights Reserved.
 *
 * class 'SQLiteDbHelper'
 * date created: 5/21/15
 *
 * last edited: 8/1/15
 *
 * accessor class for SQLite database
 */
public class SQLiteDbHelper extends SQLiteOpenHelper {
    private final String TAG = "SqLiteDbHelper";

  //  private static SQLiteDatabase dbR;
//    private static SQLiteDatabase dbW;

 //   private static SQLiteDbHelper sInstance;



    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ImagesStore.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String UNIQUE_KEY = " UNIQUE";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_LOCAL =

            "CREATE TABLE " + LocalEntry.TABLE_NAME + " (" +
                    LocalEntry._ID + " INTEGER KEY" +
                    COMMA_SEP +
                    LocalEntry.COLUMN_FROM_USER + TEXT_TYPE + COMMA_SEP +
                    LocalEntry.COLUMN_NAME_WEIGHT + INT_TYPE + COMMA_SEP +
                    LocalEntry.COLUMN_NAME_TIME + INT_TYPE + COMMA_SEP +
                    LocalEntry.COLUMN_NAME_LATITUDE + TEXT_TYPE + COMMA_SEP +
                    LocalEntry.COLUMN_NAME_LONGITUDE + TEXT_TYPE + COMMA_SEP +
                    LocalEntry.COLUMN_NAME_FILEPATH + TEXT_TYPE +
                    " )";

    private static final String SQL_CREATE_MESSAGE =

            "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                    MessageEntry._ID + PRIMARY_KEY + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_TIME + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_FROM_USER + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_FILEPATH + TEXT_TYPE +
                    " )";


    private static final String SQL_CREATE_LIVE =

            "CREATE TABLE " + LiveThreadEntry.TABLE_NAME + " (" +
                    LiveThreadEntry._ID + PRIMARY_KEY + COMMA_SEP +
                    LiveThreadEntry.COLUMN_THREAD_ID + INT_TYPE +
                    " )";


    private static final String SQL_CREATE_LIVE_INFO =

            "CREATE TABLE " + LiveThreadInfoEntry.TABLE_NAME + " (" +
                    LiveThreadInfoEntry._ID + PRIMARY_KEY + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_THREAD_ID  + INT_TYPE + UNIQUE_KEY + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_TIME + INT_TYPE + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_FILEPATH + TEXT_TYPE + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_REPLIES + INT_TYPE + COMMA_SEP +
                    LiveThreadInfoEntry.COLUMN_NAME_UNIQUE + INT_TYPE +
                    " )";

    private static final String SQL_CREATE_REPLIES =

            "CREATE TABLE " + LiveReplies.TABLE_NAME + " (" +
                    LiveReplies._ID+ PRIMARY_KEY + COMMA_SEP +
                    LiveReplies.COLUMN_NAME_THREAD_ID +  TEXT_TYPE + COMMA_SEP +
                    LiveReplies.COLUMN_NAME_TIME + INT_TYPE + COMMA_SEP +
                    LiveReplies.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    LiveReplies.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + //COMMA_SEP +
     //               LiveReplies.COLUMN_NAME_FILEPATH + TEXT_TYPE +
                    " )";

    // Defines an SQLite statement that builds the Picasa picture URL table
    private static final String CREATE_PICTUREURL_TABLE_SQL = "CREATE TABLE" + " " +
            StashEntry.PICTUREURL_TABLE_NAME + " " +
            "(" + " " +
            StashEntry.ROW_ID + " " + PRIMARY_KEY + " ," +
            StashEntry.IMAGE_THUMBURL_COLUMN + " " + TEXT_TYPE + " ," +
            StashEntry.IMAGE_URL_COLUMN + " " + TEXT_TYPE + " ," +
            StashEntry.IMAGE_THUMBNAME_COLUMN + " " + TEXT_TYPE + " ," +
            StashEntry.IMAGE_PICTURENAME_COLUMN + " " + TEXT_TYPE +
            ")";

    // Defines an SQLite statement that builds the URL modification date table
    private static final String CREATE_DATE_TABLE_SQL = "CREATE TABLE" + " " +
            StashEntry.DATE_TABLE_NAME + " " +
            "(" + " " +
            StashEntry.ROW_ID + " " + PRIMARY_KEY + " ," +
            StashEntry.DATA_DATE_COLUMN + " " + INT_TYPE +
            ")";



    private final String SQL_DELETE_LOCAL =
            "DROP TABLE IF EXISTS " + LocalEntry.TABLE_NAME;
    private final String SQL_DELETE_MESSAGE =
            "DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME;
    private final String SQL_DELETE_LIVE =
            "DROP TABLE IF EXISTS " + LiveThreadEntry.TABLE_NAME;
    private final String SQL_DELETE_LIVE_INFO =
            "DROP TABLE IF EXISTS " + LiveThreadInfoEntry.TABLE_NAME;
    private final String SQL_DELETE_REPLIES =
            "DROP TABLE IF EXISTS " + LiveReplies.TABLE_NAME;
    private final String SQL_DELETE_STASH_PICTUREURL =
            "DROP TABLE IF EXISTS " + StashEntry.PICTUREURL_TABLE_NAME;
    private final String SQL_DELETE_STASH_DATETABLE =
            "DROP TABLE IF EXISTS " + StashEntry.DATE_TABLE_NAME;



    public SQLiteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_LOCAL);
        db.execSQL(SQL_CREATE_MESSAGE);
        db.execSQL(SQL_CREATE_LIVE);
        db.execSQL(SQL_CREATE_LIVE_INFO);
        db.execSQL(SQL_CREATE_REPLIES);

        db.execSQL(CREATE_PICTUREURL_TABLE_SQL);
        db.execSQL(CREATE_DATE_TABLE_SQL);

        Log.d(TAG, "database created...");
    }

    public void dropTables(SQLiteDatabase db) {

        db.execSQL(SQL_DELETE_LOCAL);
        db.execSQL(SQL_DELETE_MESSAGE);
        db.execSQL(SQL_DELETE_LIVE);
        db.execSQL(SQL_DELETE_LIVE_INFO);
        db.execSQL(SQL_DELETE_REPLIES);
        db.execSQL(SQL_DELETE_STASH_DATETABLE);
        db.execSQL(SQL_DELETE_STASH_PICTUREURL);

    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        dropTables(db);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SQLiteDbHelper.class.getName(),
                "Downgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all the existing data");

       dropTables(db);
        onCreate(db);
    }

    public static double distanceFormula(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow((x1-x2),2)+Math.pow((y1-y2),2));
    }

}


