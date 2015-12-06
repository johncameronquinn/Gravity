package com.jokrapp.android;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.UUID;

/**
 * Author/Copyright John C. Quinn, All Rights Reserved.
 *
 * class 'ImageStackCursorAdapter'
 *
 * custom implementation of SimpleCursorAdapter to allow images to be dynamically decoded
 * and loaded into the view. Maintains a reference to the database using cursors,
 * and dynamically swaps the cursors with their actual data once the data is finished loading
 */
public class ImageStackMessageAdapter extends ImageStackCursorAdapter implements View.OnClickListener{
    private final boolean VERBOSE = true;
    private final String TAG = "ImageMessageAdapter";

    private MainActivity context;
    private int layout;
    private Uri table;

    private Drawable mEmptyDrawable;
    private LayoutInflater mLayoutInflater;

    private HashMap<String,Integer> uuidColorHashMap = new HashMap<>();

    private onIndicatorClick mListener;

    /**
     * class 'ViewHolder'
     *
     * holds view data, that persists between newView and bindView
     * this allows bindView to store data to the correct objects
     */
    private class ViewHolder {
        //TextView textView;
        PhotoView photoView;
        String path;

        ViewHolder(View v, String s) {
            photoView = (PhotoView) v.findViewById(R.id.photoView);
            path = s;
        }
    }
    /**
     * method 'ImageStackCursorAdapter'
     *
     * constructor
     *
     * @param activity activity in which this adapter is created
     * @param layout the layout to be inflated
     * @param c data to be loaded
     * @param table from where to load
     * @param flags options
     */
    public ImageStackMessageAdapter(MainActivity activity, int layout, Cursor c, Uri table, int flags) {
        super(activity, layout,c, table, flags);
        this.context = activity;
        this.layout = layout;
        mLayoutInflater = LayoutInflater.from(context);
        this.table = table;

        mEmptyDrawable = activity.getResources().getDrawable(R.drawable.imagenotqueued);
    }

    public void setOnIndicatorClickListener(onIndicatorClick clickListener) {
        mListener = clickListener;
    }

    /**
     * method 'newView'
     *
     * creates a new view with cursor references to its data.
     * in this method, the data is not actually loaded
     *
     * as each view is created, the buttons on each view have a listener, which is set to
     * a buttonlistener, so that interaction with them can be broadcasted, and received in the
     * main activity
     *
     * @param ctx contect in which the view exists
     * @param cursor data to be used tor reference
     * @param parent parent in which the view is held
     * @return View to be displayed
     */
    @Override
    public View newView(Context ctx, Cursor cursor, ViewGroup parent) {
        View vView;
        if (parent instanceof ListView) {
            vView = mLayoutInflater.inflate(R.layout.message_indicator,parent,false);
            vView.findViewById(R.id.message_indicator_image).setOnClickListener(this);
        } else {
            vView=  mLayoutInflater.inflate(layout, parent, false);

            vView.setTag(new ViewHolder(vView, cursor.getString
                    (cursor.getColumnIndex(SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH))));
            /*
             * sets tags of the buttons, so then when they are clicked, the method in mainactivity
             * knows which card they were attached to.
             */

            String UUID = cursor.getString(
                    cursor.getColumnIndex(SQLiteDbContract.MessageEntry.COLUMN_RESPONSE_ARN)
            );

            if (VERBOSE) Log.v(TAG, "Incoming ARN which I am setting is : " + UUID);

            ((TextView)vView.findViewById(R.id.userID)).setText(UUID);
            context.findViewById(R.id.button_message_reply).setTag(UUID);
            context.findViewById(R.id.button_message_block).setTag(UUID);

        }

        if (VERBOSE) {
            Log.v(TAG,"exiting newView...");
        }
        return vView;// **EDITED:**need to return the view
    }

    /**
     * method 'bindView'
     *
     * resolves the data from the cursor, and then loads the data
     * note that the data binding is performed as a seperate process from the view creation
     *
     * @param v the view
     * @param ctx the context in which this view is created
     * @param c the data to be bound.
     */
    @Override
    public void bindView(View v, Context ctx, Cursor c) {
        if (VERBOSE) {
            Log.v(TAG,"entering bindView...");

        }

        if (v instanceof FrameLayout) {

        /* Message_Indicator has a FrameLayout
         * as a root, std_card_inner users a RelativeLayout
         */

            if (VERBOSE) Log.v(TAG,"bind view listView...");
            //set the state to be either received or pending

            String suuid = c.getString(
                    c.getColumnIndex(SQLiteDbContract.MessageEntry.COLUMN_RESPONSE_ARN)
            );


            if (!"".equals(suuid)) { // is not null
                if (VERBOSE) Log.v(TAG, "UUID is not null, setting minifab to colored");


                int imageButtonColor;
                if (uuidColorHashMap.containsKey(suuid)) {
                    imageButtonColor = uuidColorHashMap.get(suuid);
                } else {
                    imageButtonColor = ctx.getResources().getColor(R.color.jpallete_neutral_blue);
                    uuidColorHashMap.put(suuid,imageButtonColor);
                }


                ((ImageButton) v.findViewById(R.id.message_indicator_image))
                        .setColorFilter(imageButtonColor);
            }

        } else {
            if (VERBOSE) Log.v(TAG,"bind view ImageCursorView...");

         /* grab the caption from incoming files*/
            String caption_text = c.getString(
                    c.getColumnIndex(
                            SQLiteDbContract.MessageEntry.COLUMN_NAME_TEXT)
            );

        /* if the caption is not empty (or null) set and display*/
            if (!"".equals(caption_text)) {
                TextView caption = ((TextView) v.findViewById(R.id.textView_message_caption));
                caption.setText(caption_text);
                caption.setVisibility(View.VISIBLE);
            }

            ViewHolder vh = (ViewHolder) v.getTag();
            if (VERBOSE) Log.v(TAG, "incoming filepath is: " + vh.path);

            vh.photoView.setImageKey(Constants.KEY_S3_MESSAGE_DIRECTORY,
                    vh.path,
                    true,
                    this.mEmptyDrawable
            );
        }

        if (VERBOSE) {
            Log.v(TAG, "exiting bindView...");
        }
    }

    /**
     * method 'pop'
     *
     * removes the topmost item from the view, then deletes it from the database, then requests
     * a replacement image
     */
    public String pop() {
        if (VERBOSE) {
            Log.v(TAG,"entering pop...");
        }

        Cursor c =  ((Cursor)getItem(0));

        if (c.getCount() == 0) { //todo this shouldn't have to be here
            Log.e(TAG,"there was no image to pop");
            return "huh";
        }
        String filePath = c.getString(c.getColumnIndex(SQLiteDbContract.MessageEntry.
                COLUMN_NAME_FILEPATH));


        String arn = c.getString(c.getColumnIndex(SQLiteDbContract.MessageEntry.
                COLUMN_RESPONSE_ARN));
        String out = "'" + filePath + "'";
        mListener.onPop(out,arn);

        if (VERBOSE) {
            Log.v(TAG,"exiting pop...");
        }

        return "filePath";
    }

    public void onClick(View v) {
        mListener.onIndicatorClick(v);

    }

    public interface onIndicatorClick {
        void onIndicatorClick(View v);
        void onPop(String filePath, String ARN);
    }


}