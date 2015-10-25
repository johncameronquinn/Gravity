package com.jokrapp.android;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jokrapp.android.SQLiteDbContract.LiveReplies;

import java.io.File;

/**
 * Created by John C. Quinn on 10/24/15.
 *
 * this adapter manages data for the listView in {@link ReplyFragment}
 */
public class ReplyCursorAdapter extends CursorAdapter {

    private final String TAG = "ReplyCursorAdapter";

    private Context ctx;

    public ReplyCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        ctx = context;
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        String path = (cursor.getString(cursor.getColumnIndexOrThrow(LiveReplies.COLUMN_NAME_FILEPATH)));


        View view = LayoutInflater.from(context).inflate(R.layout.fragment_reply_detail_row, parent, false);

        if (!path.equals("")) {
            view.setTag(path);
            Log.d(TAG, "applying tag: " + path);
        }

        return view;
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView name = (TextView) view.findViewById(R.id.reply_detail_row_name);
        TextView text = (TextView) view.findViewById(R.id.reply_detail_row_text);
        TextView date = (TextView) view.findViewById(R.id.reply_detail_row_time);



        // Extract properties from cursor
        name.setText(cursor.getString(cursor.getColumnIndexOrThrow(LiveReplies.COLUMN_NAME_NAME)));
        text.setText(cursor.getString(cursor.getColumnIndexOrThrow(LiveReplies.COLUMN_NAME_DESCRIPTION)));
        date.setText(cursor.getString(cursor.getColumnIndexOrThrow(LiveReplies.COLUMN_NAME_TIME)));

        /*ImageView image = (ImageView) view.findViewById(R.id.reply_detail_row_image);
        String path = (cursor.getString(cursor.getColumnIndexOrThrow(LiveReplies.COLUMN_NAME_FILEPATH)));

        if (!path.equals("")) {
            Log.d(TAG,"a filepath was provided... " + path + " this must be an image reply");
            new ImageLoadTask(image, null).execute(ctx.getCacheDir() + "/" + path + "s");
        } else {
            Log.d(TAG,"no filepath was supplied... this must be a text reply...");
        }*/
    }
}
