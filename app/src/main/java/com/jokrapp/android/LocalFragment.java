package com.jokrapp.android;


import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * class "LocalFragment'
 *
 * This Fragment represents the "local" UI.
 *
 */
public class LocalFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private final String TAG = "LocalFragment";
    private final boolean VERBOSE = false;
    private final int LOCAL_LOADER_ID = 2;

    private ImageStackCursorAdapter adapter;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        String[] projection = new String[1];
        projection[0] = SQLiteDbContract.LocalEntry.COLUMN_ID;

        ((MainActivity)activity).requestImages(3);

        try {
            mListener = (onLocalFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) {
            Log.v(TAG,"enter onDestroy...");
        }
        mListener = null;
        getLoaderManager().destroyLoader(LOCAL_LOADER_ID);
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


        if (VERBOSE) {
            Log.v(TAG,"exit onCreateView...");
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local, container, false);
    }

    @Override
    public void onViewCreated(View cat, Bundle savedInstanceState) {
        super.onViewCreated(cat, savedInstanceState);
        if (VERBOSE) {
            Log.v(TAG,"enter onViewCreated...");
        }

        imageAdapterView = (ImageCursorAdapterView)cat.findViewById(R.id.imageAdapter);
        imageAdapterView.setAdapter(adapter);

        if (VERBOSE) {
            Log.v(TAG,"exit onViewCreated...");
        }
    }

    private void fillData() {

        // Fields from the database (projection)
        // Must include the _id column for the adapter to work
        String[] from = new String[] {SQLiteDbContract.LocalEntry.COLUMN_NAME_FILEPATH};
        // Fields on the UI to which we map
       // int[] to = new int[] { R.id.imageID};

        getLoaderManager().initLoader(LOCAL_LOADER_ID, null, this);

        adapter = new ImageStackCursorAdapter((MainActivity)getActivity(), R.layout.std_card_inner,null,
                FireFlyContentProvider.CONTENT_URI_LOCAL,
                null,0);
                //to, 0);
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
                SQLiteDbContract.LocalEntry.COLUMN_ID,
                SQLiteDbContract.LocalEntry.COLUMN_FROM_USER,
                SQLiteDbContract.LocalEntry.COLUMN_NAME_FILEPATH,
        };

        if (VERBOSE) {
            Log.v(TAG,"exit onCreateLoader...");
        }
        return new CursorLoader(getActivity(),
                FireFlyContentProvider.CONTENT_URI_LOCAL,
                projection,
                null,
                null,
                SQLiteDbContract.LocalEntry.COLUMN_NAME_WEIGHT);

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

    /**
     * interface 'onLocalFragmentInteractionListener'
     *
     */
    public interface onLocalFragmentInteractionListener {
        public void sendMsgReportAnalyticsEvent(int event, String action);

    }
}
