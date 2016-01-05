package us.gravwith.android;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import us.gravwith.android.SQLiteDbContract.LiveEntry;
import us.gravwith.android.util.Utility;

/**
 * Created by John C. Quinn on 10/24/15.
 *
 * this adapter manages data for the listView in {@link ReplyFragment}
 */
public class HybridCursorAdapter extends CursorAdapter implements PhotoView.OnClickListener {

    private final String TAG = getClass().getSimpleName();

    private static final boolean VERBOSE = false;

    // A Drawable for a grid cell that's empty
    private Drawable mEmptyDrawable;

    public HybridCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mEmptyDrawable = context.getResources().getDrawable(R.drawable.imagenotqueued);
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (VERBOSE) Log.v(TAG,"entering newView...");

        View view = LayoutInflater.from(context).inflate(
                R.layout.hybrid_detail_row,
                parent,
                false
        );

        if (VERBOSE) Log.v(TAG,"exiting newView...");
        return view;
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView text = (TextView) view.findViewById(R.id.reply_detail_row_text);
        TextView date = (TextView) view.findViewById(R.id.reply_detail_row_time);


        // Extract properties from cursor
        text.setText(
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                LiveEntry.COLUMN_NAME_DESCRIPTION)
                )
        );
        date.setText(Utility.getDateStringFromLong(Long.parseLong(
                cursor.getString(
                cursor.getColumnIndexOrThrow(
                        LiveEntry.COLUMN_NAME_TIME)
                )))
        );
        String path = (
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                LiveEntry.COLUMN_NAME_FILEPATH)
                )
        );

        // Gets a handle to the View
        PhotoView localImageDownloaderView = ((PhotoView)view.findViewById(R.id.thumbImage));
        localImageDownloaderView.setOnClickListener(this);

        if(!path.equals("")) {
            if (VERBOSE) Log.v(TAG,"a filepath was provided... " + path +
                    " this must be an image reply");

            localImageDownloaderView.setImageKey(
                    Constants.KEY_S3_LIVE_DIRECTORY,
                    path, false, mEmptyDrawable
            );
            localImageDownloaderView.setTag(path);
            localImageDownloaderView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (VERBOSE) Log.v(TAG,"entering onClick...");
        Log.d(TAG,"View string is : " + view.toString());

        // Retrieves the urlString from the cursor
        String s3Key = ((PhotoView)view).getImageKey();
        s3Key = s3Key.substring(0,s3Key.length()-1);

        Log.d(TAG, "grabbed key is: " + s3Key);
        /*
         * Creates a new Intent to get the full picture for the thumbnail that the user clicked.
         * The full photo is loaded into a separate Fragment
         */
        Intent localIntent =
                new Intent(Constants.ACTION_VIEW_IMAGE).putExtra(Constants.KEY_S3_KEY,s3Key)
                        .putExtra(Constants.KEY_S3_DIRECTORY,Constants.KEY_S3_LIVE_DIRECTORY)
                        .putExtra(Constants.KEY_PREVIEW_IMAGE,false);

        // Broadcasts the Intent to receivers in this app. See DisplayActivity.FragmentDisplayer.
        LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(localIntent);

        if (VERBOSE) Log.v(TAG,"exiting onClick...");
    }
}
