package us.gravwith.android;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

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
public class CameraFragment extends Fragment implements Camera.AutoFocusCallback,
        ValueAnimator.AnimatorUpdateListener, MessageHandler.CameraListener {
    private static final boolean VERBOSE = true;
    private static final String TAG = "CameraFragment";
    private GestureDetector gestureDetector;
    private static boolean isPreview = true;
    private static boolean isComment = false;
    private EditText commentText = null;
    private static String messageTarget = null;
    private FrameLayout captureLayout;
    private int currentCameraMode;
    private CameraReceiver cameraReceiver;

    private final int number_of_cameras = Camera.getNumberOfCameras();

    public static final int CAMERA_MESSAGE_MODE = 1;
    public static final int CAMERA_LIVE_MODE = 2;
    public static final int CAMERA_REPLY_MODE = 3;
    public static final int CAMERA_DEFAULT_MODE = CAMERA_REPLY_MODE;

    private final int FOCUS_AREA_ANIMATION_DURATION = 300;
    private final int CAPTURE_LAYOUT_ANIMATION_DURATION = 300;
    private final int CAPTURE_LAYOUT_START_DP = 120;
    private final int CAPTURE_LAYOUT_END_DP = 100;

    private static final String CAMERA_MODE_KEY = "ckey";
    private static final String KEY_PREVIEW = "pkey";

    TextureView mPreview;

    View container;

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

        mListener = (OnCameraFragmentInteractionListener) activity;
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(cameraReceiver);
        mListener = null;
        gestureDetector = null;
        cameraReceiver = null;

        super.onDetach();
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
        cameraButtonInstance = new CameraButtonManager();

        Log.d(TAG, "Setting CameraFragment to default mode");
        currentCameraMode = CAMERA_DEFAULT_MODE;
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
        if (VERBOSE) {
            Log.v(TAG, "enter onDestroy...");
        }
        cameraButtonInstance = null;
        if (VERBOSE) {
            Log.v(TAG, "exit onDestroy...");
        }
        super.onDestroy();
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
        // Inflate the layout for this fragment
        if (VERBOSE) {
            Log.v(TAG, "enter onCreateView...");
        }
        CommentListener listener = new CommentListener();

        this.container = container;
        this.container.setOnTouchListener(listener);

        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        mPreview = (TextureView) view.findViewById(R.id.cameraSurfaceView);
        captureLayout = (FrameLayout) view.findViewById(R.id.capture_layout);

        return view;
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

        /**
         * method 'onDown'
         * @param e the incoming event
         * @return true to attempt to detect this event, false to discard
         */
        @Override
        public boolean onDown(MotionEvent e) {
            switch (currentCameraMode) {
                /*case CAMERA_DEFAULT_MODE:
                    return true;*/
                case CAMERA_MESSAGE_MODE:
                    return true;
                case CAMERA_REPLY_MODE:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * method 'onSingleTapConfirmed'
         *
         * this method provides functionality to singletaps,
         * if the preview is running, trigger an autofocus, if it is not, toggle comment
         *
         *
         * @param e incoming event
         * @return whether or not the event was consumed
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (VERBOSE) Log.v(TAG,"entering onSingleTapConfirmed...");

            if (isPreview) { //its in preview mode, so trigger an auto-focus

                startAutoFocus(e);

                if (VERBOSE) Log.v(TAG,"exiting onSingleTapConfirmed...");
                return true;
            } else { //its not in preview mode,  toggle comment
                if (isComment) {  //is in commenting mode, stop
                    isComment = false;
                    commentText.setVisibility(View.INVISIBLE);
                    commentText.setText("");
                    hideSoftKeyboard(commentText);
                } else { //is not in commenting mode, start
                    isComment = true;
                    commentText.setVisibility(View.VISIBLE);
                }
            }
            if (VERBOSE) Log.v(TAG,"exiting onSingleTapConfirmed...");
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
//                Log.v(TAG,"Event: " + event.toString());
            }
            return gestureDetector.onTouchEvent(event);
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
        Log.d(TAG, "setting surface texture listener to cameraHandler");

        if (number_of_cameras == 1) {
            (container.findViewById(R.id.switch_camera)).setVisibility(View.GONE);
        } else {
            CheckBox switchButton = (CheckBox) view.findViewById(R.id.switch_camera);
            switchButton.setOnClickListener(cameraButtonInstance);
        }

        MainActivity activity = (MainActivity) getActivity();
        //Log.d(TAG, "CameraHandler reference refers to: " + activity.getCameraHandlerSingleton(activity).toString());
        mPreview.setSurfaceTextureListener(MainActivity.sHandler);

        Log.d(TAG, "created view is: " + view.toString());


        ImageButton captureButton = (ImageButton) view.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(cameraButtonInstance);
        captureButton.bringToFront();

        commentText = (EditText)view.findViewById(R.id.commentText);
        commentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSoftKeyboard(commentText);
            }
        });

        if (messageTarget != null) {
            Log.d(TAG,"CameraFragment was started with a target UUID. Starting message mode...");
            startMessageMode(messageTarget);
        }

        //mListener.sendMsgStartPreview();
        if (VERBOSE) {
            Log.v(TAG, "exit onViewCreated...");
        }

    }


    @Override
    public void onDestroyView() {
        if (VERBOSE) {
            Log.v(TAG,"enter onDestroyView...");
        }
        container.setOnTouchListener(null);
        container = null;
        gestureDetector = null;
        mPreview = null;
        commentText = null;

        if (VERBOSE) {
            Log.v(TAG,"exit onDestroyView...");
        }
        super.onDestroyView();
    }
    /***********************************************************************************************
     *
     * USER INTERACTION
     *
     */

    /**
     * static method getButtonListener
     *
     * this is used to create a singleton reference to a buttonlistener, allowing one
     * buttonlistener to handle all button interactions
     *
     * @param parent the context in which this functions
     * @return a ButtonListener to be used by all the buttons in CameraFragment
     */

    private static CameraButtonManager cameraButtonInstance;

    /**
     * class 'CameraButtonManager'
     *
     */
    private class CameraButtonManager implements View.OnClickListener {

        private ImageButton captureButton;
        private FrameLayout captureLayout;

        private Button switchButton;
        private Button flashButton;
        private Button cancelButton;
        //private Button localButton;
        private Button liveButton;

        private Button sendMessageButton;
        private Button liveCaptureButton;
        private Button cancelMessageButton;

        @Override
        public void onClick(View v) {

            Bundle b = new Bundle();
            b.putString(Constants.KEY_ANALYTICS_CATEGORY,Constants.ANALYTICS_CATEGORY_CAMERA);


            switch (v.getId()) {
                case R.id.button_capture:
                    b.putString(Constants.KEY_ANALYTICS_ACTION,"camera capture");

                    Activity mActivity = getActivity();
                    //initialize the buttons while the picture is being taken
                    switch (currentCameraMode) {
                        /*case CAMERA_DEFAULT_MODE:
                            if (Constants.LOGD) Log.d(TAG,"Taking a picture in default mode...");
                            captureButton = (ImageButton) v;
                            switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            liveButton = (Button) mActivity.findViewById(R.id.button_live);
                            //localButton = (Button) mActivity.findViewById(R.id.button_local);
                            cancelButton = (Button) mActivity.findViewById(R.id.button_cancel);
                            mListener.sendMsgTakePicture();
                            v.setPressed(true);
                            v.setClickable(false);

                            b.putString(Constants.KEY_ANALYTICS_LABEL,"default mode");


                            break;*/

                        case CAMERA_MESSAGE_MODE:
                            if (Constants.LOGD) Log.d(TAG,"Taking a picture in message mode...");
                            captureButton = (ImageButton) v;
                            cancelMessageButton = (Button) mActivity.
                                    findViewById(R.id.button_cancel_message);
                            if (number_of_cameras > 1) {
                                switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            }
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            sendMessageButton = (Button)mActivity.
                                    findViewById(R.id.button_send_message);
                            mListener.sendMsgTakePicture();
                            v.setPressed(true);
                            v.setClickable(false);

                            b.putString(Constants.KEY_ANALYTICS_LABEL, "message mode");

                            break;

                        case CAMERA_LIVE_MODE:
                            if (Constants.LOGD) Log.d(TAG,"Taking a picture in live mode...");
                           captureButton = (ImageButton) v;
                            if (number_of_cameras > 1) {
                                switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            }
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            mListener.sendMsgTakePicture();

                            v.setPressed(true);
                            v.setClickable(false);

                            b.putString(Constants.KEY_ANALYTICS_LABEL, "live mode");

                            break;

                        case CAMERA_REPLY_MODE:
                            if (Constants.LOGD) Log.d(TAG,"Taking a picture in reply mode...");
                            captureButton = (ImageButton) v;
                            if (number_of_cameras > 1) {
                                switchButton = (Button) mActivity.findViewById(R.id.switch_camera);
                            }
                            flashButton = (Button) mActivity.findViewById(R.id.button_flash);
                            mListener.sendMsgTakePicture();
                            sendMessageButton = (Button)mActivity.
                                    findViewById(R.id.button_send_message);
                            cancelMessageButton = (Button) mActivity.
                                    findViewById(R.id.button_cancel_message);
                            commentText.setText(((MainActivity) getActivity()).getReplyComment());

                            v.setPressed(true);
                            v.setClickable(false);

                            b.putString(Constants.KEY_ANALYTICS_LABEL, "reply mode");
                            break;
                    }
                    break;

                case R.id.switch_camera:
                    if (number_of_cameras > 1) {
                        mListener.sendMsgSwitchCamera();
                    }
                    b.putString(Constants.KEY_ANALYTICS_ACTION, "switch camera");
                    break;

                /*case R.id.button_local:
                    mListener.sendMsgSaveImage(commentText, CAMERA_DEFAULT_MODE);
                    resetCameraUI();

                    b.putString(Constants.KEY_ANALYTICS_ACTION, "save to local");
                    break;*/

                case R.id.button_live:
                    mListener.sendMsgSaveImage(commentText, CAMERA_LIVE_MODE); //save the image
                    setCameraUI(CAMERA_LIVE_MODE);

                    liveButton.setVisibility(View.INVISIBLE);
                    cancelButton.setVisibility(View.INVISIBLE);
                    //localButton.setVisibility(View.INVISIBLE);

                    b.putString(Constants.KEY_ANALYTICS_ACTION, "save to live");
                    startLiveMode();
                    break;

                case R.id.button_cancel:

                    b.putString(Constants.KEY_ANALYTICS_ACTION, "cancel");
                    b.putString(Constants.KEY_ANALYTICS_LABEL, "current camera");
                    b.putString(Constants.KEY_ANALYTICS_VALUE, String.valueOf(currentCameraMode));

                    resetCameraUI();
                break;

                case R.id.button_send_message:

                    b.putString(Constants.KEY_ANALYTICS_ACTION,"camera send to");

                    switch(currentCameraMode) {
                        case CAMERA_REPLY_MODE:

                            mListener.sendMsgSaveImage(commentText, CAMERA_LIVE_MODE);
                            ((MainActivity) getActivity()).setLiveCreateThreadInfo("",commentText.getText().toString());
                            resetCameraUI();
                            stopReplyMode();

                            b.putString(Constants.KEY_ANALYTICS_LABEL, "reply");
                            break;
                        case CAMERA_MESSAGE_MODE:

                            mListener.sendMsgSaveImage(commentText, CAMERA_MESSAGE_MODE, messageTarget);
                            resetCameraUI();
                            stopMessageMode();

                            b.putString(Constants.KEY_ANALYTICS_LABEL,"message");
                            break;
                    }

                    mListener.enableScrolling();
                    break;
                case R.id.button_cancel_message:

                    b.putString(Constants.KEY_ANALYTICS_ACTION, "camera cancel send");

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

            mListener.sendMsgReportAnalyticsEvent(b);

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

            if (captureLayout == null ) {
                captureLayout = (FrameLayout)captureButton.getParent();
            }

            switch (mode) {

                /*case CAMERA_DEFAULT_MODE:
                    if (VERBOSE) Log.v(TAG,"Image captured in default mode");
                    isPreview = false;

                    cancelButton.bringToFront();
                    //localButton.bringToFront();
                    liveButton.bringToFront();

                    //captureButton.setVisibility(View.INVISIBLE);
                    captureLayout.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                    flashButton.setVisibility(View.INVISIBLE);

                    captureButton.setPressed(false);
                    captureButton.setClickable(true);

                    cancelButton.setVisibility(View.VISIBLE);
                    //localButton.setVisibility(View.VISIBLE);
                    liveButton.setVisibility(View.VISIBLE);

                    //localButton.setOnClickListener(this);
                    liveButton.setOnClickListener(this);
                    cancelButton.setOnClickListener(this);
                    break;*/
                case CAMERA_MESSAGE_MODE:    // message mode
                    if (VERBOSE) Log.v(TAG,"Image captured in message mode");
                    isPreview = false;

                    //captureButton.setVisibility(View.INVISIBLE);
                    captureLayout.setVisibility(View.INVISIBLE);
                    if (number_of_cameras > 1) {
                        switchButton.setVisibility(View.INVISIBLE);
                    }
                    flashButton.setVisibility(View.INVISIBLE);

                    sendMessageButton.setVisibility(View.VISIBLE);
                    sendMessageButton.setOnClickListener(this);
                break;

                case CAMERA_LIVE_MODE:
                    if (VERBOSE) Log.v(TAG,"Image captured in live mode");
                    isPreview = false;

                    //captureButton.setVisibility(View.INVISIBLE);
                    captureLayout.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                    flashButton.setVisibility(View.INVISIBLE);
                    startNewThreadInputMode((MainActivity)getActivity());

                    break;

                case CAMERA_REPLY_MODE:
                    isPreview = false;
                    if (VERBOSE) Log.v(TAG,"Image captured in reply mode");

                    //captureButton.setVisibility(View.INVISIBLE);
                    captureLayout.setVisibility(View.INVISIBLE);
                    if (number_of_cameras > 1) {
                        switchButton.setVisibility(View.INVISIBLE);
                    }
                    flashButton.setVisibility(View.INVISIBLE);
                    commentText.setVisibility(View.VISIBLE);
                   // startNewReplyInputMode((MainActivity)getActivity());

                    cancelMessageButton.setVisibility(View.VISIBLE);
                    cancelMessageButton.setOnClickListener(this);
                    sendMessageButton.setVisibility(View.VISIBLE);
                    sendMessageButton.setOnClickListener(this);
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

                case CAMERA_REPLY_MODE:
                case CAMERA_MESSAGE_MODE:
                    isPreview = true;

                    mListener.sendMsgStartPreview();

                    commentText.setText("");
                    commentText.setVisibility(View.INVISIBLE);

                    cancelMessageButton.setVisibility(View.INVISIBLE);
                    sendMessageButton.setVisibility(View.INVISIBLE);

                    //captureButton.setVisibility(View.VISIBLE);
                    captureLayout.setVisibility(View.VISIBLE);
                    flashButton.setVisibility(View.VISIBLE);

                    if (number_of_cameras > 1) {
                        switchButton.setVisibility(View.VISIBLE);
                    }
                    break;

                /*case CAMERA_DEFAULT_MODE:
                    isPreview = true;

                    mListener.sendMsgStartPreview();
                    mListener.enableScrolling();

                    commentText.setText("");
                    commentText.setVisibility(View.INVISIBLE);
                    //captureButton.setVisibility(View.VISIBLE);
                    captureLayout.setVisibility(View.VISIBLE);
                    if (number_of_cameras > 1) {
                        switchButton.setVisibility(View.VISIBLE);
                    }
                    flashButton.setVisibility(View.VISIBLE);

                    cancelButton.setVisibility(View.INVISIBLE);
                    //localButton.setVisibility(View.INVISIBLE);
                    liveButton.setVisibility(View.INVISIBLE);

                    break;*/

                case CAMERA_LIVE_MODE:
                    isPreview = true;

                    mListener.sendMsgStartPreview();
                    mListener.enableScrolling();

                    //captureButton.setVisibility(View.VISIBLE);
                    captureLayout.setVisibility(View.VISIBLE);
                    if (number_of_cameras > 1) {
                        switchButton.setVisibility(View.VISIBLE);
                    }
                    flashButton.setVisibility(View.VISIBLE);
                    break;

            }
            hideSoftKeyboard(commentText);
            captureButton.setPressed(false);
            captureButton.setClickable(true);
            currentCameraMode = CAMERA_DEFAULT_MODE;

            //set disable image preview
            ImageView view = (ImageView)container.findViewById(R.id.camera_image_view);
            view.setImageBitmap(null);
            view.setVisibility(View.GONE);
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
         cameraButtonInstance.setCameraUI(currentCameraMode);
         if (VERBOSE) {
             Log.v(TAG, "exit onPictureTaken...");
         }
     }


    /**
     * method 'onAutoFocus'
     *
     * called by {@link MainActivity.CameraHandler} when a focus action
     * is finished. Returns success or fail, and animates the views accordingly
     *
     * @param success whether or not the focus was successful
     * @param camera the camera which was used in the focus
     */
    @Override
    public void onAutoFocus(final boolean success, Camera camera) {

        /*
         * returns the layout to its starting size
         */
        int startpx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                CAPTURE_LAYOUT_END_DP,
                getResources().getDisplayMetrics()));
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                CAPTURE_LAYOUT_START_DP,
                getResources().getDisplayMetrics()));

        final ValueAnimator animator = ValueAnimator.ofInt(startpx, px);
        animator.setDuration(CAPTURE_LAYOUT_ANIMATION_DURATION);
        animator.addUpdateListener(this);

        /*
         * Animates the focusView and dodads on the UI thread
         */
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //the focusView should disappear now. Shrink to size 0.
                    View focusView = getActivity().findViewById(R.id.camera_focus);
                    focusView.animate()
                            .scaleX(0.0f)
                            .scaleY(0.0f)
                            .setDuration(FOCUS_AREA_ANIMATION_DURATION);
                    if (success) {
                        // this does nothing special currently, just logs.
                        Log.i(TAG, "success!");
                    } else {
                        //since the operation failed, make the dodad's shake now.
                        Animation shake = AnimationUtils.loadAnimation(getActivity(),
                                R.anim.shake);
                        View x = captureLayout.findViewById(R.id.do_dad_one);
                        View vx = captureLayout.findViewById(R.id.do_dad_two);
                        x.startAnimation(shake);
                        vx.startAnimation(shake);

                        //set the layout to return to its start AFTER the shake finishes
                        animator.setStartDelay(shake.getDuration());

                        //logs the failure
                        Log.i(TAG, "fail!");
                    }

                    //starts the layout animation
                    animator.start();
                }

            });
        }

    }

    /**
     * method 'startAutoFocus'
     *
     * uses the tap area to start an area autofocus
     *
     * this method sends a message to the camera thread to perform an autofocus, and
     * displays an animation until the focus successfully returns
     *
     * @param e motionevent containing the data from the tap
     */
    private void startAutoFocus(MotionEvent e) {

        //sends the message to start an autofocus event
        int autoFocus = mListener.sendMsgAutoFocus(e);

        if (autoFocus == 0) { // this should be returned if the message sent successfully

            View v= getView();
            if (v!= null) {
                View focusView = getView().findViewById(R.id.camera_focus);
                focusView.setX(e.getX() - focusView.getWidth() / 2);
                focusView.setY(e.getY() - focusView.getHeight());

                focusView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(FOCUS_AREA_ANIMATION_DURATION);
            }

            /*
             * perform the autofocus animation - the dodads should touch the camera capture button,
             * and wait.
             */

            int startpx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    CAPTURE_LAYOUT_START_DP,
                    getResources().getDisplayMetrics()));
            int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    CAPTURE_LAYOUT_END_DP,
                    getResources().getDisplayMetrics()));



            ValueAnimator animator = ValueAnimator.ofInt(startpx,px);
            animator.setDuration(CAPTURE_LAYOUT_ANIMATION_DURATION);
            animator.addUpdateListener(this);
            animator.start();

        } else {
            Log.e(TAG, "error sending message to start autofocus...");
        }

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        int value = (int)animation.getAnimatedValue();
        captureLayout.getLayoutParams().height = value;
        captureLayout.getLayoutParams().width = value;
        captureLayout.requestLayout();
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

       if (getView() != null) {
           cancel = (Button) getView().findViewById(R.id.button_cancel_message);
           cancel.setVisibility(View.VISIBLE);
           cancel.setOnClickListener(cameraButtonInstance);
       }
       messageTarget = id;
       currentCameraMode = CAMERA_MESSAGE_MODE;
   }

   public String getMessageTarget() {
       return messageTarget;
   }

   public static void setMessageTarget(String target) {
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

                    cameraButtonInstance.resetCameraUI();
                    buttonPostListenerReference = null;
                    break;

                case R.id.button_camera_live_mode_confirm:
                    FrameLayout layout = (FrameLayout) v.getParent().getParent();
                    String title = ((EditText)layout.findViewById(R.id.editText_live_mode_title))
                            .getText().toString();
                    String description = ((EditText)layout
                           .findViewById(R.id.editText_live_mode_description)).getText().toString();
                    activity.setLiveCreateThreadInfo(title, description);
                    layout.removeView((View) v.getParent());
                    imm.hideSoftInputFromWindow(createThreadView.getWindowToken(), 0);
                    activity.enableScrolling();

                    cameraButtonInstance.resetCameraUI();
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
        ((EditText)v.findViewById(R.id.editText_live_mode_title)).setText(commentText.getText());
        commentText.setVisibility(View.INVISIBLE);
        commentText.setText("");

        getLiveModeButtonListener(this).setCreateView(v);
        v.findViewById(R.id.button_camera_live_mode_cancel)
                .setOnClickListener(getLiveModeButtonListener(this));
        v.findViewById(R.id.button_camera_live_mode_confirm)
                .setOnClickListener(getLiveModeButtonListener(this));
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

                    cameraButtonInstance.resetCameraUI();
                    buttonReplyListenerReference = null;
                    break;
                case R.id.button_camera_live_mode_confirm:
                    FrameLayout layout = (FrameLayout) v.getParent().getParent();
                    String description = ((EditText)layout
                            .findViewById(R.id.editText_reply_mode_comment)).getText().toString();
                    activity.setLiveCreateReplyInfo("unset", description,0);
                    //todo load name from sharedpreferences
                    layout.removeView((View) v.getParent());
                    imm.hideSoftInputFromWindow(createReplyView.getWindowToken(), 0);

                    activity.enableScrolling();

                    cameraButtonInstance.resetCameraUI();
                    buttonReplyListenerReference = null;
                    break;
            }
        }

    }

    private void hideSoftKeyboard(EditText input) {
        input.clearFocus();
        Log.e(TAG, "hiding the view with focus...");
        input.setInputType(0);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    private void showSoftKeyboard(EditText input) {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    public void stopLiveMode() {
       currentCameraMode = CAMERA_DEFAULT_MODE;
   }

   public void startReplyMode() {
       Log.i(TAG,"Camera is entering reply mode...");
       Button cancel;
       cancel = (Button)getView().findViewById(R.id.button_cancel_message);
       cancel.setVisibility(View.VISIBLE);
       cancel.setOnClickListener(cameraButtonInstance);
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
        void sendMsgSaveImage(EditText comment, int postWhere);
        void sendMsgSaveImage(EditText comment, int postWhere, String messageTarget);
        void sendMsgSwitchCamera();
        int sendMsgAutoFocus(MotionEvent event);
        void sendMsgReportAnalyticsEvent(Bundle b);
    }

    static final String ACTION_PICTURE_TAKEN = "us.gravwith.android.picturetaken";
    static final String ACTION_PICTURE_SAVED = "us.gravwith.android.picturesaved";
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
