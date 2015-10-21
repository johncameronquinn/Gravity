package com.jokrapp.android;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.ParcelableSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;
import com.jokrapp.android.SQLiteDbContract.LiveThreadEntry;


/**
 * Author/Copyright John C. Quinn, All Rights Reserved.
 *
 * class 'LiveFragment'
 *
 * contains the live portion of the app
 *
 * //todo use a disk cache for storing thread topics
 */
public class LiveFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener {
    public static final boolean VERBOSE = true;
    private static final String TAG = "LiveFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_ADAPTER = "ada";

    private final String CURRENT_THREAD_KEY = "threadkey";
   // private final String THREAD_PAGER_KEY = "pagerkey";
    private final String VIEW_HACK_KEY = "pagerkey";

    private final int LIVE_LOADER_ID = 1;
    private int numberOfThreads;

    // TODO: Rename and change types of parameters
    private int currentThread;

    public static final int NO_LIVE_THREADS_ID = -1;

    private VerticalViewPager threadPager;
    private CursorPagerAdapter mAdapter;
    private WeakReference<ReplyFragment> mReplyFragmentReference = new WeakReference<>(null);


    private onLiveFragmentInteractionListener mListener;



    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param messageCount the current number of messages that have been recieved.
     * @param threadPosition your thread's current position in live.
     * @return A new instance of fragment LocalFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LiveFragment newInstance(String messageCount, String threadPosition) {
        LiveFragment fragment = new LiveFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, messageCount);
        fragment.setArguments(args);
        return fragment;
    }

    public void setReplyFragment(ReplyFragment fragment) {
        mReplyFragmentReference = new WeakReference<ReplyFragment>(fragment);
    }

    public ReplyFragment getReplyFragment() {return  mReplyFragmentReference.get();}


    /**
     * method 'createThread'
     *
     * displays a view allowing the user to enter the necessary information to create a thread
     *
     */
    /*public void createThread(MainActivity activity) {
        activity.disableScrolling();
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout root = (FrameLayout)activity.findViewById(R.id.live_root_layout);
        View v = inflater.inflate(R.layout.live_post,
                root,
                false);

        getPostButtonListener(this).setCreateView(v);
        v.findViewById(R.id.button_cancel_post).setOnClickListener(getPostButtonListener(this));
        v.findViewById(R.id.button_confirm_post).setOnClickListener(getPostButtonListener(this));
         root.addView(v,root.getChildCount());
    }*/

    public LiveFragment() {
        // Required empty public constructor
    }

/***************************************************************************************************
* LIFECYCLE METHODS
*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onCreate...");

        if (savedInstanceState != null) {
            Log.d(TAG,"restoring live state...");
            currentThread = savedInstanceState.getInt(CURRENT_THREAD_KEY);
        } else {
            currentThread = NO_LIVE_THREADS_ID;
        }

        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
        if (savedInstanceState != null) {
        } else {
            }

        String[] projection = {
                LiveThreadEntry.COLUMN_ID,
                LiveThreadEntry.COLUMN_NAME_NAME,
                LiveThreadEntry.COLUMN_NAME_TITLE,
                LiveThreadEntry.COLUMN_NAME_DESCRIPTION,
                LiveThreadEntry.COLUMN_NAME_FILEPATH,
                LiveThreadEntry.COLUMN_NAME_THREAD_ID,
                LiveThreadEntry.COLUMN_NAME_REPLIES,
                LiveThreadEntry.COLUMN_NAME_UNIQUE
        };

        mAdapter = new CursorPagerAdapter<>(getFragmentManager(),LiveThreadFragment.class,projection,null);


        if (VERBOSE) Log.v(TAG,"initializing loader at id " + LIVE_LOADER_ID);
        getLoaderManager().restartLoader(LIVE_LOADER_ID, null, this);
        if (VERBOSE) Log.v(TAG,"exiting onCreate...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) Log.v(TAG,"entering onDestroy...");


        if (VERBOSE) Log.v(TAG,"destroying loader at id " + LIVE_LOADER_ID);
        getLoaderManager().destroyLoader(LIVE_LOADER_ID);

        if (VERBOSE) Log.v(TAG,"exiting onDestroy...");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (onLiveFragmentInteractionListener)context;
        mListener.sendMsgRequestLiveThreads();


        receiver = new LiveThreadReceiver();
        IntentFilter filter = new IntentFilter(Constants.ACTION_IMAGE_LOADED);
        context.registerReceiver(receiver, filter);
    }

    //todo, this is a workaround for a bug and can be removed in the future
    public void onAttach(Activity context) {
        super.onAttach(context);
        mListener = (onLiveFragmentInteractionListener)context;
        mListener.sendMsgRequestLiveThreads();


        receiver = new LiveThreadReceiver();
        IntentFilter filter = new IntentFilter(Constants.ACTION_IMAGE_LOADED);
        context.registerReceiver(receiver, filter);
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (VERBOSE) Log.v(TAG,"entering onSaveInstanceState...");
        outState.putInt(CURRENT_THREAD_KEY, currentThread);
    //    outState.putInt(THREAD_PAGER_KEY, threadPager.getCurrentItem());

        /*View v = threadPager.getChildAt(threadPager.getCurrentItem());
        SparseArray<Parcelable> viewsave = new SparseArray<>();
        v.saveHierarchyState(viewsave);
        outState.putSparseParcelableArray(VIEW_HACK_KEY, viewsave);*/


        if (VERBOSE) Log.v(TAG,"exiting onSaveInstanceState...");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.v(TAG,"entering onCreateView...");
        //ImageView imageView = (ImageView)getActivity().findViewById(R.id.imageView3);
        //MainActivity activity;
        //if (getActivity() instanceof MainActivity) {
        //    activity.loadBitmap(activity.theImages.get(5),imageView);
        //    activity = (MainActivity)getActivity();
        //}


        // Inflate the layout for this fragment
        //inflater.inflate(R.layout.fragment_live_thread,threadPager,false);//
        // FrameLayout rootLayout = (FrameLayout)rootView.findViewById(R.id.live_root_layout);
        // View threadView = inflater.inflate(R.layout.fragment_live_thread,rootLayout,false);
        // rootLayout.addView(threadView);

        View rootView = inflater.inflate(R.layout.fragment_live,container,false);
        rootView.findViewById(R.id.button_live_refresh).setOnClickListener(getButtonListener(this));
        rootView.findViewById(R.id.button_new_thread).setOnClickListener(getButtonListener(this));
        ((SeekBar)rootView.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(getButtonListener(this));

        if (VERBOSE) Log.v(TAG,"exiting onCreateView...");
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onViewCreated...");

        threadPager = (VerticalViewPager)view.findViewById(R.id.live_thread_pager);
        threadPager.setOnPageChangeListener(this);
        threadPager.setAdapter(mAdapter);


        if (savedInstanceState != null) {
            Log.d(TAG,"savedState was not null");
//            int instate = savedInstanceState.getInt(THREAD_PAGER_KEY);
       //     int outstate = instate + 3;

      /*      View a = new View(getActivity());
            a.restoreHierarchyState(savedInstanceState.getSparseParcelableArray(VIEW_HACK_KEY));
            threadPager.removeViewAt(0);
            threadPager.addView(a,0);*/
        }

        if (VERBOSE) Log.v(TAG,"exiting onViewCreated...");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity)activity).sendMsgRequestLiveThreads();
    }*/

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(receiver);
    }

    public int getCurrentThread() {
        return currentThread;
    }
/***************************************************************************************************
 * USER INTERACTION
 */
    /**
     * method 'onActivityResult'
     *
     * called when the user selects an image to be loaded into live, or cancels
     *
     * @param requestCode code supplied to the activity
     * @param resultCode result be it cancel or otherwise
     * @param data the uri returned
     */
    private final int SELECT_PICTURE = 1;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                Bitmap b = null;
                try {
                    b = BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(selectedImageUri));
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "error decoding stream from returned URI...");
                }

            } else {

            }
        }
    }

    /**
     * class 'liveButtonListener
     *
     * listens to all the general live thread interactions
     */
    public class LiveButtonListener implements SeekBar.OnSeekBarChangeListener, View.OnClickListener,ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG,"OnClickRegistered..." + v.toString());
            }

            switch (v.getId()) {

                case R.id.button_live_refresh:

                    ((MainActivity)getActivity()).sendMsgRequestLiveThreads();
                    break;


                case R.id.button_new_thread:
                    setSeekMode(v);
                    break;
            }
        }


        final int OPEN_CAMERA_VALUE = 0;
        final int STARTING_VALUE = 50;
        final int OPEN_STASH_VALUE = 100;
        int value = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            value = progressValue;
            switch (progressValue) {
                case OPEN_CAMERA_VALUE:
                    Toast.makeText(getActivity(),"Opening camera...",Toast.LENGTH_SHORT).show();
                    ((MainActivity)getActivity()).takeLivePicture();
                    break;

                case OPEN_STASH_VALUE:
                    Log.i(TAG, "opening gallery");
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
                    break;
                case STARTING_VALUE:
                    //do nothing. possibly hide the seekbar?
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        private SeekBar seekBar;
        private int destValue;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //todo animator should interpolate the seekbar to the closest node (0,50,100)
            final int ANIMATION_SPEED_FACTOR = 1000;
            final int ANIMATION_ACCEL_FACTOR = 3;

            this.seekBar = seekBar;

            if (value < 20) {
                destValue = OPEN_CAMERA_VALUE;
            } else if (value > 80) {
                destValue = OPEN_STASH_VALUE;
            } else {
                destValue = STARTING_VALUE;
            }
            double distance = Math.abs(destValue - value);

            double speedRatio = (distance / seekBar.getMax());
            Log.d(TAG,"The distance is " + distance + " and the speedRatio is " + speedRatio);
            Log.d(TAG, "The adjusted duration is " + Math.round(speedRatio * ANIMATION_SPEED_FACTOR));

            ValueAnimator anim = ValueAnimator.ofInt(value, destValue);
            anim.setInterpolator(new AccelerateInterpolator(ANIMATION_ACCEL_FACTOR));
            anim.setDuration(Math.round(speedRatio * ANIMATION_SPEED_FACTOR));
            anim.addUpdateListener(this);
            anim.start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int animProgress = (Integer) animation.getAnimatedValue();
            seekBar.setProgress(animProgress);

            if (animProgress == destValue) {
                setSeekMode(null);
            }
        }

        private void setSeekMode(View v) {
            if (v != null) { //button_new_thread was pressed, set seek mode
             ViewGroup group = (ViewGroup)v.getParent();
                group.findViewById(R.id.button_new_thread).setVisibility(View.INVISIBLE);
                group.findViewById(R.id.live_thread_number).setVisibility(View.INVISIBLE);
                SeekBar bar = ((SeekBar)group.findViewById(R.id.seekBar));
                bar.setVisibility(View.VISIBLE);
                bar.setOnSeekBarChangeListener(this);
            } else {
                if (getView() != null) {
                    View group = getView();
                    group.findViewById(R.id.button_new_thread).setVisibility(View.VISIBLE);
                    group.findViewById(R.id.live_thread_number).setVisibility(View.VISIBLE);
                    SeekBar bar = ((SeekBar)group.findViewById(R.id.seekBar));
                    bar.setProgress(STARTING_VALUE);
                    value = STARTING_VALUE;
                    bar.setVisibility(View.INVISIBLE);
                    bar.setOnSeekBarChangeListener(null);
                }
            }
        }

    }

    /**
     * static method getButtonListener
     *
     * this is used to create a singleton reference to a buttonlistener, allowing one
     * buttonlistener to handle all button interactions
     *
     *
     * @param parent the context in which this functions
     * @return a ButtonListener to be used by all the buttons in CameraFragment
     */
    private static WeakReference<LiveButtonListener> buttonListenerReference;

    public static LiveButtonListener getButtonListener(LiveFragment parent) {
        if (buttonListenerReference == null) {
            buttonListenerReference = new WeakReference<>(parent.new LiveButtonListener());
        }

        return buttonListenerReference.get();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (VERBOSE) Log.v(TAG, "entering onPageSelected... page " + position + " selected.");
        //Toast.makeText(getActivity(),"page " + position + " selected.",Toast.LENGTH_SHORT).show();
        ((TextView) getActivity().findViewById(R.id.live_thread_number)).setText(String.valueOf(position));

        currentThread = getCurrentThreadID();
        mListener.sendMsgRequestReplies(currentThread);


        if (VERBOSE) Log.v(TAG,"initializing loader at id " + ReplyFragment.REPLY_LOADER_ID);

        Bundle args = new Bundle();
        args.putString(CURRENT_THREAD_KEY, String.valueOf(currentThread));
        getReplyFragment().setCurrentThread(String.valueOf(currentThread));
        getLoaderManager().restartLoader(ReplyFragment.REPLY_LOADER_ID, args, getReplyFragment());

        //report thread view to analytics service
        if (mListener != null) {
            mListener.sendMsgReportAnalyticsEvent(Constants.LIVE_THREADVIEW_EVENT, "Thread~ID: " + currentThread + "," +
                    " Position: " + position);
        }
        if (VERBOSE) Log.v(TAG, "exiting onPageSelected...");
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }


    /**************************************************************************************************
     * CONTENT LOADING
      */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (VERBOSE) Log.v(TAG,"enter onCreateLoader...");

        String[] projection = {
                LiveThreadEntry.COLUMN_ID,
                LiveThreadEntry.COLUMN_NAME_NAME,
                LiveThreadEntry.COLUMN_NAME_TITLE,
                LiveThreadEntry.COLUMN_NAME_DESCRIPTION,
                LiveThreadEntry.COLUMN_NAME_FILEPATH,
                LiveThreadEntry.COLUMN_NAME_THREAD_ID,
                LiveThreadEntry.COLUMN_NAME_REPLIES,
                LiveThreadEntry.COLUMN_NAME_UNIQUE
        };



        if (VERBOSE) Log.v(TAG,"loader created.");
        if (VERBOSE) Log.v(TAG,"exit onCreateLoader...");
        return new CursorLoader(getActivity(),
                FireFlyContentProvider.CONTENT_URI_LIVE,
                projection,
                null,
                null,
                LiveThreadEntry.COLUMN_ID);
        //sort by column ID
    }

    private int getCurrentThreadID() {
        int out;

        LiveThreadFragment f = (LiveThreadFragment)mAdapter.getItem(threadPager.getCurrentItem());
        if (f != null) {
            String s = f.getThreadID();
            if (s == null) {
                out = 0;
            } else {
                out = Integer.parseInt(s);
            }
        } else {
            out = 0;
        }

        return out;
      //  return  Integer.valueOf(((LiveThreadFragment)mAdapter.getItem(threadPager.getCurrentItem())).getThreadID());
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (VERBOSE) Log.v(TAG,"enter onLoadFinished...");

        Log.i(TAG, "Live cursor finished loading data");
        mAdapter.swapCursor(data);

        currentThread = getCurrentThreadID();

        Log.d(TAG, "Returned cursor contains: " + data.getCount() + " rows.");


        if (VERBOSE) Log.v(TAG,"exit onLoadFinished...");
    }


    /**
     * method 'onLoaderReset'
     *
     * called when the data at the loader is no longer available
     *
     * @param loader the loader which was reset
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (VERBOSE) Log.v(TAG,"enter onLoaderReset...");

        mAdapter.swapCursor(null);

        if (VERBOSE) Log.v(TAG,"exit onLoaderReset...");
    }

    public interface onLiveFragmentInteractionListener {
        public void sendMsgReportAnalyticsEvent(int event, String name);
        public void sendMsgRequestLiveThreads();
        public void sendMsgRequestReplies(int threadID);
    }


/***************************************************************************************************
 * DOWNLOAD STATE RECEIVER
 */

public LiveThreadReceiver receiver;

public class LiveThreadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LiveFragment.VERBOSE) {
            Log.v(TAG, "received intent...");
        }

        String path = intent.getExtras().getString(Constants.KEY_S3_KEY);
        View v = threadPager.findViewWithTag(path);

        if (v.isShown()) {
            if (VERBOSE) Log.v(TAG,"Image loaded from view is visible, decoding and displaying...");
            ImageView imageView = (ImageView) v.findViewById(R.id.live_thread_imageView);
            ProgressBar bar = (ProgressBar) v.findViewById(R.id.live_thread_progressbar);

            String[] params = {path};

            new imageLoadTask(imageView, bar).execute(
                    params);
        } else {
            if (VERBOSE) Log.v(TAG,"image is now shown do nothing...");
        }

    }
}

private class imageLoadTask extends AsyncTask<String,Integer,Bitmap>{
    WeakReference<ImageView> imageViewWeakReference;
    WeakReference<ProgressBar> progressBarWeakReference;

    public imageLoadTask(ImageView v, ProgressBar b) {
        imageViewWeakReference = new WeakReference<>(v);
        progressBarWeakReference= new WeakReference<>(b);
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        String path = params[0];

        if (VERBOSE) Log.v(TAG,"decoding image at path : " + path);

        return BitmapFactory.decodeFile(getActivity().getCacheDir().toString() + "/" + path);

    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        imageViewWeakReference.get().setImageBitmap(bitmap);
        progressBarWeakReference.get().setVisibility(View.INVISIBLE);
    }
}



}
