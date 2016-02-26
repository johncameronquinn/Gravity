package us.gravwith.android;

import com.amazonaws.mobile.content.ContentProgressListener;
import com.amazonaws.mobile.content.UserFileManager;

import java.io.File;

/**
 * Created by John C. Quinn on 2/25/16.
 *
 * this simple runnable uses amazon's content transfer systems to run its upload method
 */
public class UploadImageRunnable implements Runnable {

    private UploadImageMethods mTask;

    public interface UploadImageMethods extends ContentProgressListener, ServerTask.ServerTaskMethods {
        UserFileManager getFileManager();
        File getImageFile();
        void onUploadStarted();
    }

    public UploadImageRunnable(UploadImageMethods methods) {
        mTask = methods;
    }

    @Override
    public void run() {
        mTask.setTaskThread(Thread.currentThread());

        mTask.onUploadStarted();
        mTask.getFileManager().uploadContent(
                mTask.getImageFile(),
                mTask.getDataBundle().getString(Constants.KEY_S3_KEY),
                mTask
        );
    }
}
