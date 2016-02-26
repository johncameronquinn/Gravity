package us.gravwith.android;

import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.content.ContentItem;
import com.amazonaws.mobile.content.UserFileManager;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by John Quinn on 11/9/15.
 *
 * Attempts to send a particular thread to the server, and request
 */
class SendLivePostTask extends ServerTask implements SendLivePostRunnable.LivePostMethods,
    RequestLiveThreadsRunnable.ThreadRequestMethods, CreateGCMTopicRunnable.CreateGCMTopicMethods,
        UploadImageRunnable.UploadImageMethods {

    private Runnable mRequestRunnable;
    private Runnable mResponseRunnable;
    private Runnable mOtherRunnable;
    private Runnable mUploadRunnable;
    private String topicARN;
    private File imageFile;
    private WeakReference<UserFileManager> mFileManagerRef;

    private boolean VERBOSE = true;
    private final String TAG = "SendLivePostTask";
    private final String urlString = "/live/upload/";

    public SendLivePostTask() {
        mServerConnectRunnable = new ServerConnectRunnable(this);
        mResponseRunnable = new RequestLiveThreadsRunnable(this);
        mRequestRunnable = new SendLivePostRunnable(this);
        mOtherRunnable = new CreateGCMTopicRunnable(this);
        mUploadRunnable = new UploadImageRunnable(this);
    }

    public void setTopicARN(String arn) {
        this.topicARN = arn;
    }

    public void setFileManager(UserFileManager fileUploader) {
        mFileManagerRef = new WeakReference<>(fileUploader);
    }

    @Override
    public UserFileManager getFileManager() {
        return mFileManagerRef.get();
    }

    public void setImageFile(File fileToSend) {
        imageFile = fileToSend;
    }

    public File getImageFile() {
        return imageFile;
    }

    public String getTopicARN() {
        return topicARN;
    }

    @Override
    public Runnable getServerConnectRunnable() {
        return mServerConnectRunnable;
    }

    @Override
    public Runnable getRequestRunnable() {
        return mRequestRunnable;
    }

    @Override
    public Runnable getResponseRunnable() {
        return mResponseRunnable;
    }

    @Override
    public Runnable getOtherRunnable() {
        return mOtherRunnable;
    }

    @Override
    public Runnable getUploadRunnable() {
        return mUploadRunnable;
    }

    @Override
    public String getURLPath() {
        return urlString;
    }

    @Override
    public void handleServerConnectState(int state) {
        int outState = -10;
        switch (state) {
            case ServerConnectRunnable.CONNECT_STATE_FAILED:
                Log.d(TAG, "server connection failed...");
                outState = DataHandlingService.CONNECTION_FAILED;
                break;

            case ServerConnectRunnable.CONNECT_STATE_STARTED:
                outState = DataHandlingService.CONNECTION_STARTED;
                Log.d(TAG,"server connection started...");
                break;

            case ServerConnectRunnable.CONNECT_STATE_COMPLETED:
                outState = DataHandlingService.CONNECTION_COMPLETED;
                Log.d(TAG, "successfully connected to server :3");
                break;

        }

        handleDownloadState(outState, this);
    }

    @Override
    public void handleCreateGCMTopicState(int state) {
        int outState = -1;

        switch (state) {
            case CreateGCMTopicRunnable.CREATE_TOPIC_FAILED:
                Log.d(TAG, "create live topic failed...");
                outState = DataHandlingService.TOPIC_CREATION_FAILED;
                break;

            case CreateGCMTopicRunnable.CREATE_TOPIC_STARTED:
                Log.d(TAG, "create live topic started...");
                outState = DataHandlingService.TOPIC_CREATION_STARTED;
                break;

            case CreateGCMTopicRunnable.CREATE_TOPIC_SUCCESS:
                Log.d(TAG, "create live topic success...");
                outState = DataHandlingService.TOPIC_CREATED;
                break;
        }

        handleDownloadState(outState, this);
    }

    public void handleLivePostState(int state) {
        int outState = -1;

        switch (state) {
            case SendLivePostRunnable.REQUEST_FAILED:
                Log.d(TAG, "send live post failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case SendLivePostRunnable.REQUEST_STARTED:
                Log.d(TAG, "send live post started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case SendLivePostRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "send live post success...");
                outState = DataHandlingService.REQUEST_COMPLETED;
                break;
        }

        handleDownloadState(outState, this);
    }

    public void handleThreadsRequestState(int state) {
        int outState = -1;

        switch (state) {
            case RequestLocalRunnable.REQUEST_FAILED:
                Log.d(TAG, "request threads failed...");
                outState = DataHandlingService.REQUEST_FAILED;
                break;

            case RequestLocalRunnable.REQUEST_STARTED:
                Log.d(TAG,"request threads started...");
                outState = DataHandlingService.REQUEST_STARTED;
                break;

            case RequestLocalRunnable.REQUEST_SUCCESS:
                Log.d(TAG, "request threads success...");
                outState = DataHandlingService.TASK_COMPLETED;
                break;
        }
        handleDownloadState(outState,this);
    }

    @Override
    public void onSuccess(ContentItem contentItem) {
        handleDownloadState(DataHandlingService.IMAGE_UPLOAD_COMPLETED,this);
    }

    @Override
    public void onProgressUpdate(String filePath, boolean isWaiting, long bytesCurrent, long bytesTotal) {
        //todo pass this
    }

    @Override
    public void onError(String filePath, Exception ex) {
        Log.e(TAG,"Error occurred for filepath " + filePath,ex);
        handleDownloadState(DataHandlingService.IMAGE_UPLOAD_FAILED,this);
    }

    public void onUploadStarted() {
        handleDownloadState(DataHandlingService.IMAGE_UPLOAD_STARTED,this);
    }

    public void recycle() {
/*        if (mFileManagerRef != null) {
            mFileManagerRef.clear();
        }*/
        imageFile = null;
        super.recycle();
    }
}
