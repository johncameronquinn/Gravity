package us.gravwith.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by caliamara on 1/8/16.
 */
public class MessageHandler extends Handler{

    private static final String LOG_TAG = MessageHandler.class.getSimpleName();

/***************************************************************************************************
 * INTERFACES AND LISTENERS
**/
    private static LivePostListener mLiveListener;
    private static LiveRefreshListener mLiveRefreshListener;

    private static CameraListener cameraListener;

    private static ImageDownloadStatusListener mImageDownloadListener;

    interface CameraListener {
        void onPictureTaken(int whichCamera);
    }

    interface LivePostListener {
        void onCreateThreadCompleted(int responseCode);
        void onCreateThreadStarted();
        void onCreateThreadFailed();
    }

    interface LiveRefreshListener {
        void onRefreshComplete(int responseCode);
    }


    interface ImageDownloadStatusListener {
        void onImageDownloadCompleted(String imageKey);
        void onImageDownloadFailed(String imageKey);
    }

    public static void setLivePostListener(LivePostListener livePostListener) {
        mLiveListener = livePostListener;
    }

    public static void setLiveRefreshListener(LiveRefreshListener listener) {
        mLiveRefreshListener = listener;
    }

    public static void clearLivePostListener() {
        mLiveListener = null;
    }

    public static void setImageDownloadStatusListener(ImageDownloadStatusListener listener) {
        mImageDownloadListener = listener;
    }


    public static void setCameraListener(CameraListener listener) {
        cameraListener = listener;
    }

    static final int MSG_UPLOAD_PROGRESS = 1;

    static final int MSG_DATABASE_CLEARED= 2;

    static final int MSG_PICTURE_TAKEN = 3;

    static final int MSG_TOO_MANY_REQUESTS = 51;

    static final int MSG_LIVE_REFRESH_DONE = 52;

    static final int MSG_UNAUTHORIZED = 401;

    static final int MSG_BAD_INPUT = 422;

    static final int MSG_NOT_FOUND = 404;

    static final int MSG_NOTHING_RETURNED = 55;

    public static final int ERROR_CAMERA = 501;
         public static final int ERROR_CAMERA_OPENING_FAILED = 1;


    WeakReference<Activity> activity;

    public Handler setParent(Activity parent) {
        activity = new WeakReference<>(parent);
        return this;
    }


    public void handleMessage(Message msg) {
        if (Constants.LOGD) Log.d(LOG_TAG, "enter handleMessage");
        int respCode = msg.what;

        Bundle data = msg.getData();

        switch (respCode) {

            case PhotoManager.REQUEST_COMPLETE:
            case PhotoManager.REQUEST_FAILED:
            case PhotoManager.REQUEST_STARTED:
                /* pass message to photoManager handler */

                msg = Message.obtain(PhotoManager.getMainHandler(),respCode,msg.obj);
                msg.setData(data);
                msg.sendToTarget();

                break;

            case DataHandlingService.MSG_REQUEST_REPLIES:
                break;

            case DataHandlingService.MSG_REQUEST_LOCAL_POSTS:
                break;

            case DataHandlingService.MSG_CREATE_THREAD:
                if (Constants.LOGV)Log.v(LOG_TAG, "entering msg_create_thread");
                 /*   Toast.makeText(activity.get(),
                            "created live thread info received...",
                            Toast.LENGTH_LONG)
                            .show();*/
                switch (msg.arg1) {
                    case DataHandlingService.TASK_COMPLETED:
                        if (mLiveListener != null) {
                            mLiveListener.onCreateThreadCompleted(msg.arg2);
                        }

                        break;

                    case DataHandlingService.REQUEST_STARTED:
                        if (mLiveListener != null) {
                            mLiveListener.onCreateThreadStarted();
                        }
                        break;


                    case DataHandlingService.REQUEST_FAILED:
                        if (mLiveListener != null) {
                            mLiveListener.onCreateThreadFailed();
                        }
                        break;
                }

                break;

            case DataHandlingService.MSG_REQUEST_LIVE_THREADS:
                if (Constants.LOGV)Log.v(LOG_TAG, "entering msg_create_thread");

                if (mLiveRefreshListener != null) {
                    mLiveRefreshListener.onRefreshComplete(msg.arg2);
                }
                break;

            case MSG_DATABASE_CLEARED:
                Toast.makeText(activity.get(),
                        "entire database cleared",
                        Toast.LENGTH_LONG).show();
                break;
            case MSG_PICTURE_TAKEN: //-1 reply in main UI
                cameraListener.onPictureTaken(1);
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
                        .setMessage("A bad userID was supplied to our server... hold on a " +
                                "moment while we make you another one.")
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
                Log.i(LOG_TAG,"404 received...");
                new AlertDialog.Builder(activity.get())
                        .setTitle("404 - not found")
                        .setMessage("You have attempted to get the replies for a thread that no" +
                                " longer exists. You should refresh.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //activity.get().sendMsgRequestLiveThreads();

                                activity.get().getContentResolver()
                                        .delete(FireFlyContentProvider
                                                        .CONTENT_URI_LIVE,
                                                null,
                                                null
                                        );

                                activity.get().getContentResolver()
                                        .delete(FireFlyContentProvider
                                                        .CONTENT_URI_REPLY_LIST,
                                                null,
                                                null
                                        );
                            }
                        })
                        .setNegativeButton("Don't tell me what to do.",
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
                        break;

                    case DataHandlingService.MSG_REQUEST_LIVE_THREADS:
                        break;

                    case DataHandlingService.MSG_REQUEST_LOCAL_POSTS:
                        break;

                    case DataHandlingService.MSG_REQUEST_REPLIES:
                        //Toast.makeText(activity.get(), "No replies
                        break;
                }

                break;


            case ERROR_CAMERA:
                Log.e(LOG_TAG,"received a camera error...");

                switch (msg.arg1) {
                    case ERROR_CAMERA_OPENING_FAILED:
                        new AlertDialog.Builder(activity.get()).setTitle("Camera Failed to Open")
                                .setMessage("We couldn't connect to your Camera. Make sure no other " +
                                        "applications are currently using the Camera. You may need" +
                                        "to restart your phone.").setNeutralButton("Ok",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                        break;
                }

                break;

        }

        if (Constants.LOGD) Log.d(LOG_TAG, "exit handleMessage");
    }
}
