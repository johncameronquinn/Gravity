package com.jokrapp.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.app.Fragment;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.Tracker;
import com.jokrapp.android.user.IdentityManager;
import com.jokrapp.android.util.LogUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Author/Copyright John C. Quinn All Rights Reserved.
 * created: 3-26-2015
 * date last modified: 2015-06-17
 *
 * this is the main activity for the app Proxi
 *
 * there is only one activity, and all the other features are managed as fragments
 */
public class MainActivity extends Activity implements CameraFragment.OnCameraFragmentInteractionListener,
LocalFragment.onLocalFragmentInteractionListener, LiveFragment.onLiveFragmentInteractionListener, ViewPager.OnPageChangeListener {
    private static String TAG = "MainActivity";
    private static final boolean VERBOSE = true;
    private static final boolean SAVE_LOCALLY = false;

    //private static String imageDir;

    //UI
    private static CustomViewPager mPager;
    private static MainAdapter mAdapter;


    /** FRAGMENT MANAGEMENT
     */
    private static final int MESSAGE_LIST_POSITION = 0;
    private static final int LOCAL_LIST_POSITION = 1;
    private static final int CAMERA_LIST_POSITION = 2;
    private static final int LIVE_LIST_POSITION = 3;
    private static final int REPLY_LIST_POSITION = 4;

    private static final String MESSAGE_PAGER_TITLE = "Message";
    private static final String LOCAL_PAGER_TITLE = "Local";
    private static final String LIVE_PAGER_TITLE = "Live";
    private static final String CAMERA_PAGER_TITLE = "Camera";
    private static final String REPLY_PAGER_TITLE = "Reply";

    private static final int NUMBER_OF_FRAGMENTS = 5;

    private static WeakReference<MessageFragment> MessageFragReference = new WeakReference<>(null);
    private static WeakReference<CameraFragment> CameraFragReference = new WeakReference<>(null);
    private static WeakReference<LiveFragment> LiveFragReference = new WeakReference<>(null);
    private static WeakReference<LocalFragment> LocalFragReference = new WeakReference<>(null);


    /** CAMERA MANAGEMENT
     */
    private boolean isCamera = false;
    Messenger cameraMessenger;
   // private CameraHandler cameraHandler;


    private static final int CAMERA_POSITION_BACK = 0;
    private static final int CAMERA_POSITION_FRONT = 1;
    private static int currentCamera = CAMERA_POSITION_BACK;

    private String messageTarget;

    /** SERVICE MANAGEMENT
     */
    private boolean isBound = false;

    private Messenger mService;

    private final UIHandler uiHandler = new UIHandler();
    final Messenger messageHandler = new Messenger(uiHandler);

    /** AWS CONNECTION
     */
    private IdentityManager identityManager;


    /** ANALYTICS
     */
    private Tracker mTracker;


    /**
     * ********************************************************************************************
     * <p>
     * LIFECYCLE MANAGEMENT
     * <p>
     * /**
     * Handles the initial loading of the app. Splash is defined in AndroidManifest
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "enter onCreate...");
        if (savedInstanceState != null) {
           //Restore the camerafragment's instance

        }
        super.onCreate(savedInstanceState);
        uiHandler.setParent(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Window window = getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.jpallete_neutral_blue));
        }

        checkForLocationEnabled(this);

        initializeAWS();

        initializeUI();

        initializeAnalytics();

        Log.d(TAG, "exit onCreate...");
    }
/***************************************************************************************************
* INITIALIZATION METHODS
 *
 *
 * These methods all occur within onCreate, and exist for nicencess
**/
    /**
     * method 'initializeUI'
     *
     * creates the UI, sets the 5 fragment viewpaging, etc
     */
    private void initializeUI() {
        setContentView(R.layout.activity_main);
        mAdapter = new MainAdapter(getFragmentManager());
        mPager = (CustomViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        mPager.addOnPageChangeListener(this);
    }

    /**
     * method 'initializeAWS'
     *
     * initializes the connection to AWS
     */
    private void initializeAWS() {
        // Obtain a reference to the mobile client. It is created in the Splash Activity.
        AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();
        if (awsMobileClient == null) {
            // In the case that the activity is restarted by the OS after the application
            // is killed we must redirect to the splash activity to handle initialization.
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        // Obtain a reference to the identity manager.
        identityManager = awsMobileClient.getIdentityManager();

    }

    /**
     * method 'initializeAnalytics'
     *
     * initializes the connection to the analytics services
     */
    private void initializeAnalytics() {
        // [START shared_tracker]
        //obtain the shared Tracker instance
        AnalyticsApplication application = (AnalyticsApplication)getApplication();
        mTracker = application.getDefaultTracker();
        // [END shared_tracker]
    }


    public Messenger getMessenger() {
        return mService;
    }

    /***************************************************************************************************
     * LIFECYCLE METHODS
      */

    @Override
    protected void onStart() {
        Log.d(TAG, "enter onStart...");
        super.onStart();


        Log.d(TAG, "binding the service to this class, creating if necessary");
        Intent intent = new Intent(this, DataHandlingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

       // cameraHandler = new CameraHandlerThread(this).getCameraHandler();
        cameraMessenger = new Messenger(getCameraHandlerSingleton(this));


        try {
            Message msg = Message.obtain(null, MSG_CONNECT_CAMERA, currentCamera, 0);
            cameraMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to connect camera");
        }


        if (isBound) {
            Log.d(TAG, "sending message to connect GoogleApiClient...");
            // Create and send a message to the service, using a supported 'what' value
            Message msg1 = Message.obtain(null, DataHandlingService.MSG_CONNECT_CLIENT, 0, 0);
              try {
                mService.send(msg1);
             } catch (RemoteException e) {
                  e.printStackTrace();
            }
        }

        Log.d(TAG, "exit onStart...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "enter onResume...");


        Log.d(TAG, "exit onResume...");
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "enter onPause...");



        Log.d(TAG, "exit onPause...");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "enter onStop...");

        try {
            cameraMessenger.send(Message.obtain(null, MSG_DISCONNECT_CAMERA));

        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to connect camera");
        }


        if (VERBOSE) Log.v(TAG,"Releseing reference to cameraHandler");
        cameraHandlerWeakReference = new WeakReference<>(null);


        //connect the googleApiC

        //unbind the service now
        if (isBound) {

             Log.d(TAG, "sending message to disconnect GoogleApiClient...");
             Message msg1 = Message.obtain(null, DataHandlingService.MSG_DISCONNECT_CLIENT, 0, 0);

            try {
                mService.send(msg1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "unbinding the service...");
            unbindService(mConnection);
            isBound = false;
        }

        Log.d(TAG, "exit onStop...");
    }


    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "enter onDestroy...");

        try {
            cameraMessenger.send(Message.obtain(null, MSG_ACTIVITY_DESTROYED));
        } catch (RemoteException e) {
            Log.e(TAG,"error notifying that fragment was destroyed",e);
        }

        Log.d(TAG, "exit onDestroy...");
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "enter onSavedInstanceState...");

        Log.d(TAG, "exit onSavedInstanceState...");
    }

    /**
     * method 'onTrimMemory'
     *
     * called when the android system sends out various broadcasts regarding memory recovery
     * @param level the type of broadcast sent out
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN:
                Log.d(TAG,"Trim memory UI Hidden called");
                break;

        }


    }

    /***********************************************************************************************
     *
     * USER INTERACTION
     *
     */
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {


        return super.onCreateView(name, context, attrs);
    }

    public void onNotImplemented(View v) {
        Toast.makeText(this,"Not yet implemented...",Toast.LENGTH_SHORT).show();
    }


    /**
     * method 'onLongClick'
     *
     * callback from each respective fragment
     *
     * StashActivity is started onLongPress at any point in the app, with which page it loads
     * correlating to the fragment which it was launched from
     *
     * @param v the view that was clicked
     * @return whether or not the clickEvent was consumed
     */
    public boolean onLongClick(View v) {

        final int LOCAL_SETTINGS = 0;
        final int GALLERY = 1;
        final int LIVE_SETTINGS = 2;

        int startPage = GALLERY;
        switch (mPager.getCurrentItem()) {

            case MESSAGE_LIST_POSITION:
            case LOCAL_LIST_POSITION:
                startPage = LOCAL_SETTINGS;
                break;

            case CAMERA_LIST_POSITION:
                startPage = GALLERY;
                break;

            case LIVE_LIST_POSITION:
            case REPLY_LIST_POSITION:
                startPage = LIVE_SETTINGS;
                break;
        }
        Intent stashActivtyIntent = new Intent(this,StashActivity.class);
        stashActivtyIntent.putExtra(StashActivity.STARTING_PAGE_POSITION_KEY, startPage);
        startActivity(stashActivtyIntent);

        return true;
    }

    public void onLocalMessagePressed(View view) {
        if (VERBOSE) {
            Toast.makeText(this,"Message pressed: " + view.getTag(),Toast.LENGTH_LONG).show();
        }

        CameraFragReference.get().startMessageMode((String)view.getTag());
        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        mPager.setPagingEnabled(false);
    }

    private Runnable mLongPressed = new Runnable() {
        public void run() {
            final int LOCAL_SETTINGS = 0;
            final int GALLERY = 1;
            final int LIVE_SETTINGS = 2;

            int startPage = GALLERY;
            switch (mPager.getCurrentItem()) {

                case MESSAGE_LIST_POSITION:
                case LOCAL_LIST_POSITION:
                    Toast.makeText(getApplicationContext(),"Opening Local Settings...",Toast.LENGTH_SHORT).show();
                    startPage = LOCAL_SETTINGS;
                    break;

                case CAMERA_LIST_POSITION:
                    Toast.makeText(getApplicationContext(),"Opening Stash Gallery...",Toast.LENGTH_SHORT).show();
                    startPage = GALLERY;
                    break;

                case LIVE_LIST_POSITION:
                case REPLY_LIST_POSITION:
                    Toast.makeText(getApplicationContext(),"Opening Live Settings...",Toast.LENGTH_SHORT).show();
                    startPage = LIVE_SETTINGS;
                    break;
            }
            Intent stashActivtyIntent = new Intent(getApplicationContext(),StashActivity.class);
            stashActivtyIntent.putExtra(StashActivity.STARTING_PAGE_POSITION_KEY, startPage);
            startActivity(stashActivtyIntent);
        }
    };


 //   private float xpos = 0;
 //   private float ypos = 0;

    /**
     * method 'dispatchTouchEvent'
     *
     * all incoming touchEvents from the window are first checked here before being dispatched
     * to their respective view's.
     *
     * in this method, we test for a long click and handle it accordingly.
     *
     * @param
     * @return
     */
   /* @Override
    public boolean dispatchTouchEvent(MotionEvent event) {


        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            uiHandler.postDelayed(mLongPressed, 2000);
            xpos = event.getX();
            ypos = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            uiHandler.removeCallbacks(mLongPressed);
        }


        return super.dispatchTouchEvent(event);
    }*/

    public void onMessageRefresh(View v) {
        sendMsgRequestLocalMessages();
    }

    public void onLocalRefresh(View v) {
        sendMsgRequestLocalPosts(3);
    }

    public void onLocalReplyPressed(View view) {
        if (VERBOSE) {
            Toast.makeText(this,"Message pressed: " + view.getTag(),Toast.LENGTH_LONG).show();
        }
        CameraFragReference.get().setMessageTarget((String) view.getTag());

        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        mPager.setPagingEnabled(false);

    }

    public void onLiveTakeReplyPicture(View view) {
        takeReplyPicture();
    }


    /**
     * method 'onLocalBlockPressed'
     *
     * called when a user pressed the block button on an image in local
     *
     * @param view the view of the button that was pressed.
     */
    public void onLocalBlockPressed(View view) {
        if (VERBOSE) Log.v(TAG,"Block button pressed.");

        Toast.makeText(this,"Blocking user " + (String)view.getTag(),Toast.LENGTH_SHORT).show();

        if (isBound) {
            if (VERBOSE) Log.v(TAG,"sending message to block ");
            Message msg = Message.obtain(null, DataHandlingService.MSG_BLOCK_USER);
            Bundle b = new Bundle();
            b.putString(Constants.MESSAGE_TARGET, (String) view.getTag());
            msg.setData(b);

            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to block user",e);
            }
        }
    }

    @Override
    public void createLiveThread() {
        mPager.setCurrentItem(LIVE_LIST_POSITION);
    }

    public void takeLivePicture() {
        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        CameraFragReference.get().startLiveMode();
    }

    public void takeReplyPicture() {
        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        CameraFragReference.get().startReplyMode();
    }

    public void sendMsgRequestReplies(int threadNumber) {
        Log.i(TAG, "refreshing replies for thread: " + threadNumber);

        if (isBound) {
            try {
                mService.send(Message.obtain(null,DataHandlingService.MSG_REQUEST_REPLIES, threadNumber, 0));
            } catch (RemoteException e) {
                Log.e(TAG, "error sending message to background service...", e);
            }
        }
    }

    /**
     * method 'sendImageToLocal'
     *
     * {@link com.jokrapp.android.CameraFragment.OnCameraFragmentInteractionListener}
     *
     * notifys the mainActivity that an image was just saved, so that the activity
     * can message the service to send this image to the server
     *
     * @param filePath the file path of the image saved
     * @param currentCamera the camera that the image was taken with
     */
    public void sendImageToLocal(String filePath, int currentCamera, String text) {
        if (VERBOSE) Log.v(TAG,"entering sendImageToLocal...");

        if (isBound) {
            Log.d(TAG, "sending message to server containing filepath to load...");

            Message msg = Message.obtain(null, DataHandlingService.MSG_SEND_IMAGE,currentCamera,0);
            Bundle b = new Bundle();
            b.putString(Constants.KEY_S3_KEY, filePath);
            b.putString(Constants.KEY_TEXT,text);

            if (messageTarget != null) {
                Log.d(TAG, "Sending message to user : " + messageTarget);
                b.putString(Constants.MESSAGE_TARGET, messageTarget);
            } else {
                Log.d(TAG, "Sending local broadcast...");
            }
            msg.setData(b);

            try {
                msg.replyTo = messageHandler;
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        messageTarget = null;
        if (VERBOSE) Log.v(TAG,"exiting sendImageToLocal...");
    }


    /**
     * class 'MainAdapter'
     * <p>
     * handles the fragment instantiations, instantiating all of the fragments
     * permanently, and allows their navigation to be swipeable
     * <p>
     * makes use of the android support library
     */
    public static class MainAdapter extends FragmentStatePagerAdapter {


        public MainAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUMBER_OF_FRAGMENTS;
        }

        /**
         * instantiates each fragment, and places at its individual placement
         *f
         * uses weakreferences to each fragment to ensure calls are handled properly
         *
         * @param position current position the pager to place the fragment 0 through 2
         * @return the fragment to be instantiated
         */
        @Override
        public Fragment getItem(int position) {
            if (VERBOSE) {
                Log.v(TAG,"getting item at position " + position);
            }
            if (position == MESSAGE_LIST_POSITION) {
                if (MessageFragReference.get() == null){
                    MessageFragReference = new WeakReference<>(new MessageFragment());
                } return MessageFragReference.get();
            } else if (position == LOCAL_LIST_POSITION) {
                if (LocalFragReference.get() == null) {
                    LocalFragReference = new WeakReference<>(new LocalFragment());
                }

                return LocalFragReference.get();
            } else if (position == LIVE_LIST_POSITION) {
                if (LiveFragReference.get() == null){
                    LiveFragReference = new WeakReference<>(LiveFragment.newInstance("a","a"));
                }

                return LiveFragReference.get();
            } else if (position == CAMERA_LIST_POSITION) {
                if (CameraFragReference.get() == null){
                    CameraFragReference = new WeakReference<>(CameraFragment.newInstance(0));
                }

                return CameraFragReference.get();
            } else if (position == REPLY_LIST_POSITION) {
                if (LiveFragReference.get().getReplyFragment() == null) {
                    ReplyFragment f = ReplyFragment.newInstance(LiveFragReference.get()
                            .getCurrentThread());
                    LiveFragReference.get().setReplyFragment(f);
                    return f;
                } else {
                    return LiveFragReference.get().getReplyFragment();
                }
            } else {
                Log.e(TAG, "Invalid fragment position loaded");
                return null;
            }
        }

        /**
         * method 'getPageTitle'
         *
         * called by the PagerTitleStrip within the ViewPager
         * used to title the various tabs of the ViewPager
         *
         * @param position of the page who's title needs to be returned
         * @return String of the title
         */
        @Override
        public CharSequence getPageTitle(int position) {
                    switch (position) {
                    case MESSAGE_LIST_POSITION:
                        return MESSAGE_PAGER_TITLE;
                    case LOCAL_LIST_POSITION:
                        return LOCAL_PAGER_TITLE;
                    case CAMERA_LIST_POSITION:
                        return CAMERA_PAGER_TITLE;
                    case LIVE_LIST_POSITION:
                        return LIVE_PAGER_TITLE;
                    case REPLY_LIST_POSITION:
                        return REPLY_PAGER_TITLE;
                }

                return null;
            }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    int previousPosition=CAMERA_LIST_POSITION;

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case CAMERA_LIST_POSITION:
                sendMsgReportAnalyticsEvent(Constants.FRAGMENT_VIEW_EVENT,"Fragment~" + CAMERA_PAGER_TITLE);
                mPager.findViewById(R.id.pager_tab_strip).setVisibility(View.VISIBLE);
            break;

            case LOCAL_LIST_POSITION:
                sendMsgReportAnalyticsEvent(Constants.FRAGMENT_VIEW_EVENT,"Fragment~" + LOCAL_PAGER_TITLE);
                mPager.findViewById(R.id.pager_tab_strip).setVisibility(View.VISIBLE);
                break;

            case MESSAGE_LIST_POSITION:
                sendMsgReportAnalyticsEvent(Constants.FRAGMENT_VIEW_EVENT,"Fragment~" + MESSAGE_PAGER_TITLE);
                mPager.findViewById(R.id.pager_tab_strip).setVisibility(View.GONE);

                break;

            case LIVE_LIST_POSITION:
                sendMsgReportAnalyticsEvent(Constants.FRAGMENT_VIEW_EVENT,"Fragment~" + LIVE_PAGER_TITLE);
            if (previousPosition == REPLY_LIST_POSITION) {
                    //LiveFragReference.get().getReplyFragment().deleteLoader();
                }
                mPager.findViewById(R.id.pager_tab_strip).setVisibility(View.VISIBLE);
                break;

            case REPLY_LIST_POSITION:
                sendMsgReportAnalyticsEvent(Constants.FRAGMENT_VIEW_EVENT, "Fragment~" + REPLY_PAGER_TITLE);
                Integer currentThread = LiveFragReference.get().getCurrentThread();

                mPager.findViewById(R.id.pager_tab_strip).setVisibility(View.GONE);

                //  LiveFragReference.get().getReplyFragment().resetDisplay();
                break;
        }

        previousPosition = position;
    }

    /***********************************************************************************************
     * INITIALIZE
     *
     */
    /**
     * method checkForLocationEnabled
     * <p>
     * tests if the location services are enables
     *
     * @param context context of the location to be testing
     */
    public static void checkForLocationEnabled(final Context context) {

        LocationManager lm = null;
        boolean gps_enabled = false, network_enabled = false;
        if (lm == null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!gps_enabled && !network_enabled) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(context); //todo make this nicer
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(context.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    paramDialogInterface.dismiss();
                }
            });
            dialog.show();

        }
    }

    public boolean resolveDns(String address) {

        if (isBound) {

            Log.d(TAG, "sending message to resolve DNS...");
            Message msg = Message.obtain(null, DataHandlingService.MSG_RESOLVE_HOST);

            Bundle b = new Bundle();
            b.putString("hostname", address);
            msg.setData(b);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return true;
    }


    /**
     * method 'sendMsgRequestLocalPosts'
     *
     * requests images from the service
     *
     * @param num of images to request from server
     */
    public void sendMsgRequestLocalPosts(int num) {

        if (isBound) {
            Log.d(TAG, "sending message to request " + num + " images");

            Message msg = Message.obtain(null, DataHandlingService.MSG_REQUEST_LOCAL_POSTS,num, 0);
            Bundle data = new Bundle();
            data.putInt(Constants.IMAGE_COUNT, num);
            msg.setData(data);

            try {
                msg.replyTo = messageHandler;
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "error sending message to request images", e);
            }

        }
    }

    /**
     * method 'sendMsgRequestMessages'
     *
     * sends a message to the imageRequestService to request messages
     */
    public void sendMsgRequestLocalMessages() {
        if (isBound) {
            Log.d(TAG, "sending message to request Local messages");
            Message msg = Message.obtain(null, DataHandlingService.MSG_REQUEST_MESSAGES);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "error sending message to request images", e);
            }
        }
    }


    /** LIVE POST and REPLY HANDLING
     */
    Bundle liveData;
    private boolean cancelPost = false;


    Bundle replyData;
    private boolean cancelReply = false;

    /**
     * method 'sendMsgCreateThread'
     *
     * send a message to the service to pass the data on to the server
     *
     */
    public void sendMsgCreateThread(Bundle liveData) {
        if (VERBOSE) Log.v(TAG,"entering sendMsgCreateThread...");

        if (SAVE_LOCALLY) {
       /*     ContentValues values = new ContentValues();
            values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_ID,((int)(Math.random()*1000)));
            values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_TIME,0);
            values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_NAME,
                    liveData.getString("name"));
            values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_TITLE,
                    liveData.getString("title"));
            values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_FILEPATH,
                    liveData.getString("filePath"));
            values.put(SQLiteDbContract.LiveThreadEntry.COLUMN_NAME_DESCRIPTION,
                    liveData.getString("description"));
            getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LIVE,values);

I*/
            if (VERBOSE) Log.v(TAG,"exiting sendMsgCreateThread... IMAGE SAVED LOCALLY");
            return;
        }

        if (isBound) {
            Message msg = Message.obtain(null, DataHandlingService.MSG_CREATE_THREAD);
            msg.setData(liveData);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to create live thread",e);
            }
        }

        if (VERBOSE) Log.v(TAG,"exiting sendMsgCreateThread...");
    }


    private void sendMsgCreateReply(Bundle replyData) {
        if (VERBOSE) {
            Log.v(TAG,"entering sendMsgCreateReply");
            LogUtils.printBundle(replyData,TAG);
        }

        if (isBound) {
            Toast.makeText(this,"posting a reply to the server.",Toast.LENGTH_SHORT).show();
            Message msg = Message.obtain(null, DataHandlingService.MSG_REPLY_TO_THREAD);
            msg.setData(replyData);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to reply to thread",e);
            }
        }
        if (VERBOSE) Log.v(TAG,"exiting sendMsgCreateReply");
    }


    /**
     * method 'setLiveCreateThreadInfo'
     *
     * add the thread info to the iveData info bundle,
     * when the bundle posses all 4 attributes, send the message
     *
     * @param name the name the user used to create the thread with
     * @param title the title that was provided
     * @param description the description text
     * */
    public void setLiveCreateThreadInfo(String name, String title, String description) {
        if (liveData == null) {
            liveData = new Bundle(3);
        }
        liveData.putString("name",name);
        liveData.putString("title",title);
        liveData.putString("description",description);

        Log.d(TAG, "size is " + liveData.size());

        if (liveData.size() == 4) {
            sendMsgCreateThread(liveData);
            liveData = null;
        }
    }


    /**
     * method 'setLiveCreateInfo'
     *
     * overload, this version loads name from sharedPreferences and passes to main version
     *
     * @param title title of thread
     * @param description description for thread
     */
    public void setLiveCreateThreadInfo(String title, String description) {
        String name = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,MODE_PRIVATE).
                getString(StashLiveSettingsFragment.LIVE_NAME_KEY,"jester");
        setLiveCreateThreadInfo(name, title, description);
    }

    public void setLiveCreateReplyInfo(String comment, int threadID) {
        if (VERBOSE) Log.v(TAG,"enter setLiveCreateReplyInfo with "  + comment + ", " + threadID);

        String name = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,MODE_PRIVATE).
                getString(StashLiveSettingsFragment.LIVE_NAME_KEY,"jester");
        setLiveCreateReplyInfo(name, comment, threadID);


        if (VERBOSE) Log.v(TAG,"exiting setLiveCreateReplyInfo");
    }

    public void setLiveCreateReplyInfo(String name, String comment, int threadID) {
        if (VERBOSE) Log.v(TAG,"enter setLiveCreateReplyInfo with " + name + ", " + comment + ", " + threadID);

        if (replyData == null) {
            replyData = new Bundle(3);
        }

        replyData.putString("name",name);
        replyData.putString("description",comment);
        replyData.putInt("threadID", threadID);

        if (replyData.size() == 4) {
            sendMsgCreateReply(replyData);
            replyData = null;
        }

        if (VERBOSE) Log.v(TAG,"exiting setLiveCreateReplyInfo");
    }

    public void clearReplyInfo() {
        if (VERBOSE) Log.v(TAG,"reply picture mode was cancelled, clearing info.");
        replyData.clear();
    }


    /**
     * method 'setLiveFilePath'
     *
     * add the filepath to the liveData info bundle,
     * when the bundle posses all 4 attributes, send the message
     *
     * if the user has aborted the live post before this method is called, it will instead
     * use the file path to delete the image from internal storabge
     *
     * @param filePath path to where the image is
     */
    public void setLiveFilePath(String filePath) {
        if (cancelPost) { //true if the user aborted posting to live
            new File(filePath).delete();
            cancelPost = false;
            liveData = null;
            return;
        }

        if (liveData == null) {
            liveData = new Bundle(1);
        }
        liveData.putString(Constants.KEY_S3_KEY, filePath);
        if (liveData.size() == 4) {
            sendMsgCreateThread(liveData);
            liveData = null;
        }
    }

    public void setReplyFilePath(String filePath) {
        if (VERBOSE) Log.v(TAG,"entering setReplyFilepath" + filePath);

        if (cancelReply) {
            new File(filePath).delete();
            cancelPost = false;
            replyData = null;
            return;
        }

        if (replyData == null) {
            replyData = new Bundle(1);
        }
        replyData.putString(Constants.KEY_S3_KEY,filePath);
        if (replyData.size() == 4) {
            sendMsgCreateReply(replyData);
            replyData = null;
        }
        if (VERBOSE) Log.v(TAG,"exiting setReplyFilepath" + filePath);
    }

    public void setReplyComment(String comment) {
        replyData.putString("description",comment);
    }


    public String getReplyComment() {
        return replyData.getString("description","");
    }

    public void removePendingLiveImage() {
        //todo, cancel image saving process if it has not saved it
       if (liveData!=null) { //if the image has already been saved and set, delete it
           String path = liveData.getString(Constants.KEY_S3_KEY);
           if (path!=null) {
               new File(path).delete();
           }
           liveData = null;
       } else { //if not, make sure that when it is, its deleted.
           cancelPost = true;
       }
    }

    public void sendMsgRequestLiveThreads() {
        Log.i(TAG,"requesting a new live thread list...");
        if (isBound) {
            Message msg = Message.obtain(null, DataHandlingService.MSG_REQUEST_LIVE_THREADS);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to create request live threads",e);
            }
        }
    }

    public void sendMsgDownloadImage(String s3Directory, String s3Key) {
        Log.i(TAG,"send a message download image...");
        if (isBound) {
            Message msg = Message.obtain(null, DataHandlingService.MSG_DOWNLOAD_IMAGE);
            Bundle b = new Bundle(2);
            b.putString(Constants.KEY_S3_KEY,s3Key);
            b.putString(Constants.KEY_S3_DIRECTORY,s3Directory);
            msg.setData(b);

            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to create request live threads",e);
            }
        } else {
            Log.e(TAG,"service was not bound!");
        }
    }


    /***********************************************************************************************
     * CAMERA HANDLING
     *
     */
    /**
     * Create a file Uri for saving an image or video
     */

    /**
     * Create a File for saving an image or video
     */
    public File getInternalImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());

        File file = new File(this.getFilesDir(), File.separator +
                "IMG_" + timeStamp + ".jpg");

        return file;
    }



    /** CAMERA HANDLER COMMUNICATION - HANDLER THREAD
     */
    public static final int MSG_CONNECT_CAMERA = 0;
    public static final int MSG_DISCONNECT_CAMERA = 1;
    public static final int MSG_START_PREVIEW = 2;
    public static final int MSG_STOP_PREVIEW = 3;
    public static final int MSG_ACTIVITY_DESTROYED = 4;
    public static final int MSG_TAKE_PICTURE = 5;
    public static final int MSG_SAVE_PICTURE = 6;
    public static final int MSG_SWITCH_CAMERA = 7;
    public static final int MSG_FLASH_ON = 8;
    public static final int MSG_FLASH_OFF = 9;
    public static final int MSG_AUTOFOCUS = 10;

    /**
     * class 'CameraHanderThread'
     *
     * this handler thread, an implementation of looper, serves as the background thread
     * to receive messages for the camera.
     *
     * the camera connection, image capturing, etc, is managed in this thread and this thread only
     */
    /*class CameraHandlerThread extends HandlerThread {
        private CameraHandler cameraHandler;



        CameraHandlerThread(MainActivity parent) {
            super("CameraHandlerThread");
            start();
            cameraHandler = new CameraHandler(getLooper());
            cameraHandler.setParent(parent);
        }

        public CameraHandler getCameraHandler() {
            return cameraHandler;
        }

    }*/

    private static WeakReference<CameraHandler> cameraHandlerWeakReference = new WeakReference<>(null);

    public synchronized CameraHandler getCameraHandlerSingleton(MainActivity activity) {
        if (cameraHandlerWeakReference.get() == null) {
            if (VERBOSE) Log.v(TAG,"reference to camera handler did not exist... creating new cameraHandler...");

            HandlerThread handlerThread = new HandlerThread("CameraHandlerThread");

            //must start handlerthread prior to getLooper() else it will return null
            handlerThread.start();
            CameraHandler handler = new CameraHandler(handlerThread.getLooper());
            handler.setParent(activity);
            Log.i(TAG, "New CameraHandler created and set: " + handler.toString());

            cameraHandlerWeakReference = new WeakReference<>(handler);
        } else {
            if (VERBOSE)  Log.v(TAG,"reference to camera handler did already existed... grabbing existing...");
        }

        return cameraHandlerWeakReference.get();
    }

    private static SurfaceTexture mSurface;
    private static Camera mCamera; //static to prevent garbage collection during onStop

    /**
     * class 'CameraHandler'
     *
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     *
     * Draws to a TextureView, which is stored in {@link CameraFragment}
     *
     * It is possible that a SurfaceView would be a more effective implementation //todo look into this
     */
    class CameraHandler extends Handler implements TextureView.SurfaceTextureListener, Camera.PictureCallback, Camera.ShutterCallback {
        private static final String TAG = "MainCameraHandler";
        private boolean isConnected = false;


        private static final int CAMERA_POSITION_BACK = 0;
        private static final int CAMERA_POSITION_FRONT = 1;

        /** SHARED PREFERENCES
         */
        private static final String FRONT_CAMERA_PARAMETERS = "fParams";
        private static final String BACK_CAMERA_PARAMETERS = "bParams";

        private Camera.CameraInfo cameraInfo;

        private Camera.Parameters parameters;

        private byte[] theData;

        private boolean meteringAreaSupported = false;

        private int width;
        private int height;


        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<MainActivity> mWeakActivity;

        public void setParent(MainActivity parent) {
            mWeakActivity = new WeakReference<>(parent);

            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(parent);
            /*
            if (currentCamera == CAMERA_POSITION_FRONT) {
                if (p.getString(FRONT_CAMERA_PARAMETERS,null)!=null){
                    parameters.unflatten(p.getString(FRONT_CAMERA_PARAMETERS,null));
                }
            } else {
                if (p.getString(BACK_CAMERA_PARAMETERS,null)!=null){
                    parameters.unflatten(p.getString(BACK_CAMERA_PARAMETERS,null));
                }
            }*/
        }

        public CameraHandler(Looper looper) {
            super(looper);
        }

        /**
         * method 'onSurfaceTextureAvailable'
         *
         * called when the textureView in {@link CameraFragment} is ready to use
         *
         * @param surface the SurfaceTexture that is ready to be used
         * @param width the width of the surface texture
         * @param height the height of the surface texture
         */
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (VERBOSE) {
                Log.v(TAG,"enter onSurfaceTextureAvailable");
            }
            mSurface = surface;
            this.width = width;
            this.height = height;

//            Log.d(TAG, "thread is : " + getLooper().getThread().toString());

            if (mSurface != null) {
                Log.i(TAG,"surface was created and saved");
            }


            if (getMainLooper().getThread() == getLooper().getThread()) {
                Log.e(TAG,"this occurs in the main thread...");
            }

            if (isConnected) {

                if (mCamera != null) {
                    Log.d(TAG, "Camera is not connected and is available, setting and starting " +
                            "preview.");
                            try {
                                mCamera.setPreviewTexture(mSurface);
                                mCamera.startPreview();
                            } catch (IOException e) {
                                Log.e(TAG, "error setting preview texture to camera", e);
                            } catch (NullPointerException e) {
                                Log.e(TAG, "error setting Preview texture to camera", e);
                            }
                } else {
                    Log.d(TAG, "camera was not avaliable, saving surface...");
                    mSurface = surface;
                }
            } else {
                Log.v(TAG,"Camera was not connected, saving surface...");
                mSurface = surface;
            }


            if (VERBOSE) {
                Log.v(TAG,"exit onSurfaceTextureAvailable");
            }
        }



        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if (VERBOSE) {
                Log.v(TAG,"enter onSurfaceTextureSizeChanged");
            }
            mSurface = surface;
            Camera.Parameters p = mCamera.getParameters();
            if (p.getMaxNumMeteringAreas() > 0) {
                this.meteringAreaSupported = true;
            }
            this.width = width;
            this.height = height;

            if (VERBOSE) {
                Log.v(TAG,"exit onSurfaceTextureSizeChanged");
            }
        }


        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (VERBOSE) {
                Log.v(TAG, "enter onSurfaceTextureDestroyed");
            }


            if (mCamera != null) {
                if (isConnected) {
                    Log.d(TAG,"mCamera was not null, stopping preview");
                    mCamera.stopPreview();

                    Log.d(TAG,"abandoning bufferqueue");
                    mCamera.setPreviewCallbackWithBuffer(null);
                }

                if (mSurface != null) {
                    surface.release();
                    mSurface.release();
                    mSurface = null;
                }

            } else {
                Log.d(TAG, "mCamera was null, no action taken");
            }


            if (VERBOSE) {
                Log.v(TAG,"exit onSurfaceTextureDestroyed");
            }
            return true;
        }

        /**
         * method 'onSurfaceTextureUpdated'
         *
         * called every time the surface texture is redrawn
         *
         * @param surface the surface that was updated
         */
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            if (VERBOSE) {
                Log.v(TAG, "clearing weakreference to activity");
            }
            mWeakActivity.clear();
        }

        /**
         * method 'handleMessage'
         *
         * performs tasks based on incoming messages
         *
         * @param inputMessage the message that was sent
         */
        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            if (getLooper() == Looper.getMainLooper()) {
                Log.d(TAG,"this handler runs on the UI thread");
            }

            MainActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.w(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_CONNECT_CAMERA: //0
                    cameraInfo = new Camera.CameraInfo();
                    //Camera.getCameraInfo(currentCamera,cameraInfo);

                    Log.d(TAG,"connecting to camera, setting all attributes...");
                    mCamera = getCameraInstance(inputMessage.arg1);

                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera.setDisplayOrientation(cameraInfo.orientation-180);
                    } else {
                        mCamera.setDisplayOrientation(cameraInfo.orientation);
                    }

                    /*    SharedPreferences.Editor editor = PreferenceManager.
                                getDefaultSharedPreferences(mWeakActivity.get()).edit();
                        if (currentCamera == CAMERA_POSITION_FRONT) {
                            editor.putString(FRONT_CAMERA_PARAMETERS,parameters.flatten());
                        } else {
                            editor.putString(BACK_CAMERA_PARAMETERS,parameters.flatten());
                        }
                        Log.i(TAG,"storing calculated camera parameters to SharedPreferences");
                        editor.commit();
                    */
                    break;
                case MSG_DISCONNECT_CAMERA: //1
                    if (mCamera!=null) {
                        releaseCamera(mCamera);
                    }
                    mCamera = null;
                    break;

                case MSG_START_PREVIEW: //2
                    try {
                          Log.d(TAG,"setting and starting preview...");
                          //Log.e(TAG,"this is disabled...");
                          mCamera.startPreview();
                      } catch (Exception e) {
                         Log.e(TAG,"generic error setting and starting preview",e);
                      }
                    break;
                case MSG_STOP_PREVIEW: //3
                    if (mCamera != null) {
                        mCamera.stopPreview();
                    }
                    break;

                case MSG_TAKE_PICTURE: //5
                    mCamera.takePicture(this, null, this);
                    break;
                case MSG_SAVE_PICTURE: //6
                    saveImage(theData,
                            inputMessage.getData().getString(Constants.KEY_TEXT),
                            0,
                            inputMessage.arg2); // local vs live
                    theData = null;
                    break;
                case MSG_SWITCH_CAMERA: //7

                    Log.d(TAG,"switching to camera: " + inputMessage.arg1);

                    if (mCamera!=null) {
//                        Log.d(TAG,"abandoning bufferqueue");
                        mCamera.stopPreview();
                        mCamera.setPreviewCallbackWithBuffer(null);
                        releaseCamera(mCamera);
                    }
                    isConnected = false;
                    mCamera = getCameraInstance(inputMessage.arg1);

                    if (isConnected) {
                        Log.d(TAG,"camera isConnected");
                        try {
                            Log.d(TAG,"setting preview texture to mCamera...");
                            mCamera.setPreviewTexture(mSurface);
                        } catch (IOException e) {
                            Log.e(TAG,"error setting preview texture to camera",e);
                        }
                        mCamera.startPreview();
                    } else {
                        Log.e(TAG,"mCamera is not connected...");
                    }

                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera.setDisplayOrientation(cameraInfo.orientation-180);
                    } else {
                        mCamera.setDisplayOrientation(cameraInfo.orientation);
                    }

                    break;

                case MSG_FLASH_ON: //8

                        if (parameters.getSupportedFlashModes().
                                contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                            mCamera.setParameters(parameters);
                        }
                    break;


                case MSG_FLASH_OFF: //9

                        if (parameters.getSupportedFlashModes().
                                contains(Camera.Parameters.FLASH_MODE_OFF)) {
                           parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                           mCamera.setParameters(parameters);
                      }

                    break;

                case MSG_ACTIVITY_DESTROYED: //4
                    invalidateHandler();
                    break;

                case MSG_AUTOFOCUS:
                        focusOnTouch(inputMessage.getData().getFloat("x"),inputMessage.getData().getFloat("y"));
                    break;


                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }

        /**
         * convenience class to ease debugging
         */
        private void startPreview() {
            if (isConnected) {
                if (VERBOSE) {
                    Log.v(TAG, "starting camera preview");
                }
                mCamera.startPreview();
            } else {
                Log.d(TAG, "attempted to start preview but camera was not connected");
            }
        }


        /** A safe way to get an instance of the Camera object. */
        public Camera getCameraInstance(int whichCamera){
            if (VERBOSE) {
                Log.v(TAG,"connecting to camera " + whichCamera);
            }

            Camera.CameraInfo cInfo = new Camera.CameraInfo();
            cameraInfo = cInfo;

            Camera c = null;
            Camera.getCameraInfo(whichCamera,cInfo);
            try {
                c = Camera.open(whichCamera); // attempt to get a Camera instance
            }
            catch (Exception e){
                // Camera is not available (in use or does not exist)
                Log.e(TAG,"Error opening camera - dialog should show" +
                        "",e);
            }

            if (c != null) {
                isConnected = true;

                
            if (cInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                c.setDisplayOrientation(cInfo.orientation - 180);
            } else {
                c.setDisplayOrientation(cInfo.orientation);
            }

            int width = mWeakActivity.get().getResources().getDisplayMetrics().widthPixels;
            int height = mWeakActivity.get().getResources().getDisplayMetrics().heightPixels;

            parameters = c.getParameters();
            Camera.Size mPreviewSize = calculateOptimalPreviewSize(
                    parameters.getSupportedPreviewSizes(),width,height);
            Camera.Size mDisplaySize = calculateOptimalCameraResolution(
                    parameters.getSupportedPictureSizes());
            parameters.setPictureSize(mDisplaySize.width, mDisplaySize.height);
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            c.setParameters(parameters);

           if (mSurface != null) {
               try {
                   c.setPreviewTexture(mSurface);
                   c.startPreview();
               } catch (IOException e) {
                   Log.e(TAG, "error setting mSurface as surfacetexture", e);
               }
           } else {
               Log.e(TAG,"The camera surface texture has yet to be created");
           }


            } else {
                Log.i(TAG, "getCameraInstance failed to connect to camera");
            }

            return c; // returns null if camera is unavailable
        }

        /* convenience method to ease debugging */
        private void releaseCamera(Camera c) {
            if (VERBOSE) {
                Log.v(TAG,"disconnecting from camera " + c.toString());
            }
            isConnected = false;
            c.release();
        }

        @Override
        public void onShutter() {
            if (VERBOSE) Log.v(TAG,"entering onShutter");

            Intent intent = new Intent(CameraFragment.ACTION_PICTURE_TAKEN);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

            if (VERBOSE) Log.v(TAG,"exiting onShutter");
        }

        public void onPictureTaken(final byte[] data, android.hardware.Camera camera) {
            if (VERBOSE) {
                Log.v(TAG, "enter onPictureTaken... ");
            }
            camera.stopPreview();
            theData = data;
            if (VERBOSE) {
                Log.v(TAG, "exit onPictureTaken... ");
            }
        }

        /**
         * method 'saveImage'
         *
         * @param data image data to be saved
         * @param commentText text of the comment to be recreated
         * @param height positioning of the comment to be recreated
         */
        public void saveImage(byte[] data, String commentText, int height, int callBack) {
            int TEXT_BOX_HEIGHT = 55;
            int COMPRESSION_QUALITY = 100;

            Log.d(TAG, "saving image...");
            Log.d(TAG,"incoming string comment " + commentText);

            UUID name = UUID.nameUUIDFromBytes(data);
            String key = name.toString()+ ".jpg";

            File file = new File(mWeakActivity.get().getCacheDir(),key);

            String filePath = file.getAbsolutePath();


            Log.d(TAG, "Saving file to... " + filePath);
            Log.d(TAG, "The size of the image before compression: " + data.length);


            //create bitmap from data
            Bitmap image;
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inMutable = true;
            image = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            Matrix matrix = new Matrix();
            matrix.postRotate(cameraInfo.orientation);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(),
                    image.getHeight(), matrix, true);

            Log.d(TAG, "comment was null...");


            try {
                image.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY,
                        new FileOutputStream(filePath));
            } catch (FileNotFoundException e) {
                Log.e(TAG,"error compressing bitmap to filepath" + filePath, e);

            }
             Log.d(TAG, "The size of the image after: " + data.length);

            if (callBack == CameraFragment.CAMERA_REPLY_MODE) {
                Log.d(TAG,"now extracting and saving thumbnail");

                try {
                    image = ThumbnailUtils.extractThumbnail(image,250,250);
                    image.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY,
                            new FileOutputStream(filePath+"s"));
                } catch (FileNotFoundException e) {
                    Log.e(TAG,"error compressing bitmap to filepath" + filePath, e);

                }
            }



            switch (callBack) {
                case CameraFragment.CAMERA_MESSAGE_MODE:
                case CameraFragment.CAMERA_DEFAULT_MODE:
                    if (Constants.LOGD) Log.d(TAG, "notifying MainActivity to" +
                            " post image to Local...");
                    mWeakActivity.get().sendImageToLocal(key, 2, commentText);
                    break;

                case CameraFragment.CAMERA_LIVE_MODE:
                    if (Constants.LOGD) Log.d(TAG, "notifying MainActivity image " +
                            "is ready to post to Live...");
                    mWeakActivity.get().setLiveFilePath(key);
                    break;

                case CameraFragment.CAMERA_REPLY_MODE:
                    if (Constants.LOGD) Log.d(TAG, "notifying MainActivity" +
                            " image is ready to post to Reply...");
                    mWeakActivity.get().setReplyFilePath(key);
                    break;
            }


            if (VERBOSE) {
                Log.v(TAG, "exit savePicture...");
            }
        }


        /**
         * method 'focusOnTouch'
         *
         * from http://stackoverflow.com/questions/18460647/android-setfocusarea-and-auto-focus
         *
         * @param x the x coord of the touchevent
         * @param y the y coord of the touchevent
         */
        protected void focusOnTouch(float x, float y) {
            if (mCamera != null) {

                mCamera.cancelAutoFocus();
                Rect focusRect = calculateTapArea(x, y, 1f);
                Rect meteringRect = calculateTapArea(x, y, 1.5f);


                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                ArrayList<Camera.Area> cameraAreas = new ArrayList<>();
                cameraAreas.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusAreas(cameraAreas);

                if (meteringAreaSupported) {
                    ArrayList<Camera.Area> meterAreas = new ArrayList<>();
                    meterAreas.add(new Camera.Area(meteringRect, 1000));
                    parameters.setMeteringAreas(meterAreas);
                }

                Log.d(TAG,parameters.flatten());

                try {
                    mCamera.setParameters(parameters);
                    mCamera.autoFocus((CameraFragment)mAdapter.getItem(CAMERA_LIST_POSITION));
                } catch (RuntimeException e) {
                    Log.e(TAG,"Failed to set camera parameters...",e);
                }
            }
        }

        Matrix matrix = new Matrix();
        int focusAreaSize = 75;

        /**
         * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
         */
        private Rect calculateTapArea(float x, float y, float coefficient) {
            int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

            int left = clamp((int) x - areaSize / 2, 0, width - areaSize);
            int top = clamp((int) y - areaSize / 2, 0, height - areaSize);

            RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

            matrix.mapRect(rectF);



            return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
        }
        private int clamp(int x, int min, int max) {
            if (x > max) {
                return max;
            }
            if (x < min) {
                return min;
            }
            return x;
        }


        /**
         * method 'getOptionalPreviewSize'
         *
         * finds the best preview size to aspect ratio and returns the size
         *
         * @param sizes camera sizes available
         * @param w width to aim for
         * @param h height to aim for
         * @return the optinal Camera.size for the preview
         */
        private Camera.Size calculateOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
            if (VERBOSE) {
                Log.v(TAG, "enter calculateOptimalPreviewSize...");
            }

            final double ASPECT_TOLERANCE = 0.1;
            double targetRatio = (double) h / w;

            if (sizes == null) return null;

            Camera.Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;

            int targetHeight = h;

            for (Camera.Size size : sizes) {
                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }

            if (optimalSize == null) {
                minDiff = Double.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    if (Math.abs(size.height - targetHeight) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }
            if (VERBOSE) {
                Log.d(TAG,"optimal" +
                        "preview size found is " + optimalSize.height + ", " + optimalSize.width);
                Log.v(TAG,"exit calculateOptimalPreviewSize...");
            }
            return optimalSize;
        }

        /**
         * method 'calculateOptimalResolution'
         *
         * tests available resolutions of the provided camera, first checks if it supports
         * the optimal resolution. Should that resolution not be available, it then attempts to
         * calculate the resolution which is closest to the optimal.
         *
         * Uses the distance formula in {@link SQLiteDbHelper}
         *
         * @param sizes avaiable camera resolution sizes
         * @return best resolution available
         */
        private Camera.Size calculateOptimalCameraResolution(List<Camera.Size> sizes) {
            if (VERBOSE) {
                Log.v(TAG,"enter calculateOptimalResolution...");
            }

            if (VERBOSE) {
                Log.v(TAG, "viable image sizes: ");
                for(int i = 0; i < sizes.size(); i++) {
                    Log.v(" ", " " + sizes.get(i).width + sizes.get(i).height);
                }
            }


            //for each size, first, do they have it? - try each size
            for (Camera.Size s : sizes) {
                if (s.width == Constants.IDEAL_WIDTH) {
                    if (s.height == Constants.IDEAL_HEIGHT) {
                        //found it
                        Log.d(TAG,"perfect resolution found! " + s.width + ", " + s.height);
                        return s;
                    }
                }
            }

            //guess it failed, calculation time
            Log.d(TAG,"perfect resolution not found.");

            int closestDistance = 9000;
            Camera.Size closestSize = sizes.get(sizes.size()-1);

            for (Camera.Size s : sizes) {
                Double currentDistance = SQLiteDbHelper.distanceFormula(s.width,s.height,
                        Constants.IDEAL_WIDTH,Constants.IDEAL_HEIGHT);

                if (currentDistance < closestDistance) {
                    closestDistance = currentDistance.intValue();
                    closestSize = s;

                }
            }
            Log.d(TAG,"closestSize found is: " + closestSize.width + ", " + closestSize.height);

            if (VERBOSE) {
                Log.v(TAG, "exit calculateOptimalResolution...");
            }
            return closestSize;
        }



    }

    public void sendMsgStartPreview() {
        if (VERBOSE) {
            Log.v(TAG,"received callback from CameraFragment");
            Log.v(TAG,"entering sendMsgStartPreview");
        }
        try {
            cameraMessenger.send(Message.obtain(null, MSG_START_PREVIEW));
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to start Preview");
        }


        if (VERBOSE)
            Log.v(TAG,"exiting sendMsgStartPreview()");
    }

    public void sendMsgStopPreview() {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgStopPreview...");
        }
        try {
            cameraMessenger.send(Message.obtain(null, MSG_STOP_PREVIEW));
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to stop Preview");
        }

        if (VERBOSE) {
            Log.v(TAG, "exiting sendMsgStopPreview...");
        }
    }

    public void sendMsgTakePicture() {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgTakePicture...");
        }

        try {
            Message msg = Message.obtain(null,MSG_TAKE_PICTURE);
            cameraMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to take picture");
        }

        if (VERBOSE) {
            Log.v(TAG,"exiting sendMsgTakePicture...");
        }
    }

    /**
     * method 'sendMsgSaveImage'
     *
     * send message to the camera event thread to save the current image
     *
     * @param comment if there is a comment, it is passed here
     */
    public void sendMsgSaveImage(EditText comment, int where) {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgSaveImage...");
        }

        Message msg = Message.obtain(null, MSG_SAVE_PICTURE);

        //APPLYING SETTINGS
        msg.arg2 = where;

        if (!comment.getText().toString().trim().matches("")) {
            Log.d(TAG, "Text retrieved was not null, attempting to send");

            Bundle b = new Bundle();
            b.putString(Constants.KEY_TEXT,comment.getText().toString());
            msg.setData(b);
        } else {
            Log.d(TAG, "comment was null...");
        }

        //NOW SENDING
        try {
            cameraMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to save image");
        }

        if (VERBOSE) {
            Log.v(TAG, "exiting sendMsgSaveImage...");
        }
    }

    /**
     * method 'sendMsgSaveImage'
     *
     * send message to the camera event thread to save the current image
     *
     * @param comment if there is a comment, it is passed here
     */
    public void sendMsgSaveImage(EditText comment, int where, String messageTarget) {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgSaveImageWrapper...");
            Log.v(TAG, "printing arguments: ");
            Log.v(TAG, "comment " + comment.getText().toString());
            Log.v(TAG, "where " + where);
            Log.v(TAG, "messageTarget " + messageTarget);
        }


        this.messageTarget = messageTarget;
        sendMsgSaveImage(comment,where);
        if (VERBOSE) Log.v(TAG, "exiting sendMsgSaveImageWrapper...");
    }

    public void sendMsgSwitchCamera() {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgSaveImage...");
        }
        if (currentCamera == CAMERA_POSITION_FRONT) {
            currentCamera = CAMERA_POSITION_BACK;
        } else {
            currentCamera = CAMERA_POSITION_FRONT;
        }

        try {
            cameraMessenger.send(Message.obtain(null, MSG_SWITCH_CAMERA, currentCamera, 0));
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to save image");
        }

        if (VERBOSE) {
            Log.v(TAG, "exiting sendMsgSaveImage...");
        }
    }

    public void setFlash(View view) {
        if (VERBOSE) {
            Log.v(TAG, "entering setFlash...");
        }

        CheckBox flashBox = (CheckBox)view;

        if (flashBox.isChecked()) {
            try {
                cameraMessenger.send(Message.obtain(null, MSG_FLASH_ON));
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to save image");
            }
        } else {
            try {
                cameraMessenger.send(Message.obtain(null, MSG_FLASH_OFF));
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to save image");
            }
        }

        if (VERBOSE) {
            Log.v(TAG, "exiting setFlash...");
        }
    }

    public void sendMsgAutoFocus(MotionEvent event) {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgAutoFocus...");
        }

        try {
            Bundle b = new Bundle(2);
            b.putFloat("x",event.getX());
            b.putFloat("y",event.getY());

            Message msg = Message.obtain(null,MSG_AUTOFOCUS);
            msg.setData(b);
            cameraMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG,"error sending message to take picture");
        }

        if (VERBOSE) {
            Log.v(TAG,"exiting sendMsgAutoFocus...");
        }
    }


    /**
     * tags for replyHandler's various tasks
     */
    static final int MSG_IMAGE_STORED = 0;

    static final int MSG_IMAGE_SENT= 1;

    static final int MSG_DATABASE_CLEARED= 2;

    static final int MSG_PICTURE_TAKEN = 3;

    static final int MSG_SUCCESS_LOCAL = 15;

    static final int MSG_SUCCESS_LIVE = 16;

    static final int MSG_SUCCESS = 50;

    static final int MSG_TOO_MANY_REQUESTS = 51;

    static final int MSG_LIVE_REFRESH_DONE = 52;

    static final int MSG_UNAUTHORIZED = 53;

    static final int MSG_NOT_FOUND = 54;

    static final int MSG_NOTHING_RETURNED = 55;


    /**
     * class 'replyHandler'
     *
     * handles responses from the service
     */
    class UIHandler extends Handler {

        WeakReference<MainActivity> activity;

        public Handler setParent(MainActivity parent) {
            activity = new WeakReference<>(parent);
            return this;
        }

        public void handleMessage(Message msg) {
            Log.d(TAG,"enter handleMessage");
            int respCode = msg.what;

            int result;
            int distance;

            switch (respCode) {
                case MSG_IMAGE_STORED:
                    result = msg.arg1;
                    Toast.makeText(activity.get(),"reply from service received " + result,Toast.LENGTH_LONG).show();

                    //File[] toLoad = new File[1];
                    //toLoad[0] = new File("IMG_"+msg.arg1+".jpg");
                    if (mPager.getCurrentItem() <= CAMERA_LIST_POSITION) {//ensure localFrament exists.
                    }
                    break;

                case MSG_IMAGE_SENT:
                    result = msg.arg1;
                    Toast.makeText(activity.get(),"Image size to be sent is: " + result/1024 + "kB",Toast.LENGTH_LONG).show();
                    break;

                case MSG_DATABASE_CLEARED:
                    Toast.makeText(activity.get(),"entire database cleared",Toast.LENGTH_LONG).show();
                    break;

                case MSG_PICTURE_TAKEN: //-1 reply in main UI
                    CameraFragReference.get().onPictureTaken(1);
                    break;

                case MSG_SUCCESS_LOCAL:
                    Toast.makeText(activity.get(), "Successfully posted to local!", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SUCCESS_LIVE:
                    Toast.makeText(activity.get(),"Live post not implemented...",Toast.LENGTH_SHORT).show();
                    break;

                case MSG_SUCCESS:

                    switch (msg.arg1) {
                        case DataHandlingService.MSG_REPLY_TO_THREAD:
                            Toast.makeText(activity.get(),"reply successful",Toast.LENGTH_SHORT).show();
                            sendMsgRequestReplies(msg.getData().getInt("threadID"));
                            break;

                        case -1:
                            if (Constants.LOGV) {
                                Log.v(TAG,"generic operation successful, printing data bundle");
                                com.jokrapp.android.util.LogUtils.printBundle(msg.getData(),TAG);
                            }
                            break;


                    }
                    break;

                case MSG_TOO_MANY_REQUESTS:
                    new AlertDialog.Builder(activity.get())
                            .setTitle("Alert")
                            .setMessage("You have posted too many times, " +
                                    "in a small period, and now we're worried you're not human.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    break;

                case MSG_LIVE_REFRESH_DONE:
                  //  Toast.makeText(activity.get(),"Live Refresh Finished...",Toast.LENGTH_SHORT).show();
                   /* LiveFragment f = LiveFragReference.get();
                    if (f != null) {
                        if (VERBOSE) Log.v(TAG,"recreating live loader at id" + LiveFragment.LIVE_LOADER_ID);
                        getLoaderManager().initLoader(LiveFragment.LIVE_LOADER_ID, null,f);
                        f = null;
                    } else {
                        Log.d(TAG,"Live is currently not instantiated... doing nothing...");
                    }*/
                    break;

                case MSG_UNAUTHORIZED:
                    new AlertDialog.Builder(activity.get())
                            .setTitle("Unauthorized")
                            .setMessage("A bad userID was supplied to our server... hold on a moment while we make you another one.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Got it, coach", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;

                case MSG_NOT_FOUND:
                    new AlertDialog.Builder(activity.get())
                        .setTitle("404 - not found")
                        .setMessage("You have attempted to get the replies for a thread that no longer exists. You should refresh.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.get().sendMsgRequestReplies(
                                            ((ReplyFragment)mAdapter.getItem(REPLY_LIST_POSITION))
                                                    .getCurrentThread());
                                }
                            })
                            .setPositiveButton("Don't tell me what to do.",
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                        .show();
                    break;

                case MSG_NOTHING_RETURNED:

                    switch (msg.arg1) {
                        case DataHandlingService.MSG_REQUEST_MESSAGES:
                            Toast.makeText(activity.get(),"No messages received...",Toast.LENGTH_SHORT).show();
                            break;

                        case DataHandlingService.MSG_REQUEST_LIVE_THREADS:
                            Toast.makeText(activity.get(),"No live threads returned...",Toast.LENGTH_SHORT).show();
                            break;

                        case DataHandlingService.MSG_REQUEST_LOCAL_POSTS:
                            Toast.makeText(activity.get(),"No local posts returned...",Toast.LENGTH_SHORT).show();
                            break;

                        case DataHandlingService.MSG_REQUEST_REPLIES:
                            Toast.makeText(activity.get(),"No replies received...",Toast.LENGTH_SHORT).show();
                            break;
                    }

                    break;
            }

            Log.d(TAG, "exit handleMessage");
        }
    }

    /**
     *
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.d(TAG,"binding to service...");
            mService = new Messenger(service);
            isBound = true;

            //resolve DNS
            resolveDns("jokrbackend.ddns.net");
            Message msg = Message.obtain(null, DataHandlingService.MSG_SET_CALLBACK_MESSENGER);
            msg.replyTo = messageHandler;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to set messageHandler. This is a fatal error.",e);
                System.exit(1);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d(TAG,"unbinding from service...");

            mService = null;
            isBound = false;
        }
    };

    /**
     * callback from {@link com.jokrapp.android.CameraFragment.OnCameraFragmentInteractionListener}
     */
    @Override
    public void toggleScrolling() {
        if (mPager.isPagingEnabled()) {
            mPager.setPagingEnabled(false);
        } else {
            mPager.setPagingEnabled(true);
        }
    }

    public void enableScrolling() { mPager.setPagingEnabled(true); }
    public void disableScrolling() {
        mPager.setPagingEnabled(false);
    }

/***************************************************************************************************
 * ANALYTICS
 **/

    /**
     * method 'reportAnalyticsEvent'
     *
     * reports an event to Google analytics.
     *
     * {@link com.jokrapp.android.LiveFragment.onLiveFragmentInteractionListener}
     * {@link com.jokrapp.android.LocalFragment.onLocalFragmentInteractionListener}
     * Called by these linked interface callbacks.
     *
     * @param event which kind of even to report
     * @param name additional info to be reported (this will maybe be a Bundle)
     */
    public void sendMsgReportAnalyticsEvent(int event,String name) {
        if (VERBOSE) Log.d(TAG,"sending message to report analytics event");
        if (isBound) {
            try {
                Message msg = Message.obtain(null,event);
                Bundle b = new Bundle();
                b.putString("name",name);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "error send message to report analytics",e);
            }
        } else {
            Log.e(TAG,"failed to report analyitcs event, service was not bound...");
        }
    }



    public void onDeveloperInteraction(int request, Uri resource) {
        Log.i(TAG, "entering onDeveloperInteraction with request- " + request + " and resource - " + resource.toString());
    }


}