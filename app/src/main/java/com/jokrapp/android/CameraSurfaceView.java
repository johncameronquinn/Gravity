package com.jokrapp.android;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by root on 6/18/15.
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final boolean VERBOSE = true;

    private SurfaceHolder mHolder;
    private Camera camera;
    private final String TAG = "CameraPreview";
    private Camera.Size mPreviewSize;


    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public CameraSurfaceView(Context context) {

        super(context);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Camera getCamera() {
        return camera;
    }

    public SurfaceHolder getmHolder() {
        return mHolder;
    }


        public void surfaceCreated(SurfaceHolder holder) {
            if (VERBOSE) {
                Log.v(TAG, "entering surfaceCreated...");
            }

            // The Surface has been created, now tell the camera where to draw the preview

            if (VERBOSE) {
                Log.v(TAG, "exiting surfaceCreated...");
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity
            if (VERBOSE) {
                Log.d(TAG, "enter surfaceDestroyed...");
                Log.d(TAG, "exit surfaceDestroyed...");
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (VERBOSE) {
                Log.d(TAG, "enter surfaceChanged...");
            }

            if (mHolder.getSurface() == null) {
                Log.e(TAG,"mHolder.getSurface() was null... quitting");
                // preview surface does not exist
                return;
            }


            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings

            if (VERBOSE) {
                Log.d(TAG, "exit surfaceChanged...");
            }
        }



    }


