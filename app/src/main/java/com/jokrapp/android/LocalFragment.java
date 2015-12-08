package com.jokrapp.android;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jokrapp.android.view.ImageCursorAdapterView;

import com.jokrapp.android.SQLiteDbContract.LocalEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.net.HttpURLConnection;


/**
 * class "LocalFragment'
 *
 * This Fragment represents the "local" UI.
 *
 */
public class LocalFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    private final String TAG = "LocalFragment";
    private final boolean VERBOSE = true;
    private final int LOCAL_LOADER_ID = 2;

    private ImageAdapter adapter;
    private ImageCursorAdapterView imageAdapterView;

    private onLocalFragmentInteractionListener mListener;

    public LocalFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERBOSE) {
            Log.v(TAG,"enter onCreate...");
        }
        fillData();
        if (VERBOSE) {
            Log.v(TAG,"exit onCreate...");
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);


        String[] projection = new String[1];
        projection[0] = SQLiteDbContract.LocalEntry.COLUMN_ID;

        ((MainActivity)activity).sendMsgRequestLocalPosts(3);

        try {
            mListener = (onLocalFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        String[] projection = new String[1];
        projection[0] = SQLiteDbContract.LocalEntry.COLUMN_ID;

        ((MainActivity)activity).sendMsgRequestLocalPosts(3);

        try {
            mListener = (onLocalFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDestroy() {
        if (VERBOSE) {
            Log.v(TAG,"enter onDestroy...");
        }
        getLoaderManager().destroyLoader(LOCAL_LOADER_ID);

        adapter = null;
        mListener = null;
        PhotoManager.cancelDirectory(Constants.KEY_S3_REPLIES_DIRECTORY);
        if (VERBOSE) {
            Log.v(TAG,"exit onDestroy...");
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) {
            Log.v(TAG,"enter onCreateView...");
        }


        if (VERBOSE) {
            Log.v(TAG,"exit onCreateView...");
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local, container, false);
    }

    @Override
    public void onDestroyView() {

        imageAdapterView = null;

        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View cat, Bundle savedInstanceState) {
        super.onViewCreated(cat, savedInstanceState);
        if (VERBOSE) {
            Log.v(TAG,"enter onViewCreated...");
        }

        imageAdapterView = (ImageCursorAdapterView)cat.findViewById(R.id.imageAdapter);
        imageAdapterView.setAdapter(adapter);
        imageAdapterView.setOnPopListener(adapter);



        cat.findViewById(R.id.button_local_message).setOnClickListener(this);
        cat.findViewById(R.id.button_local_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (VERBOSE) Log.v(TAG, "entering LocalSaveToStash-OnClick...");

                PhotoView topView = (
                        (PhotoView) imageAdapterView
                                .getmTopCard()
                                .findViewById(R.id.photoView)
                );

                mListener.saveToStash(topView);
                if (VERBOSE) Log.v(TAG, "exiting LocalSaveToStash-OnClick...");
            }
        });

        if (VERBOSE) {
            Log.v(TAG,"exit onViewCreated...");
        }
    }

    private void fillData() {

        // Fields from the database (projection)
        // Must include the _id column for the adapter to work

         // Fields on the UI to which we map
       // int[] to = new int[] { R.id.imageID};

        getLoaderManager().restartLoader(LOCAL_LOADER_ID, null, this);

        /*adapter = new ImageStackCursorAdapter((MainActivity)getActivity(),
                R.layout.std_card_inner,
                null,
                FireFlyContentProvider.CONTENT_URI_LOCAL,
                0);*/
                //to, 0);

        adapter = new ImageAdapter(getActivity(),null);
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
            Log.v(TAG,"enter onCreateLoader...");
        }

        String[] projection = {
                LocalEntry.COLUMN_ID,
                LocalEntry.COLUMN_FROM_USER,
                LocalEntry.COLUMN_NAME_FILEPATH,
                LocalEntry.COLUMN_NAME_TEXT,
                LocalEntry.COLUMN_NAME_RESPONSE_ARN,
        };

        if (VERBOSE) {
            Log.v(TAG,"exit onCreateLoader...");
        }
        return new CursorLoader(getActivity(),
                FireFlyContentProvider.CONTENT_URI_LOCAL,
                projection,
                null,
                null,
                LocalEntry.COLUMN_NAME_WEIGHT);

    }
    public void handleLocalResponseState(Message msg) {
        if (VERBOSE) {
            Log.v(TAG,"entering handleLocalResponseState...");
        }

        switch (msg.arg2) {
            case HttpURLConnection.HTTP_OK:
                if (VERBOSE) Log.v(TAG,"Response code : " + msg.arg2);
                //imageAdapterView.setAdapter(adapter);
                break;

            default:
                //Toast.makeText(getActivity(), "Response code : " + msg.arg2, Toast.LENGTH_SHORT).show();
                break;
        }

        if (VERBOSE) {
            Log.v(TAG,"exiting handleReplyResponseState...");
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
        adapter.swapCursor(data);
        if (VERBOSE) {
            Log.v(TAG,"exit onLoadFinished...");
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (VERBOSE) {
            Log.v(TAG,"enter onLoaderReset...");
        }
        adapter.swapCursor(null);

        if (VERBOSE) {
            Log.v(TAG,"exit onLoaderReset...");
        }
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
                arn_column_index = newCursor.getColumnIndexOrThrow(LocalEntry.COLUMN_NAME_RESPONSE_ARN);
                url_column_index = newCursor.getColumnIndexOrThrow(LocalEntry.COLUMN_NAME_FILEPATH);
                fromUser_column_index = newCursor.getColumnIndexOrThrow(LocalEntry.COLUMN_FROM_USER);
                text_column_index = newCursor.getColumnIndexOrThrow(LocalEntry.COLUMN_NAME_TEXT);
            }

            return super.swapCursor(newCursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View outView = inflater.inflate(R.layout.std_card_inner, parent, false);
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
            if (!"".equals(captionText)) {
                TextView caption = ((TextView)
                        view.findViewById(R.id.textView_local_caption));
                caption.setText(captionText);
                caption.setVisibility(View.VISIBLE);
            }

            ((PhotoView)view.findViewById(R.id.photoView))
                    .setImageKey(Constants.KEY_S3_LOCAL_DIRECTORY,
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
                            delete(FireFlyContentProvider.CONTENT_URI_LOCAL,
                                    SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH
                                            + " = ?",
                                    selectionArgs);
                }
            }).start();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_local_message:
                String arn = (String)imageAdapterView.getmTopCard().getTag(R.integer.arn_key);
                ((MainActivity)getActivity()).localMessagePressed(arn);
                break;
        }
    }

    /**
     * interface 'onLocalFragmentInteractionListener'
     *
     */
    public interface onLocalFragmentInteractionListener {
        void setAnalyticsScreenName(String name);
        void sendMsgReportAnalyticsEvent(Bundle b);
        void sendMsgDownloadImage(String s3Directroy, String s3Key);
        void saveToStash(PhotoView imageToSave);
    }
}
