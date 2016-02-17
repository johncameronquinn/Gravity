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

import java.util.UUID;

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

    private boolean VERBOSE = true;
    private final String LOG_TAG = ReportManager.class.getSimpleName();

    private MainActivity mainActivity;
    private AdapterView viewContainer;
    private final Messenger reportStatusMessenger;
    static final int REPORT_STATUS_SUCCESS = 1;
    static final int REPORT_STATUS_STARTED = 0;
    static final int REPORT_STATUS_FAILED = -1;

    private int viewContainerIndex = 0;

    private ReportStatusListener mListener;

    private UUID selectedContentID;

    /**
     * @param activity the mainactivity which contains this object
     *
     * @param container this is the container which holds the reportable views
     */
    public ReportManager(MainActivity activity, AdapterView container, ReportStatusListener listener) {
        if (VERBOSE) Log.v(LOG_TAG,"creating ReportManager...");
        mainActivity = activity;
        viewContainer = container;
        reportStatusMessenger = new Messenger(new ReportStatusHandler());
        mListener = listener;
    }

    public ReportManager(MainActivity activity, View v, ReportStatusListener listener) {
        if (VERBOSE) Log.v(LOG_TAG,"creating ReportManager...");
        mainActivity = activity;
        reportStatusMessenger = new Messenger(new ReportStatusHandler());
        mListener = listener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (VERBOSE) Log.v(LOG_TAG,"entering onItemClick...");

        if (viewContainer.indexOfChild(v) == -1) {
            Log.v(LOG_TAG,"view is not found...");
        } else {
            Log.v(LOG_TAG,"view clicked which is in provided container...");
            selectedContentID = (UUID)v.getTag(R.integer.content_id_key);

            endReportSelectionMode();

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
                                }
                            })
                    .setNegativeButton(R.string.report_dialog_negative_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mListener.onDialogClosed(false);
                                }
                            }).show();

        }

        if (VERBOSE) Log.v(LOG_TAG,"exiting onItemClick...");
    }

    public void setItemIDAndShow(UUID id) {
        if (VERBOSE) Log.v(LOG_TAG,"entering setItemIDAndShow...");
        selectedContentID = id;

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
                            }
                        })
                .setNegativeButton(R.string.report_dialog_negative_label,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mListener.onDialogClosed(false);
                            }
                        }).show();


        if (VERBOSE) Log.v(LOG_TAG,"exiting setItemIDAndShow...");
    }

    public void startReportSelectionMode() {
        if (VERBOSE) Log.v(LOG_TAG,"entering startReportSelectionMode...");

        assert viewContainer != null;

        //Block posts from being clickable
        viewContainer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        viewContainer.setOnItemClickListener(this);

        //Save container position and pull to front
        ViewGroup group = (ViewGroup)viewContainer.getParent();
        viewContainerIndex = group.indexOfChild(viewContainer);
        viewContainer.bringToFront();

        if (VERBOSE) Log.v(LOG_TAG,"exiting startReportSelectionMode...");
    }

    public void endReportSelectionMode() {
        if (VERBOSE) Log.v(LOG_TAG,"entering endReportSelectionMode...");

        viewContainer.setOnItemClickListener(null);

        viewContainer.setDescendantFocusability(
                ViewGroup.FOCUS_AFTER_DESCENDANTS
        );

        ViewGroup group = (ViewGroup)viewContainer.getParent();
        group.removeView(viewContainer);
        group.addView(viewContainer,viewContainerIndex);

        if (VERBOSE) Log.v(LOG_TAG,"exiting endReportSelectionMode...");
    }

    private void sendReport() {
        if (VERBOSE) Log.v(LOG_TAG,"entering sendReport...");

        if (selectedContentID == null) {
            throw new RuntimeException("no contentID was selected.");
        }
        mainActivity.sendMsgSendReportToServer(selectedContentID,reportStatusMessenger);

        if (VERBOSE) Log.v(LOG_TAG,"exiting sendReport...");
    }

    private class ReportStatusHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case REPORT_STATUS_FAILED:
                    mListener.onRequestError(msg.arg1);
                    if (VERBOSE) Log.v(LOG_TAG,"Request failed with error code : " + msg.arg1);
                    break;

                case REPORT_STATUS_SUCCESS:
                    mListener.onRequestSuccess();
                    if (VERBOSE) Log.v(LOG_TAG,"");
                    break;

                case REPORT_STATUS_STARTED:
                    mListener.onRequestStarted();
                    break;
            }

            super.handleMessage(msg);
        }
    }
}
