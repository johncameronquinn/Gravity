package com.jokrapp.android;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.app.Fragment;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.jokrapp.android.SQLiteDbContract.MessageEntry;

import com.amazonaws.mobile.AWSMobileClient;
import com.jokrapp.android.view.ImageCursorAdapterView;

import java.net.HttpURLConnection;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the=
 * to handle interaction events.
 */
public class MessageFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    private final String TAG = "MessageFragment";
    private final boolean VERBOSE = true;

    private ListView messageIndicatorListView;
    private IndicatorAdapter mIndicatorAdapter;
    private ImageAdapter mImageAdapter;
    private ImageCursorAdapterView imageAdapterView;

    private int IMAGE_LOADER_ID = 10;
    private int INDICATOR_LOADER_ID = 85;

    private onMessageFragmentInteractionListener mListener;

    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (VERBOSE) {
            Log.v(TAG, "enter onCreate...");
        }


        if (VERBOSE) {
            Log.v(TAG,"exit onCreate...");
        }
        fillData();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ((MainActivity)activity).sendMsgRequestLocalMessages();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) {
            Log.v(TAG,"enter onDestroy...");
        }

        getLoaderManager().destroyLoader(IMAGE_LOADER_ID);
        getLoaderManager().destroyLoader(IMAGE_LOADER_ID);

        mListener = null;
        mImageAdapter = null;
        mIndicatorAdapter = null;

        PhotoManager.cancelDirectory(Constants.KEY_S3_MESSAGE_DIRECTORY);
        if (VERBOSE) {
            Log.v(TAG,"exit onDestroy...");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) {
            Log.v(TAG,"enter onCreateView...");
        }
        // Inflate the layout for this fragment
        View cat = inflater.inflate(R.layout.fragment_message, container, false);

        imageAdapterView = (ImageCursorAdapterView)cat.findViewById(R.id.message_images_view);
        messageIndicatorListView = (ListView)cat.findViewById(R.id.message_indicator_ListView);

        if (VERBOSE) {
            Log.v(TAG,"exit onCreateView...");
        }
        return cat;
    }

    @Override
    public void onViewCreated(View cat, Bundle savedInstanceState) {
        super.onViewCreated(cat, savedInstanceState);
        if (VERBOSE) {
            Log.v(TAG,"enter onViewCreated...");
        }

        imageAdapterView.setAdapter(mImageAdapter);
        imageAdapterView.setOnPopListener(mImageAdapter);
        messageIndicatorListView.setAdapter(mIndicatorAdapter);
        cat.findViewById(R.id.button_message_reply).setOnClickListener(this);

        cat.findViewById(R.id.button_message_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (VERBOSE) Log.v(TAG, "entering MessageSaveToStash-OnClick...");

                PhotoView topView = (
                        (PhotoView) imageAdapterView
                                .getmTopCard()
                                .findViewById(R.id.photoView)
                );

                mListener.saveToStash(topView);
                if (VERBOSE) Log.v(TAG, "exiting MessageSaveToStash-OnClick...");
            }
        });
        if (VERBOSE) {
            Log.v(TAG,"exit onViewCreated...");
        }
    }

    private void fillData() {

        // Fields from the database (projection)
        // Must include the _id column for the adapter to work
        String[] from = new String[] {SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH};
        // Fields on the UI to which we map
        // int[] to = new int[] { R.id.imageID};

        getLoaderManager().initLoader(IMAGE_LOADER_ID, null, this);
        getLoaderManager().initLoader(INDICATOR_LOADER_ID, null, this);

        /*adapter = new ImageStackMessageAdapter((MainActivity)getActivity(),
                R.layout.std_msg_inner,
                null,
                FireFlyContentProvider.CONTENT_URI_MESSAGE,
                0);*/

        mIndicatorAdapter = new IndicatorAdapter(getActivity(),null);
        mImageAdapter = new ImageAdapter(getActivity(),null);
    }

    public void handleMessageResponseState(Message msg) {
        if (VERBOSE) {
            Log.v(TAG, "entering handleMessageResponseState...");
        }

        switch (msg.arg2) {
            case HttpURLConnection.HTTP_OK:
                if (VERBOSE) Log.v(TAG, "Response code : " + msg.arg2);
                //imageAdapterView.setAdapter(adapter);
                break;

            default:
                //Toast.makeText(getActivity(), "Response code : " + msg.arg2, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * method 'onCreateLoader'
     *
     * this method is called to create the loader, it controls what data the loader will
     * pull from the content provider with projection
     *
     * thex
     *
     * @param id no idea
     * @param args no idea
     * @return the created loader
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (VERBOSE) {
            Log.v(TAG, "enter onCreateLoader...");
        }

        if (id == IMAGE_LOADER_ID) { // image loader

            String[] projection = {
                    SQLiteDbContract.MessageEntry.COLUMN_ID,
                    SQLiteDbContract.MessageEntry.COLUMN_FROM_USER,
                    SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH,
                    SQLiteDbContract.MessageEntry.COLUMN_NAME_TEXT,
                    SQLiteDbContract.MessageEntry.COLUMN_RESPONSE_ARN,
            };

            if (VERBOSE) {
                Log.v(TAG, "exit onCreateLoader...");
            }
            return new CursorLoader(getActivity(),
                    FireFlyContentProvider.CONTENT_URI_MESSAGE,
                    projection,
                    MessageEntry.COLUMN_NAME_FILEPATH + " IS NOT NULL",
                    null,
                    null);

        } else { // indicator loader


            String[] projection = {
                    SQLiteDbContract.MessageEntry.COLUMN_ID,
                    SQLiteDbContract.MessageEntry.COLUMN_FROM_USER,
                    SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH,
                    SQLiteDbContract.MessageEntry.COLUMN_RESPONSE_ARN,
            };

            if (VERBOSE) {
                Log.v(TAG, "exit onCreateLoader...");
            }
            return new CursorLoader(getActivity(),
                    FireFlyContentProvider.CONTENT_URI_MESSAGE,
                    projection,
                    null,
                    null,
                    null);
        }
    }


    /**
     * method 'onLoadFinished'
     *
     * called when the loader finishes loading a bit of data
     * that data is initially stored in a cursor, which is a reference to a row
     * in the database
     *
     * when the load is finished, the newly loaded data is swaped with the cursor, and shown
     * to the user
     *
     * @param loader the loader that just finished loading
     * @param data the data that was loaded
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (VERBOSE) {
            Log.v(TAG,"enter onLoadFinished...");
        }

        /* only set the action buttons to visible if there is content */
        if (data.getCount() > 0) {
            View v = getView();
            if (v!=null) {
                v.findViewById(R.id.button_message_reply).setVisibility(View.VISIBLE);
                v.findViewById(R.id.button_message_save).setVisibility(View.VISIBLE);
                v.findViewById(R.id.button_message_block).setVisibility(View.VISIBLE);
                v.findViewById(R.id.button_local_report).setVisibility(View.VISIBLE);
            }
        } else {
            View v = getView();
            if (v!=null) {
                v.findViewById(R.id.button_message_reply).setVisibility(View.GONE);
                v.findViewById(R.id.button_message_save).setVisibility(View.GONE);
                v.findViewById(R.id.button_message_block).setVisibility(View.GONE);
                v.findViewById(R.id.button_local_report).setVisibility(View.GONE);
            }

            Log.i(TAG,"there are no messages pending or received... display something...?");
            //todo, show text that explains to the user what message is and how use
        }



        if (loader.getId() == IMAGE_LOADER_ID) {
            mImageAdapter.swapCursor(data);
        } else {
            mIndicatorAdapter.swapCursor(data);
        }

        if (VERBOSE) {
            Log.v(TAG,"exit onLoadFinished...");
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (VERBOSE) {
            Log.v(TAG,"enter onLoaderReset...");
        }
        mIndicatorAdapter.swapCursor(null);
        mImageAdapter.swapCursor(null);

        if (VERBOSE) {
            Log.v(TAG,"exit onLoaderReset...");
        }
    }

    //@Override
    public void onIndicatorClick(View v) {
        Log.v(TAG,"clicked! : " + v.toString());

        //get the position of the view which was clicked
        int position = messageIndicatorListView.getPositionForView(v);

        //get the corresponding cardview
        View cardView = imageAdapterView.getChildAt(position);

        //show
        //cardView.bringToFront();
    }

    //@Override
    public void onPop(final String filepath, final String ARN) {
        if (VERBOSE) Log.v(TAG,"entering onPop with : " + filepath + ", " + ARN);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContentResolver().
                        delete(FireFlyContentProvider.CONTENT_URI_MESSAGE,
                                SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH
                                        + " = " + filepath,
                                null);
                AWSMobileClient.defaultMobileClient().getPushManager().sendReadReceipt(ARN);
            }
        }).start();


        if (VERBOSE) Log.v(TAG,"exiting onPop...");
    }

    private class ImageAdapter extends CursorAdapter implements ImageCursorAdapterView.OnPopListener {

        LayoutInflater inflater;
        private Drawable mEmptyDrawable;

        int arn_column_index;
        int url_column_index;
        int fromUser_column_index;
        int text_column_index;

        public ImageAdapter(Context ctx, Cursor c) {
            super(ctx,c,0);
            inflater = LayoutInflater.from(ctx);
            mEmptyDrawable = ctx.getResources().getDrawable(R.drawable.imagenotqueued);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {

            if (newCursor != null) {
                /* preset column indexes for better performance */
                arn_column_index = newCursor.getColumnIndexOrThrow(MessageEntry.COLUMN_RESPONSE_ARN);
                url_column_index = newCursor.getColumnIndexOrThrow(MessageEntry.COLUMN_NAME_FILEPATH);
                fromUser_column_index = newCursor.getColumnIndexOrThrow(MessageEntry.COLUMN_FROM_USER);
                text_column_index = newCursor.getColumnIndexOrThrow(MessageEntry.COLUMN_NAME_TEXT);
            }

            return super.swapCursor(newCursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View outView = inflater.inflate(R.layout.std_msg_inner, parent, false);
            return outView;
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            /* Message_Indicator has a FrameLayout
            * as a root, std_card_inner users a RelativeLayout
            */

            /* grab the s3key from incoming files*/
            String s3key = c.getString(url_column_index);

            /* grab the arn from files*/
            String arn = c.getString(arn_column_index);

            view.setTag(R.integer.arn_key,arn);
            view.setTag(R.integer.file_path_key,s3key);

                 /* grab the caption from incoming files*/
            String captionText = c.getString(text_column_index);

                /* if the caption is not empty (or null) set and display*/
            if (!"".equals(captionText) && captionText != null) {
                TextView caption = ((TextView)
                        view.findViewById(R.id.textView_message_caption));
                caption.setText(captionText);
                caption.setVisibility(View.VISIBLE);
            }

            ((PhotoView)view.findViewById(R.id.photoView))
                    .setImageKey(Constants.KEY_S3_MESSAGE_DIRECTORY,
                            s3key,
                            true,
                            this.mEmptyDrawable
                    );
        }

        public void onPop(View topCardView) {
            final String arn = (String) topCardView.getTag(R.integer.arn_key);
            final String s3key = (String) topCardView.getTag(R.integer.file_path_key);
            if (VERBOSE) Log.v(TAG, "entering onPop with : " + s3key + ", " + arn);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String[] selectionArgs = {s3key};

                    getActivity().getContentResolver().
                            delete(FireFlyContentProvider.CONTENT_URI_MESSAGE,
                                    SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH
                                            + " = ?",
                                    selectionArgs);
                    AWSMobileClient.defaultMobileClient().getPushManager().sendReadReceipt(arn);
                }
            }).start();
        }
    }

    private class IndicatorAdapter extends CursorAdapter implements View.OnClickListener,
            ImageCursorAdapterView.OnPopListener {

        LayoutInflater inflater;

        int arn_column_index;
        int url_column_index;
        int fromUser_column_index;

        public IndicatorAdapter(Context ctx, Cursor c) {

            super(ctx,c,0);
            inflater = LayoutInflater.from(ctx);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {

            if (newCursor != null) {
                /* preset column indexes for better performance */
                arn_column_index = newCursor.getColumnIndexOrThrow(MessageEntry.COLUMN_RESPONSE_ARN);
                fromUser_column_index = newCursor.getColumnIndexOrThrow(MessageEntry.COLUMN_FROM_USER);
            }

            return super.swapCursor(newCursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View outView;
                outView = inflater.inflate(R.layout.message_indicator, parent, false);
            outView.findViewById(R.id.message_indicator_image).setOnClickListener(this);
            return outView;
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            /* Message_Indicator has a FrameLayout
            * as a root, std_card_inner users a RelativeLayout
            */

            /* grab the s3key from incoming files*/
            String s3key = c.getString(url_column_index);

            /* grab the arn from files*/
            String arn = c.getString(arn_column_index);

            view.setTag(R.integer.arn_key, arn);
            view.setTag(R.integer.file_path_key, s3key);

            if (VERBOSE) Log.v(TAG, "bind view listView...");
            //set the state to be either received or pending

            if (s3key != null) {
                if (VERBOSE) Log.v(TAG, "URL is not null, setting minifab to colored");
                Log.v(TAG, "Url: " + s3key);

                ((ImageButton) view.findViewById(R.id.message_indicator_image))
                        .setColorFilter(
                                getActivity()
                                        .getResources()
                                        .getColor(R.color.cyber_light_blue
                                        ),
                        android.graphics.PorterDuff.Mode.MULTIPLY
                        );
            } else {
                ((ImageButton) view.findViewById(R.id.message_indicator_image))
                        .setColorFilter(R.color.clear_background);
            }

        }

        public void onPop(View topCardView) {
            final String arn = (String)topCardView.getTag(R.integer.arn_key);
            final String s3key = (String)topCardView.getTag(R.integer.file_path_key);
            if (VERBOSE) Log.v(TAG,"entering onPop with : " + s3key + ", " + arn);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String[] selectionArgs = {s3key};

                    getActivity().getContentResolver().
                            delete(FireFlyContentProvider.CONTENT_URI_MESSAGE,
                                    SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH
                                            + " = ?",
                                    selectionArgs);
                    AWSMobileClient.defaultMobileClient().getPushManager().sendReadReceipt(arn);
                }
            }).start();
        }

        public void onClick(View v) {
            //mListener.onIndicatorClick(v);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_message_reply:
                String arn = (String)imageAdapterView.getmTopCard().getTag(R.integer.arn_key);
                ((MainActivity)getActivity()).localMessagePressed(arn);
                break;
        }
    }


    /**
     * interface 'onLocalFragmentInteractionListener'
     *
     */
    public interface onMessageFragmentInteractionListener {
        void saveToStash(PhotoView view);
    }
}
