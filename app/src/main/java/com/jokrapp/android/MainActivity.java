package com.jokrapp.android;

import android.app.ActionBar;
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
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.LocationManager;
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
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
LocalFragment.onLocalFragmentInteractionListener, View.OnLongClickListener, ViewPager.OnPageChangeListener{
    private static String TAG = "MainActivity";
    private static final boolean VERBOSE = false;
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


    /** SERVICE MANAGEMENT
     */
    private boolean isBound = false;

    private Messenger mService;
    final Messenger messageHandler = new Messenger(new replyHandler().setParent(this));


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

        checkForLocationEnabled(this);

        //loads blank fragments
        createUI();

        //grabs stored images and loads
        findImages();

        final ActionBar actionBar = getActionBar();

        //request images

        Log.d(TAG,"exit onCreate...");
    }

    public Messenger getMessenger() {
        return mService;
    }


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

        //saves the cameraFragment

        /*
        if (isCamera) {
            if (camera != null) {
                Log.d(TAG, "camera was not null");
                //getFragmentManager().putFragment(outState, "savedFragment", camera);
            } else {
                Log.e(TAG, "camera was null");
            }
        }*/


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
    /**
     * class 'createUI'
     * <p>
     * instantiates the UI pager elements
     * its just a wrapper from the onCreate thread for niceness
     */
    private void createUI() {
        setContentView(R.layout.activity_main);
        findViewById(R.id.rootlayout).setOnLongClickListener(this);
        mAdapter = new MainAdapter(getFragmentManager());
        mPager = (CustomViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        mPager.setOnLongClickListener(this);
        mPager.setOnPageChangeListener(this);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name,context,attrs);
    }

    public boolean onLongClick(View v) {
        if (VERBOSE) Log.v(TAG,"onLongClick registered..." + v.toString());
        Toast.makeText(this,"Longclick",Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    public void onLocalReplyPressed(View view) {
        if (VERBOSE) {
            Toast.makeText(this,"Message pressed: " + view.getTag(),Toast.LENGTH_LONG).show();
        }

        CameraFragment.setMessageTarget((String) view.getTag());
        /*
        if (CameraFragReference.get() != null) {
            CameraFragReference.get().startMessageMode(UUID.fromString((String)view.getTag()));
        } else {
            CameraFragment.setMessageTarget(UUID.fromString((String) view.getTag()));
        }*/
        mPager.setCurrentItem(CAMERA_LIST_POSITION);
        mPager.setPagingEnabled(false);

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

    public void sendMsgRequestReplies(int a) {
        Toast.makeText(this, "refreshing replies", Toast.LENGTH_SHORT).show();
        if (isBound) {
            try {
                mService.send(Message.obtain(null,DataHandlingService.MSG_REQUEST_REPLIES, a, 0));
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to background service...",e);
            }
        }
    }

    /* class UIReceiver extends BroadcastReceiver {
        static final String RMSG_BUTTON_PRESSED = "com.jokrapp.android.MSG";
        static final String RBLOCK_BUTTON_PRESSED = "com.jokrapp.android.BLOCK";
        static final String INT_DATA = "intdata";
        private final String TAG = "UIReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            if (VERBOSE) {
                Log.v(TAG,"Intent received: " + intent.toString());
            }

            switch (intent.getAction()) {
                case RMSG_BUTTON_PRESSED:
                    onLocalMessage(intent.getIntExtra(INT_DATA,-1));
                    break;

                case RBLOCK_BUTTON_PRESSED:
                    onLocalBlock(intent.getIntExtra(INT_DATA,-1));
                    break;
            }
        }
    }*/


    /**
     * method 'onImageSaved'
     *
     * {@link com.jokrapp.android.CameraFragment.OnCameraFragmentInteractionListener}
     *
     * notifys the mainActivity that an image was just saved, so that the activity
     * can message the service to send this image to the server
     *
     * @param filePath the file path of the image saved
     * @param currentCamera the camera that the image was taken with
     */
    public void sendImageToLocal(String filePath, int currentCamera) {
        Log.d(TAG, "sending message to send Image");
        if (isBound) {
            Log.d(TAG, "sending message containing filepath to load...");
            Message msg = Message.obtain(null, DataHandlingService.MSG_SEND_IMAGE,currentCamera,0);

            String target = CameraFragReference.get().getMessageTarget();

            Bundle b = new Bundle();
            b.putString(Constants.IMAGE_FILEPATH, filePath);
            if (target != null) {
                Log.d(TAG,"Sending message to user : " + CameraFragReference.get()
                            .getMessageTarget()) ;
                b.putString(Constants.MESSAGE_TARGET, CameraFragReference.get()
                        .getMessageTarget().toString());
            } else {
                Log.d(TAG,"Sending local broadcast...");
            }
            msg.setData(b);

            try {
                msg.replyTo = messageHandler;
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
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
                }
                return MessageFragReference.get();
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
                    CameraFragReference = new WeakReference<>(CameraFragment.newInstance());
                }
                return CameraFragReference.get();
            } else if (position == REPLY_LIST_POSITION) {
                ReplyFragment f  = ReplyFragment.newInstance(LiveFragReference.get().getCurrentThread());
                LiveFragReference.get().setReplyFragment(f);
                return f;
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
                        return "Message";
                    case LOCAL_LIST_POSITION:
                        return "Local";
                    case CAMERA_LIST_POSITION:
                        return "Camera";
                    case LIVE_LIST_POSITION:
                        return "Live";
                    case REPLY_LIST_POSITION:
                        return "Reply";
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

    boolean wasOnReply = false;

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case LIVE_LIST_POSITION:
                if (wasOnReply) {
                    LiveFragReference.get().getReplyFragment().deleteLoader();
                }
                wasOnReply = false;
                break;
            case REPLY_LIST_POSITION:
                wasOnReply = true;
                LiveFragReference.get().getReplyFragment().resetDisplay();
                break;
        }
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
     * method 'requestImages'
     *
     * requests images from the service
     *
     * @param num of images to request from server
     */
    public void requestImages(int num) {

        if (isBound) {
            Log.d(TAG, "sending message to request " + num + " images");

            Message msg = Message.obtain(null, DataHandlingService.MSG_REQUEST_IMAGES,num, 0);
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
        if (VERBOSE) Log.v(TAG,"entering sendMsgCreateReply");
        if (isBound) {
            Toast.makeText(this,"posting a reply to the server.",Toast.LENGTH_SHORT).show();
            Message msg = Message.obtain(null, DataHandlingService.MSG_REPLY_TO_THREAD);
            msg.arg1 = replyData.getInt("threadID");
            replyData.remove("threadID");
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



    public void setLiveCreateReplyInfo(String name, String Comment, int threadID) {
        if (replyData == null) {
            replyData = new Bundle(3);
        }

        replyData.putString("name",name);
        replyData.putString("description",Comment);
        replyData.putInt("threadID",threadID);

        sendMsgCreateReply(replyData);
        replyData = null;
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
        liveData.putString("filePath",filePath);
        if (liveData.size() == 4) {
            sendMsgCreateThread(liveData);
            liveData = null;
        }
    }

    public void setReplyFilePath(String filePath) {
        if (cancelReply) {
            new File(filePath).delete();
            cancelPost = false;
            replyData = null;
            return;
        }

        if (replyData == null) {
            replyData = new Bundle(1);
        }
        replyData.putString("filePath",filePath);
        if (replyData.size() == 4) {
            sendMsgCreateThread(replyData);
            replyData = null;
        }
    }

    public void removePendingLiveImage() {
        //todo, cancel image saving process if it has not saved it
       if (liveData!=null) { //if the image has already been saved and set, delete it
           String path = liveData.getString("filePath");
           if (path!=null) {
               new File(path).delete();
           }
           liveData = null;
       } else { //if not, make sure that when it is, its deleted.
           cancelPost = true;
       }
    }

    public void sendMsgRequestLiveThreads() {
        if (VERBOSE) Log.v(TAG,"requesting live threads...");
        if (isBound) {
            Message msg = Message.obtain(null, DataHandlingService.MSG_REQUEST_THREAD_LIST);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG,"error sending message to create request live threads",e);
            }
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

    public CameraHandler getCameraHandlerSingleton(MainActivity activity) {
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

    /**
     * static class 'CameraHandler'
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


        private Camera mCamera; //static to prevent garbage collection during onStop
        private Camera.CameraInfo cameraInfo;

        private Camera.Parameters parameters;

        private byte[] theData;

        private SurfaceTexture mSurface;



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

//            Log.d(TAG, "thread is : " + getLooper().getThread().toString());

            if (mSurface != null) {
                Log.i(TAG,"surface was created and saved");
            }

            if (isConnected) {
                if (mCamera != null) {
                    Log.d(TAG, "Camera is not connected and is available, setting and starting " +
                            "preview.");
                    try {
                        mCamera.setPreviewCallbackWithBuffer(null);
                        mCamera.setPreviewTexture(surface);
                        mCamera.startPreview();
                    } catch (IOException e) {
                        Log.e(TAG, "error setting preview texture to camera", e);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "error setting Preview texture to camera", e);
                    }
                } else {
                    Log.d(TAG, "camera was not avaliable, saving surface...");
                    //mSurface = surface;
                }
            } else {
                Log.v(TAG,"Camera was not connected, saving surface...");
                //mSurface = surface;
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

                mSurface.release();
                mSurface = null;

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
                            inputMessage.getData().getString("commenttext"),
                            inputMessage.arg1, //height of commentText
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

/*                    Camera.getCameraInfo(inputMessage.arg1,cameraInfo);

                    Log.d(TAG,"setting camera DisplayOrientation to: " + cameraInfo.orientation);


                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera.setDisplayOrientation(cameraInfo.orientation-180);
                    } else {
                        mCamera.setDisplayOrientation(cameraInfo.orientation);
                    }

                    int sWidth = activity.getResources().getDisplayMetrics().widthPixels;
                    int sHeight = activity.getResources().getDisplayMetrics().heightPixels;

                        parameters = mCamera.getParameters();
                        Camera.Size mFpreviewSize = calculateOptimalPreviewSize(
                                parameters.getSupportedPreviewSizes(),sWidth,sHeight);
                        Camera.Size mFdisplaySize = calculateOptimalCameraResolution(
                                parameters.getSupportedPictureSizes());
                        parameters.setPictureSize(mFdisplaySize.width, mFdisplaySize.height);
                        parameters.setPreviewSize(mFpreviewSize.width, mFpreviewSize.height);

                    mCamera.setParameters(parameters);

                    try {
                        if (mSurface != null) {
                            mCamera.setPreviewTexture(mSurface);
                            mCamera.startPreview();
                        }
                    } catch (IOException e) {
                        Log.e(TAG,"error setting preview display",e);
                    }*/

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
                    Log.v(TAG,"starting camera preview");
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

            theData = data;
//                        try {
            //                          mWeakActivity.get().messageHandler.
            //                                send(Message.obtain(null, MSG_PICTURE_TAKEN));
            //                  } catch (RemoteException e) {
            //                    Log.e(TAG,"error notifying that picture was taken...",e);
            //              }
            if (VERBOSE) {
                Log.v(TAG, "exit onPictureTaken... ");
            }


        }


        /**
         * Interface to receive picture callback from the capturebutton
         *
         * sets a new click listener for the confirm button, which saves the file
         */
/*        private final android.hardware.Camera.PictureCallback mPicture =
                new android.hardware.Camera.PictureCallback() {

                    /**
                     * method 'onPictureTaken'
                     *
                     * the method that is called when a picture is taken
                     *
                     * @param data the byte array data of the image that was taken
                     * @param camera the camera from which the picture was taken
                     */
          /*          @Override
                    public void onPictureTaken(final byte[] data, android.hardware.Camera camera) {
                        if (VERBOSE) {
                            Log.v(TAG, "enter onPictureTaken... ");
                        }
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            Log.i(TAG,"callback is in the main thread");
                        }

                        theData = data;

                        Intent intent = new Intent(CameraFragment.ACTION_PICTURE_TAKEN);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

//                        try {
  //                          mWeakActivity.get().messageHandler.
    //                                send(Message.obtain(null, MSG_PICTURE_TAKEN));
      //                  } catch (RemoteException e) {
        //                    Log.e(TAG,"error notifying that picture was taken...",e);
          //              }
                        if (VERBOSE) {
                            Log.v(TAG, "exit onPictureTaken... ");
                        }
                    }


                };*/

        /**
         * method 'saveImage'
         *
         * @param data image data to be saved
         * @param commentText text of the comment to be recreated
         * @param height positioning of the comment to be recreated
         */
        public void saveImage(byte[] data, String commentText, int height, int callBack) {
            int TEXT_BOX_HEIGHT = 55;
            int COMPRESSION_QUALITY = 80;

            Log.d(TAG, "saving image...");

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // File file = new File(mWeakActivity.get().getFilesDir(),
            // "IMG_" + timeStamp + ".jpg");

            File file = new File(mWeakActivity.get().getFilesDir(),
                    "IMG_" + timeStamp + ".jpg");

            String filePath = file.getAbsolutePath();


            // String filePath = pictureFile.getAbsolutePath();


/*            if (file == null) {
                Log.e(TAG,"no file was able to be opened");
                return;
            }*/


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



            if (commentText == null) {
                Log.d(TAG, "comment was null...");

                try {
                    image.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY,
                            new FileOutputStream(filePath));
                } catch (FileNotFoundException e) {
                    Log.e(TAG,"error compressing bitmap to filepath" + filePath, e);
                }

                Log.d(TAG, "The size of the image after: " + data.length);

            } else {
                Log.d(TAG, "comment text is:"+commentText);

                Canvas canvas = new Canvas(image);


                TextView tv = new TextView(mWeakActivity.get());
                tv.setText(commentText);
                tv.layout(0, 0, canvas.getWidth(), TEXT_BOX_HEIGHT);
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundColor(mWeakActivity.get().
                        getResources().
                        getColor(R.color.pallete_accent_blue_alpha));
                tv.setGravity(Gravity.CENTER);
                tv.setDrawingCacheEnabled(true);
                tv.buildDrawingCache();
                canvas.drawBitmap(tv.getDrawingCache(), 0, height, null);

                try {
                    image.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY,
                            new FileOutputStream(filePath));
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                }
                Log.d(TAG, "The size of the image after: " + data.length);

            }

            /*
            ContentValues values = new ContentValues();
            values.put(SQLiteDbContract.LocalEntry.COLUMN_ID, 1337);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_TIME, 15091);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_LATITUDE, 88.88);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_LONGITUDE, 88.88);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_FILEPATH, "IMG_1337.jpg");

            mWeakActivity.get().getContentResolver().insert(FireFlyContentProvider.CONTENT_URI,
                    values);*/


            //mWeakActivity.get().onImageSaved(filePath, currentCamera);

            final int LOCAL_CALLBACK = 0;
            final int LIVE_CALLBACK = 1;
            final int REPLY_CALLBACK = 2;

            switch (callBack) {
                case LOCAL_CALLBACK:
                    Log.d(TAG, "notifying MainActivity to post image to Local...");
                    mWeakActivity.get().sendImageToLocal(filePath, 2);
                /*
                if (SAVE_LOCALLY) {
            ContentValues values = new ContentValues();
            values.put(SQLiteDbContract.LocalEntry.COLUMN_ID, 1337);
            values.put(SQLiteDbContract.LiveEntry.COLUMN_NAME_NAME,"LOCAL");
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_TIME, 15091);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_LATITUDE, 88.88);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_LONGITUDE, 88.88);
            values.put(SQLiteDbContract.LocalEntry.COLUMN_NAME_FILEPATH, "IMG_1337.jpg");
            mWeakActivity.get().getContentResolver().insert(FireFlyContentProvider.CONTENT_URI_LOCAL,
                      values);
                }*/
                    break;

                case LIVE_CALLBACK:
                    Log.d(TAG, "notifying MainActivity image is ready to post to Live...");
                    mWeakActivity.get().setLiveFilePath(filePath);
                    break;

                case REPLY_CALLBACK:
                    Log.d(TAG,"notifying MainActivity image is ready to post to Reply...");
                    mWeakActivity.get().setReplyFilePath(filePath);
                    break;
            }


            if (VERBOSE) {
                Log.v(TAG, "exit savePicture...");
            }
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
         * method 'getOptionalPreviewSize'
         *
         * grabs the best preview size from shared preferences, or calculates it through
         * calculateOptimalPreviewSize
         *
         * @param camera the camera with which to measure
         * @param w width to aim for
         * @param h height to aim for
         * @return the optinal Camera.size for the preview
         *//*
        private Camera.Size getOptimalPreviewSize(Camera camera, int w, int h) {
            if (VERBOSE) {
                Log.v(TAG,"enter getOptimalPreviewSize...");
            }
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(mWeakActivity.get().getApplicationContext());


            Camera.Size optimalSize;

            int width;
            int height;

            if (currentCamera  == CAMERA_POSITION_FRONT) {
                width = p.getInt(PREVIEW_WIDTH_FRONT, -1);
                height = p.getInt(PREVIEW_HEIGHT_FRONT, -1);


                if (width == -1) {//preview size has yet to be measured
                    Log.d(TAG,"Measuring front camera preview dimensions");

                    optimalSize = calculateOptimalPreviewSize(camera.getParameters().getSupportedPreviewSizes(),w,h);

                    p.edit().putInt(PREVIEW_WIDTH_FRONT, optimalSize.width).apply();
                    p.edit().putInt(PREVIEW_HEIGHT_FRONT, optimalSize.height).apply();
                } else {
                    optimalSize = camera.new Size(width,height);
                }

            } else {

                width = p.getInt(PREVIEW_WIDTH_BACK, -1);
                height = p.getInt(PREVIEW_HEIGHT_BACK, -1);

                if (width == -1) {//preview size has yet to be measured
                    Log.d(TAG,"Measuring back camera preview dimensions");

                    optimalSize = calculateOptimalPreviewSize(camera.getParameters().getSupportedPreviewSizes(),w,h);

                    p.edit().putInt(PREVIEW_WIDTH_BACK, optimalSize.width).apply();
                    p.edit().putInt(PREVIEW_HEIGHT_BACK, optimalSize.height).apply();

                } else {
                    optimalSize = camera.new Size(width,height);
                }
            }

            if (VERBOSE) {
                Log.v(TAG,"exit getOptimalPreviewSize...");
            }

            return optimalSize;
        }

        private Camera.Size getOptimalCameraResolution(Camera camera) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(mWeakActivity.get().getApplicationContext());
            int optimalWidth= p.getInt(RESOLUTION_WIDTH, -1);
            int optimalHeight= p.getInt(RESOLUTION_HEIGHT, -1);
            if (optimalWidth != -1 || optimalHeight != -1) {
                return camera.new Size(optimalWidth, optimalHeight);
            } else {
                Camera.Size size =calculateOptimalCameraResolution(camera);
                p.edit().putInt(RESOLUTION_WIDTH,size.width).apply();
                p.edit().putInt(RESOLUTION_HEIGHT,size.height).apply();
                return size;
            }
        }*/


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
            Log.v(TAG,"entering sendMsgTakePicture...");
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
    public void sendMsgSaveImage(EditText comment, boolean postToLive) {
        if (VERBOSE) {
            Log.v(TAG, "entering sendMsgSaveImage...");
        }

        Message msg = Message.obtain(null, MSG_SAVE_PICTURE);

        //APPLYING SETTINGS
        if (postToLive) {
            msg.arg2 = 1;
        }
        if (!comment.getText().toString().trim().matches("")) {
            Log.d(TAG,"Text retrieved was not null, attempting to send");

            Bundle b = new Bundle();
            b.putString("commenttext",comment.getText().toString());
            msg.setData(b);
            msg.arg1 = comment.getTop();
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

    /***********************************************************************************************
     * IMAGE MANAGEMENT
     *
     *
     *  loading from gallery for now ~~
     */
    /**
     * method 'loadImages'
     *
     * context-> onCreate()
     *
     * places all of the image filenames into a Map for access at any time
     */
    public void findImages(){
        //mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        //        Environment.DIRECTORY_PICTURES), "Boomerang");
        //Log.d(TAG,"listing files from internal storage...");
        //File[] files = getFilesDir().listFiles();

        //for(int i = 0; i < files.length; i++) {
        //    theImages.put(i,files[i].getAbsolutePath());
       // }

        //if (theImages.size() == 0) {
            //DataHandlingService.startActionRequestImages(this, 10, null);
       // } else {
        //    TextView textView = (TextView) findViewById(R.id.statusTextView);
        //    textView.setVisibility(View.INVISIBLE);
       // }


    }

    /**
     * static method 'decodeSampledBitmapFromFile'
     *
     * @param filePath the filePath of the image to be converted
     * @return bitmap that has successfully decoded and sized
     *
     *
     * decodes the bitmap given the filePath. This method is executed during an
     * asynchronous method, in order to properly run in the background
     * ideally, the image should already be scaled and sized properly by the server
     *
     */
    /*public Bitmap decodeSampledBitmapFromFile(String filePath) {
        Log.d(TAG,"entering decodeSampledBitmapFromFile...");


        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        //only decode boundaries, then check
        //options.inJustDecodeBounds = true;
        //BitmapFactory.decodeFile(filePath, options);
        //options.inSampleSize = resizeImage(options, reqWidth, reqHeight);

        //now decode for real
        ///options.inJustDecodeBounds = false;
        /**
         * A switching mechanism, assumes we load the first bitmap first, SWITCH
         */
        /*
       if (currentBitmapToLoad) {
            options.inBitmap = localCachedFirst.get(); //send the first bitmap out
            localCachedFirst = new WeakReference<>(localCachedSecond.get()); //the second now is the first
            currentBitmapToLoad = false;
        } else {
            options.inBitmap = localCachedSecond.get();  //the send the second out
            currentBitmapToLoad = true;
        }*/

      /*  Log.d(TAG, "bitmap dimensons: height= " + options.outHeight + "width = " + options.outWidth); //todo remove this
        return BitmapFactory.decodeFile(imageDir + filePath, options);
    }

    /**
     * static method 'resizeImage'
     *
     * @param options options object for the bitmap
     * @param reqHeight height to be scaled to
     * @param reqWidth width to be scaled to
     * @return the size of the bitmap scaled
     *
     * scales the bitmap to the proper size based on the required width and height provided
     */

    /*public int resizeImage(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final String TAG = "resizeImage";


        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 0;

        Log.d(TAG,"current width is + " + width + " and height is " + height);
        Log.d(TAG,"reqWidth is " + reqWidth + " and height is " + reqHeight);


        if (height > reqHeight || width > reqWidth) {
            if (inSampleSize == 0) {
                inSampleSize = 2;
            }

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        Log.d(TAG, "resizeImage sample size is: " + inSampleSize);

        return inSampleSize;
    }

    /***
     * static method 'cancelPotentialWork'
     *
     * @param data file name of the image to be compared
     * @param imageView imageView in question
     * @return the status of the task
     *
     * checks to see if the imageView is currently involved with any other process
     * and if so, cancels
     *
     * *ripped from google*
     */
    /*public static boolean cancelPotentialWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView); //todo learn this

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.data;
            Log.d(TAG, bitmapData + "compared to " + data);

            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || !bitmapData.equals(data)) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }

        Log.d(TAG, "cancelPotentialWork found that the imageView is missing or busy");

        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /**
     * static class 'getBitmapWorkerTask'
     *
     * @param imageView imageView in question
     * @return null
     *
     * gets the drawable
     *
     * *ripped from google*
     */
    /*private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
              if (drawable instanceof AsyncDrawable) {
                Log.d(TAG, "getBitmapWorkerTask found an AsyncDrawable");

                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
              }
        }

        Log.d(TAG, "getBitmapWorkerTask> AsyncDrawable was NOT found");

        return null;
    }

    /**
     * class 'AsyncDrawable'
     *
     * creates an AsyncDrawable
     * *ripped from google*
     */
    /*static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }



    /**
     * class 'BitmapWorkerTask'
     *
     * passed a String and returns a Bitmap
     *
     * decodes Bitmap from a filename in a separate thread for performance
     *
     */
    /*static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String data = null;
        private final String TAG = "BitMapWorkerTask: ";


        //data pulled in from loadImages
        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);

            Log.d(TAG,"Pre-Execute started");
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String[] params) {
            data = params[0];
            Log.d(TAG, "image loaded, file name is: " + data);
            return decodeSampledBitmapFromFile(data);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
                else
                {
                    Log.d(TAG,"ImageView was null");
                }
            }
        }
    }*/


    /**
     * tags for replyHandler's various tasks
     */
    static final int MSG_IMAGE_STORED = 0;

    static final int MSG_IMAGE_SENT= 1;

    static final int MSG_DATABASE_CLEARED= 2;

    static final int MSG_PICTURE_TAKEN = 3;

    static final int MSG_SUCCESS_LOCAL = 15;

    static final int MSG_SUCCESS_LIVE = 16;


    /**
     * class 'replyHandler'
     *
     * handles responses from the service
     */
    static class replyHandler extends Handler {

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
            }

            Log.d(TAG,"exit handleMessage");
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
            resolveDns("http://android.ddns.net/");
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
}