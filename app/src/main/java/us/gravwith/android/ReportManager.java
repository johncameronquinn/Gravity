package us.gravwith.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * Created by John C. Quinn on 1/6/16.
 *
 *
 */
public class ReportManager implements AdapterView.OnItemClickListener {

    interface ReportStatusListener {
        void onRequestSuccess();
        void onRequestError(int code);
        void onRequestStarted();
        void onDialogClosed(boolean didTheyHitYes);
    }

    private MainActivity mainActivity;
    private AdapterView viewContainer;
    private final Messenger reportStatusMessenger;
    static final int REPORT_STATUS_SUCCESS = 1;
    static final int REPORT_STATUS_STARTED = 0;
    static final int REPORT_STATUS_FAILED = -1;

    private ReportStatusListener mListener;

    private int selectedContentID;

    private final String LOG_TAG = ReportManager.class.getSimpleName();

    private final int CONTENT_ID_KEY;

    /**
     * @param activity the mainactivity which contains this object
     *
     * @param container this is the container which holds the reportable views
     */
    public ReportManager(MainActivity activity, AdapterView container, ReportStatusListener listener) {
        mainActivity = activity;
        viewContainer = container;
        reportStatusMessenger = new Messenger(new ReportStatusHandler());
        mListener = listener;
        CONTENT_ID_KEY = activity.getResources().getInteger(R.integer.content_id_key);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (viewContainer.indexOfChild(v) == -1) {
            Log.v(LOG_TAG,"view is not found...");
        } else {
            Log.v(LOG_TAG,"view clicked which is in provided container...");
            selectedContentID = (int)v.getTag(CONTENT_ID_KEY);

            new AlertDialog.Builder(mainActivity)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.report_dialog_title)
                    .setMessage(R.string.report_dialog_message)
                    .setPositiveButton(R.string.report_dialog_positive_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sendReport();
                                    dialog.dismiss();
                                    mListener.onDialogClosed(true);
                                    viewContainer.setDescendantFocusability(
                                            ViewGroup.FOCUS_AFTER_DESCENDANTS
                                    );
                                    viewContainer.setOnClickListener(null);
                                }
                            })
                    .setNegativeButton(R.string.report_dialog_negative_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mListener.onDialogClosed(false);
                                    viewContainer.setDescendantFocusability(
                                            ViewGroup.FOCUS_AFTER_DESCENDANTS
                                    );
                                }
                            }).show();

        }

    }

    public void startReportSelection() {
        viewContainer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        viewContainer.setOnItemClickListener(this);
    }

    private void sendReport() {
        if (selectedContentID == 0) {
            throw new RuntimeException("no contentID was selected.");
        }
        mainActivity.sendMsgSendReportToServer(selectedContentID,reportStatusMessenger);
    }

    private class ReportStatusHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case REPORT_STATUS_FAILED:
                    mListener.onRequestError(msg.arg1);
                    break;

                case REPORT_STATUS_SUCCESS:
                    mListener.onRequestSuccess();
                    break;

                case REPORT_STATUS_STARTED:
                    mListener.onRequestStarted();
                    break;
            }

            super.handleMessage(msg);
        }
    }
}
