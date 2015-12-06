package com.jokrapp.android;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

import android.app.Fragment;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.amazonaws.mobile.AWSMobileClient;
import com.jokrapp.android.view.ImageCursorAdapterView;

import java.net.HttpURLConnection;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the=
 * to handle interaction events.
 */
public class MessageFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
     ImageStackMessageAdapter.onIndicatorClick{
    private final String TAG = "MessageFragment";
    private final boolean VERBOSE = false;

    private ListView messageIndicatorListView;
    private ImageStackMessageAdapter adapter;
    private ListAdapter messageListAdapter;
    private ImageCursorAdapterView imageAdapterView;

    private int MESSAGE_LOADER_ID = 10;

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

        getLoaderManager().destroyLoader(MESSAGE_LOADER_ID);

        mListener = null;
        adapter = null;

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

        imageAdapterView.setAdapter(adapter);
        messageIndicatorListView.setAdapter(adapter);

        cat.findViewById(R.id.button_message_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (VERBOSE) Log.v(TAG,"entering MessageSaveToStash-OnClick...");

                PhotoView topView = (
                        (PhotoView)imageAdapterView
                                .getmTopCard()
                                .findViewById(R.id.photoView)
                );

                mListener.saveToStash(topView);
                if (VERBOSE)Log.v(TAG,"exiting MessageSaveToStash-OnClick...");
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

        getLoaderManager().initLoader(MESSAGE_LOADER_ID, null, this);

        adapter = new ImageStackMessageAdapter((MainActivity)getActivity(),
                R.layout.std_msg_inner,
                null,
                FireFlyContentProvider.CONTENT_URI_MESSAGE,
                0);

        adapter.setOnIndicatorClickListener(this);
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
            Log.v(TAG,"enter onCreateLoader...");
        }

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
                FireFlyContentProvider.CONTENT_URI_MESSAGE, projection, null, null, null);

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

    @Override
    public void onIndicatorClick(View v) {
        Log.v(TAG,"clicked! : " + v.toString());

        //get the position of the view which was clicked
        int position = messageIndicatorListView.getPositionForView(v);

        //get the corresponding cardview
        View cardView = imageAdapterView.getChildAt(position);

        //show
        //cardView.bringToFront();
    }

    @Override
    public void onPop(final String filepath, final String ARN) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                /*getActivity().getContentResolver().
                        delete(FireFlyContentProvider.CONTENT_URI_MESSAGE,
                                SQLiteDbContract.MessageEntry.COLUMN_NAME_FILEPATH
                                        + " = " + filepath,
                                null);*/
                AWSMobileClient.defaultMobileClient().getPushManager().sendReadReceipt(ARN);
            }
        }).start();


    }

    /**
     * interface 'onLocalFragmentInteractionListener'
     *
     */
    public interface onMessageFragmentInteractionListener {
        void saveToStash(PhotoView view);
    }
}
