package com.jokrapp.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.UUID;


/**
 * Author/Copyright John C. Quinn All Rights Reserved
 * Date last modified: 2015-06-17
 *
 *
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CameraFragment.OnCameraFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class CameraFragment extends Fragment {
    private static final boolean VERBOSE = true;
    private static final String TAG = "CameraFragment";

    private GestureDetector gestureDetector;
    private static boolean isPreview = true;
    private static boolean isComment = false;

    private EditText commentText = null;
    private String messageTarget = null;

    private int currentCameraMode;
    private CameraReceiver cameraReceiver;

    private final int CAMERA_DEFAULT_MODE = 0;
    private final int CAMERA_MESSAGE_MODE = 1;
    private final int CAMERA_LIVE_MODE = 2;
    private final int CAMERA_REPLY_MODE = 3;

    private static final String CAMERA_MODE_KEY = "ckey";
    private static final String KEY_PREVIEW = "pkey";


    private OnCameraFragmentInteractionListener mListener;

/***************************************************************************************************
 * LIFECYCLE METHODS
 */
 /**
  * Use this factory method to create a new instance of
  * this fragment using the provided parameters.
  *
  * @return A new instance of fragment CameraFragment.
n  */
  // TODO: Rename and change types and number of parameters
 public static CameraFragment newInstance(int cameraMode) {
      CameraFragment fragment = new CameraFragment();
      Bundle args = new Bundle();
      args.putInt(CAMERA_MODE_KEY,cameraMode);
      fragment.setArguments(args);
      return fragment;
    }

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        cameraReceiver = new CameraReceiver();
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                cameraReceiver,
                new IntentFilter(ACTION_PICTURE_TAKEN));
        try {
            mListener = (OnCameraFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(cameraReceiver);
        mListener = null;
        gestureDetector = null;
        cameraReceiver = null;

    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERBOSE)  Log.v(TAG,"onCreate");
        Bundle b = getArguments();
        if (b != null) {
            if (VERBOSE)  Log.v(TAG,"Arguments were supplied to create CameraFragment...");
        }

        if (savedInstanceState != null) {
            Log.d(TAG, "restoring state...");
        }
        GestureDoubleTap gestureDoubleTap = new GestureDoubleTap();
        gestureDetector = new GestureDetector(getActivity(), gestureDoubleTap);

        Log.d(TAG, "Setting CameraFragment to default mode");
        currentCameraMode = 0;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (VERBOSE) {
            Log.v(TAG, "entering onSaveInstanceState...");
        }

        outState.putBoolean(KEY_PREVIEW,isPreview);

        if (VERBOSE) {
            Log.v(TAG, "exiting onSaveInstanceState...");
        }
    }


    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (VERBOSE) Log.v(TAG,"entering onViewStateRestored...");
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            isPreview = savedInstanceState.getBoolean(KEY_PREVIEW);
        }

        if (VERBOSE) Log.v(TAG,"exiting onViewStateRestored...");
    }


    @Override
    public void onPause() {
        super.onPause();
        if (VERBOSE) {
            Log.v(TAG, "enter onPause...");
        }

        if (VERBOSE) {
            Log.v(TAG, "exit onPause...");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (VERBOSE) {
            Log.v(TAG, "enter onResume...");
        }

        if (VERBOSE) {
            Log.v(TAG, "exit onResume...");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (VERBOSE) {
            Log.v(TAG, "enter onStart...");
        }

        if (getView() != null) {
        }


        if (VERBOSE) {
            Log.v(TAG, "exit onStart...");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (VERBOSE) {
            Log.v(TAG, "enter onStop...");
        }
        if (commentText != null && commentText.getVisibility() == View.VISIBLE) {
            commentText.setVisibility(View.INVISIBLE);
            messageTarget = null;
        }

        if (VERBOSE) {
            Log.v(TAG, "exit onStop...");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) {
            Log.v(TAG, "enter onDestroy...");
        }
        if (VERBOSE) {
            Log.v(TAG, "exit onDestroy...");
        }

    }

    /**
     * method 'onCreateView'
     *
     * lifecycle method called when the view is beginning to be created
     *
     * @param inflater the inflater which is creating the view
     * @param container the container in which the view is held
     * @param savedInstanceState if there is a savedinstanceState, data will be stored here
     * @return the view to be created
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"parent is: " + container.toString());
        // Inflate the layout for this fragment
        if (VERBOSE) {
            Log.v(TAG, "enter onCreateView...");
        }
        CommentListener listener = new CommentListener();
        container.setOnTouchListener(listener);


      //creates the camera preview, adds to
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    public class GestureDoubleTap extends GestureDetector.SimpleOnGestureListener  {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (isPreview) { //its in preview mode, switch camera
                mListener.sendMsgSwitchCamera();
            }/* else {
                if (isComment) {  //is in commenting mode, stop
                    isComment = false;
                    commentText.setVisibility(View.INVISIBLE);
                } else { //is not in commenting mode, start
                    isComment = true;
                    commentText.setVisibility(View.VISIBLE);
                }
            }*/
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isPreview) { //its in preview mode, ignore
                mListener.sendMsgAutoFocus(e);

                return true;
            } else { //its not in preview mode, enable comments
                if (isComment) {  //is in commenting mode, stop
                    isComment = false;
                    commentText.setVisibility(View.INVISIBLE);
                    commentText.setText(null);
                    if (getView() != null) {
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                    }
                } else { //is not in commenting mode, start
                    isComment = true;
                    commentText.setVisibility(View.VISIBLE);
                }
            }
            return true;
        }
    }

    class CommentListener implements View.OnTouchListener{
        private float y;
        private float dy;
        private float mLastTouchY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (VERBOSE) {
                //Log.v(TAG,"Event: " + event.toString());
            }
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            } else {
                return false;
            }

        }

    }




    /**
     * method 'onViewCreated'
     *
     * Lifecycle method- called when the current view is created
     *
     * @param view current view that was created
     * @param savedInstanceState a savedinstancestate, if any
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (VERBOSE) {
            Log.v(TAG, "enter onViewCreated...");
        }
        if (savedInstanceState != null) {
            if (VERBOSE) {
                Log.v(TAG, "onViewCreated had a saved instance.");
            }
        }


        TextureView mPreview = (TextureView) view.findViewById(R.id.cameraSurfaceView);
        Log.d(TAG, "setting surface texture listener to cameraHandler");

        MainActivity activity = (MainActivity) getActivity();
        Log.d(TAG, "CameraHandler reference refers to: " + activity.getCameraHandlerSingleton(activity).toString());
        mPreview.setSurfaceTextureListener(activity.getCameraHandlerSingleton(activity));

        Log.d(TAG, "created view is: " + view.toString());


        final Button switchButton = (Button) view.findViewById(R.id.switch_camera);

        switchButton.setOnClickListener(getButtonListener(this));

        Button captureButton = (Button) view.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(getButtonListener(this));
        captureButton.bringToFront();

        commentText = (EditText)view.findViewById(R.id.commentText);

        if (messageTarget != null) {
            Log.d(TAG,"CameraFragment was started with a target UUID. Starting message mode...");
            startMessageMode(messageTarget);
        }



        if (VERBOSE) {
            Log.v(TAG, "exit onViewCreated...");
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (VERBOSE) {
            Log.v(TAG,"enter onDestroyView...");
        }
        getActivity().findViewById(R.id.pager).setOnTouchListener(null);
        gestureDetector = null;
        if (VERBOSE) {
            Log.v(TAG,"exit onDestroyView...");
        }
    }
    /***********************************************************************************************
     *
     * USER INTERACTION
     *
     */

    private static WeakReference<cameraButtonManager> buttonListenerReference;

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
    public static cameraButtonManager getButtonListener(CameraFragment parent) {
        if (buttonListenerReference == null) {
            buttonListenerReference = new WeakReference<>(parent.new cameraButtonManager());
        }
        return buttonListenerReference.get();
    }

    /**
     * class 'cameraButtonManager'
     *
     */
    private class cameraButtonManager implements View.OnClickListener {

        private Button captureButton;
        private Button switchButton;
        private Button flashButton;
        private Button cancelButton;
        private Button localButton;
        private Button liveButton;

        private Button sendMessageButton;
        private Button liveCaptureButton;
        private Button cancelMessageButton;

        @Override
        public void onClick(View v) {

            Log.d(TAG,"click " + v.toString());
            switch (v.getId()) {
                case R.id.button_capture:

                    Activity mActivity = getActivity();
                    //initialize the buttons while the picture is being taken
                    switch (currentCameraMode) {
                        case CAMERA_DEFAULT_MODE:
                            Log.d(TAG,"Taking a picture in default mode...");
                            captureButton = (Button) v;
                            switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            liveButton = (Button) mActivity.findViewById(R.id.button_live);
                            localButton = (Button) mActivity.findViewById(R.id.button_local);
                            cancelButton = (Button) mActivity.findViewById(R.id.button_cancel);
                            mListener.sendMsgTakePicture();
                            v.setPressed(true);
                            v.setClickable(false);
                            break;

                        case CAMERA_MESSAGE_MODE:
                            Log.d(TAG,"Taking a picture in message mode...");
                            captureButton = (Button) v;
                            cancelMessageButton = (Button) mActivity.
                                    findViewById(R.id.button_cancel_message);
                            switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            sendMessageButton = (Button)mActivity.
                                    findViewById(R.id.button_send_message);
                            mListener.sendMsgTakePicture();
                            v.setPressed(true);
                            v.setClickable(false);
                            break;

                        case CAMERA_LIVE_MODE:
                            Log.d(TAG,"Taking a picture in live mode...");
                           captureButton = (Button) v;
                            switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            mListener.sendMsgTakePicture();

                            v.setPressed(true);
                            v.setClickable(false);
                            break;

                        case CAMERA_REPLY_MODE:
                            Log.d(TAG,"Taking a picture in reply mode...");
                            Log.d(TAG,"Taking a picture in live mode...");
                            captureButton = (Button) v;
                            switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            mListener.sendMsgTakePicture();

                            v.setPressed(true);
                            v.setClickable(false);
                            break;
                    }
                    break;

                case R.id.switch_camera:
                    mListener.sendMsgSwitchCamera();
                    break;

                case R.id.button_local:
                    mListener.sendMsgSaveImage(commentText, false);
                    resetCameraUI();
                    break;

                case R.id.button_live:
                    mListener.sendMsgSaveImage(commentText, true); //save the image
                    setCameraUI(CAMERA_LIVE_MODE);

                    liveButton.setVisibility(View.INVISIBLE);
                    cancelButton.setVisibility(View.INVISIBLE);
                    localButton.setVisibility(View.INVISIBLE);

                    startLiveMode();
                    break;

                case R.id.button_cancel:
                    resetCameraUI();
                break;

                case R.id.button_send_message:
                    mListener.sendMsgSaveImage(commentText, false, messageTarget);
                    resetCameraUI();
                    stopMessageMode();

                    mListener.enableScrolling();
                    break;
                case R.id.button_cancel_message:
                    if(sendMessageButton != null) { //this means a picture has been taken, resetUI
                        resetCameraUI();
                    }
                    cancelMessageButton = (Button)getActivity().
                                findViewById(R.id.button_cancel_message);
                    cancelMessageButton.setVisibility(View.INVISIBLE);
                    stopMessageMode();

                    mListener.enableScrolling();
                    break;


            }

        }

        /**
         * method 'setCameraUI()'
         *
         * called when a picture is successfully taken, sets up the UI to allow the user to
         * decide what to do with the taken picture
         *
         * if the UUID target is null, this method sets the camera UI to post mode,
         * allowing the user to select where the app should send the image they just took.
         *
         * if the UUID is not null, this method sets the UUID in message mode,
         *
         * @param mode the mode of the camera UI
         */
        public void setCameraUI(int mode) {

            switch (mode) {

                case CAMERA_DEFAULT_MODE:
                    if (VERBOSE) Log.v(TAG,"Image captured in default mode");
                    isPreview = false;

                    cancelButton.bringToFront();
                    localButton.bringToFront();
                    liveButton.bringToFront();

                    captureButton.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                    flashButton.setVisibility(View.INVISIBLE);

                    captureButton.setPressed(false);
                    captureButton.setClickable(true);

                    cancelButton.setVisibility(View.VISIBLE);
                    localButton.setVisibility(View.VISIBLE);
                    liveButton.setVisibility(View.VISIBLE);

                    localButton.setOnClickListener(this);
                    liveButton.setOnClickListener(this);
                    cancelButton.setOnClickListener(this);
                    break;
                case CAMERA_MESSAGE_MODE:    // message mode
                    if (VERBOSE) Log.v(TAG,"Image captured in message mode");
                    isPreview = false;

                    captureButton.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                    flashButton.setVisibility(View.INVISIBLE);

                    sendMessageButton.setVisibility(View.VISIBLE);
                    sendMessageButton.setOnClickListener(this);
                break;

                case CAMERA_LIVE_MODE:
                    if (VERBOSE) Log.v(TAG,"Image captured in live mode");
                    isPreview = false;

                    captureButton.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                    flashButton.setVisibility(View.INVISIBLE);
                    startNewThreadInputMode((MainActivity)getActivity());

                    break;

                case CAMERA_REPLY_MODE:
                    isPreview = false;
                    if (VERBOSE) Log.v(TAG,"Image captured in reply mode");

                    captureButton.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                    flashButton.setVisibility(View.INVISIBLE);
                    startNewReplyInputMode((MainActivity)getActivity());

                    break;

            }
        }

        /**
         * method 'resetCameraUI()'
         *
         * resets the camera UI back to capture mode
         */
        public void resetCameraUI() {
            switch (currentCameraMode) {
                case CAMERA_MESSAGE_MODE:
                    isPreview = true;

                    mListener.sendMsgStartPreview();

                    commentText.setText("");
                    commentText.setVisibility(View.INVISIBLE);

                    cancelMessageButton.setVisibility(View.INVISIBLE);
                    sendMessageButton.setVisibility(View.INVISIBLE);

                    captureButton.setVisibility(View.VISIBLE);
                    switchButton.setVisibility(View.VISIBLE);
                    flashButton.setVisibility(View.VISIBLE);

                    break;

                case CAMERA_DEFAULT_MODE:
                    isPreview = true;

                    mListener.sendMsgStartPreview();
                    mListener.enableScrolling();

                    commentText.setText("");
                    commentText.setVisibility(View.INVISIBLE);
                    captureButton.setVisibility(View.VISIBLE);
                    switchButton.setVisibility(View.VISIBLE);
                    flashButton.setVisibility(View.VISIBLE);

                    cancelButton.setVisibility(View.INVISIBLE);
                    localButton.setVisibility(View.INVISIBLE);
                    liveButton.setVisibility(View.INVISIBLE);

                    break;

                case CAMERA_LIVE_MODE:
                case CAMERA_REPLY_MODE:
                    isPreview = true;

                    mListener.sendMsgStartPreview();
                    mListener.enableScrolling();

                    captureButton.setVisibility(View.VISIBLE);
                    switchButton.setVisibility(View.VISIBLE);
                    flashButton.setVisibility(View.VISIBLE);
                    break;

            }
            captureButton.setPressed(false);
            captureButton.setClickable(true);
            currentCameraMode = CAMERA_DEFAULT_MODE;
        }
    }

    /***********************************************************************************************
     *
     *   CAMERA MANAGEMENT
     *
     */
    /**
     * Interface to receive picture callback from the capturebutton
     *
     * sets a new click listener for the confirm button, which saves the file
     */


     /**
      * method 'onPictureTaken'
      *
      * callback method that is called when a picture is taken
      *
      * @param Success whether or not the image saving was a success
      */
     public void onPictureTaken(int Success) {
         if (VERBOSE) {
             Log.v(TAG, "enter onPictureTaken... ");
         }
         getButtonListener(this).setCameraUI(currentCameraMode);
         if (VERBOSE) {
             Log.v(TAG, "exit onPictureTaken...");
         }
     }

/***************************************************************************************************
 * MESSAGING
 *
 */
   public void startMessageMode(String id) {
       Log.i(TAG,"preparing to send message to user: " + id);

       //final int dp = 60;
       //calculates density pixels
       /**DisplayMetrics metrics = new DisplayMetrics();
       getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
       float logicalDensity = metrics.density;
       int px = (int) Math.ceil(dp * logicalDensity);

       FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px,px);
       params.gravity = (Gravity.TOP | Gravity.CENTER);
       params.setMargins(10,50,10,10);

       cancel = new Button(getActivity());
       cancel.setLayoutParams(params);
       cancel.setBackgroundDrawable(getActivity().getResources().getDrawable(R.drawable.cancel));*/

       //FrameLayout layout = (FrameLayout)getView().findViewById(R.id.camera_root);
       //layout.addView(cancel);
       Button cancel;
       cancel = (Button)getView().findViewById(R.id.button_cancel_message);
       cancel.setVisibility(View.VISIBLE);
       cancel.setOnClickListener(getButtonListener(this));
       messageTarget = id;
       currentCameraMode = CAMERA_MESSAGE_MODE;
   }

   public String getMessageTarget() {
       return messageTarget;
   }

   public void setMessageTarget(String target) {
       messageTarget = target;
   }

   public void stopMessageMode() {
       messageTarget = null;
       currentCameraMode = CAMERA_DEFAULT_MODE;
   }

    /***************************************************************************************************
     * LIVE MODE
      */

    private static WeakReference<LiveModeButtonListener> buttonPostListenerReference;

    public static LiveModeButtonListener getLiveModeButtonListener(CameraFragment parent) {
        if (buttonPostListenerReference == null) {
            buttonPostListenerReference = new WeakReference<>(parent.new LiveModeButtonListener(parent));
        }
        return buttonPostListenerReference.get();
    }

    /***************************************************************************************************
     * USER INTERACTION
     */
    /**
     * class 'livePostButtonListener'
     *
     * special class that listens to the live_write_post buttons, and gets data
     */
    public class LiveModeButtonListener implements View.OnClickListener {

        View createThreadView;

        private CameraFragment parent;

        public LiveModeButtonListener(CameraFragment parent) {
            this.parent = parent;
        }

        public void setCreateView(View view) {
            createThreadView = view;
        }

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG,"OnClickRegistered..." + v.toString());
            }
            MainActivity activity = (MainActivity)getActivity();
            InputMethodManager imm = (InputMethodManager)activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            switch (v.getId()) {
                case R.id.button_camera_live_mode_cancel:
                    ((ViewGroup)v.getParent().getParent()).removeView((View) v.getParent());
                    activity.removePendingLiveImage();
                    imm.hideSoftInputFromWindow(createThreadView.getWindowToken(), 0);
                    activity.enableScrolling();

                    CameraFragment.getButtonListener(parent).resetCameraUI();
                    buttonPostListenerReference = null;
                    break;
                case R.id.button_camera_live_mode_confirm:
                    FrameLayout layout = (FrameLayout) v.getParent().getParent();
                    String title = ((EditText)layout.findViewById(R.id.editText_live_mode_title)).getText().toString();
                    String description = ((EditText)layout.findViewById(R.id.editText_live_mode_description)).getText().toString();
                    activity.setLiveCreateThreadInfo(title, description);
                    layout.removeView((View) v.getParent());
                    imm.hideSoftInputFromWindow(createThreadView.getWindowToken(), 0);

                    activity.enableScrolling();

                    CameraFragment.getButtonListener(parent).resetCameraUI();
                    buttonPostListenerReference = null;
                    break;
            }
        }

    }

    public void startLiveMode() {
       Log.i(TAG, "Camera is entering live mode...");
       currentCameraMode = CAMERA_LIVE_MODE;
   }

    public void startNewThreadInputMode(MainActivity activity) {
        if (VERBOSE) {
            Log.v(TAG,"entering startNewThreadInputMode...");
        }
        activity.disableScrolling();
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout root = (FrameLayout)activity.findViewById(R.id.camera_root);
        View v = inflater.inflate(R.layout.camera_live_mode,
                root,
                false);

        getLiveModeButtonListener(this).setCreateView(v);
        v.findViewById(R.id.button_camera_live_mode_cancel).setOnClickListener(getLiveModeButtonListener(this));
        v.findViewById(R.id.button_camera_live_mode_confirm).setOnClickListener(getLiveModeButtonListener(this));
        root.addView(v,root.getChildCount());

        if (VERBOSE) {
            Log.v(TAG,"exiting startNewThreadInputMode...");
        }
    }
/***************************************************************************************************
 * REPLY MODE
*/
    private static WeakReference<ReplyModeButtonListener> buttonReplyListenerReference;

    public static ReplyModeButtonListener getReplyModeButtonListener(CameraFragment parent) {
        if (buttonReplyListenerReference == null) {
            buttonReplyListenerReference = new WeakReference<>(parent.new ReplyModeButtonListener(parent));
        }
        return buttonReplyListenerReference.get();
    }

    /**
     * class 'ReplyMostButtonListener'
     *
     * special class that listens to the buttons inflated from the layout camera_reply_mode
     */
    public class ReplyModeButtonListener implements View.OnClickListener {

        View createReplyView;

        private CameraFragment parent;

        public ReplyModeButtonListener(CameraFragment parent) {
            this.parent = parent;
        }

        public void setCreateView(View view) {
            createReplyView = view;
        }

        @Override
        public void onClick(View v) {
            if (VERBOSE) {
                Log.v(TAG,"OnClickRegistered..." + v.toString());
            }
            MainActivity activity = (MainActivity)getActivity();
            InputMethodManager imm = (InputMethodManager)activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            switch (v.getId()) {
                case R.id.button_camera_reply_mode_cancel:
                    ((ViewGroup)v.getParent().getParent()).removeView((View) v.getParent());
                    activity.removePendingLiveImage();
                    imm.hideSoftInputFromWindow(createReplyView.getWindowToken(), 0);
                    activity.enableScrolling();

                    CameraFragment.getButtonListener(parent).resetCameraUI();
                    buttonReplyListenerReference = null;
                    break;
                case R.id.button_camera_live_mode_confirm:
                    FrameLayout layout = (FrameLayout) v.getParent().getParent();
                    String description = ((EditText)layout.findViewById(R.id.editText_reply_mode_comment)).getText().toString();
                    activity.setLiveCreateReplyInfo("unset", description,0);//todo load name from sharedpreferences
                    layout.removeView((View) v.getParent());
                    imm.hideSoftInputFromWindow(createReplyView.getWindowToken(), 0);

                    activity.enableScrolling();

                    CameraFragment.getButtonListener(parent).resetCameraUI();
                    buttonReplyListenerReference = null;
                    break;
            }
        }

    }

    public void startNewReplyInputMode(MainActivity activity) {
        if (VERBOSE) {
            Log.v(TAG,"entering startNewReplyInputMode...");
        }
        activity.disableScrolling();
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout root = (FrameLayout)activity.findViewById(R.id.camera_root);
        View v = inflater.inflate(R.layout.camera_reply_mode,
                root,
                false);

        getReplyModeButtonListener(this).setCreateView(v);
        v.findViewById(R.id.button_camera_reply_mode_cancel).setOnClickListener(getReplyModeButtonListener(this));
        v.findViewById(R.id.button_camera_reply_mode_confirm).setOnClickListener(getReplyModeButtonListener(this));
        root.addView(v,root.getChildCount());

        if (VERBOSE) {
            Log.v(TAG,"exiting startNewReplyInputMode...");
        }
    }


    public void stopLiveMode() {
       currentCameraMode = CAMERA_DEFAULT_MODE;
   }

   public void startReplyMode() {
       Log.i(TAG,"Camera is entering reply mode...");
       currentCameraMode = CAMERA_REPLY_MODE;
   }

   public void stopReplyMode() {
       currentCameraMode = CAMERA_DEFAULT_MODE;
   }

    /**
     * Communication back to the activity
     *
     */
    /**
     * currently, this method passed the file path to a function in the MainActivity
     * that loads the bitmap
     */
    public interface OnCameraFragmentInteractionListener {
        void enableScrolling();
        void toggleScrolling();
        void sendMsgStartPreview();
        void sendMsgTakePicture();
        void createLiveThread();
        void sendMsgSaveImage(EditText comment, boolean postToLive);
        void sendMsgSaveImage(EditText comment, boolean postToLive, String messageTarget);
        void sendMsgSwitchCamera();
        void sendMsgAutoFocus(MotionEvent event);
    }

    static final String ACTION_PICTURE_TAKEN = "com.jokrapp.android.picturetaken";
    static final String ACTION_PICTURE_SAVED = "com.jokrapp.android.picturesaved";
    public class CameraReceiver extends BroadcastReceiver implements Runnable {
        private int success;
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_PICTURE_TAKEN:
                    success = intent.getIntExtra("success",0);
                    getActivity().runOnUiThread(this);
                    break;
            }
        }


        @Override
        public void run() {
            onPictureTaken(success);
        }
    }
}
