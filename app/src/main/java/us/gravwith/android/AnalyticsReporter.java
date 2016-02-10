package us.gravwith.android;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;

/**
 * Created by John C. Quinn on 2/6/16.
 */
public class AnalyticsReporter {

    /* RAW ACTIONS */
    static final String ANALYTICS_ACTION_BUTTON_PRESS = "button_press";
    static final String ANALYTICS_ACTION_SWIPE = "swipe";
    static final String ANALYTICS_ACTION_VIEW = "view";
    static final String ANALYTICS_ACTION_HIDE = "hide";

    static final String VALUE_CAPTURE_REPLY = "capture_image_for_reply";
    static final String VALUE_CAPTURE_LIVE = "capture_image_for_live";
    static final String VALUE_CAPTURE_MESSAGE = "capture_image_for_message";

    public String getButtonResourceID(int viewID) {
        String out;

        switch (viewID) {
            case R.id.button_camera_capture:
                out = "0";
                break;

            case R.id.button_camera_switch:
                out =  "1";
                break;

            case R.id.button_camera_flash:
                out =  "2";
                break;

            case R.id.button_camera_live:
                out =  "3";
                break;

            case R.id.button_camera_cancel:
                out =  "4";
                break;

            case R.id.button_live_refresh:
                out = "5";
                break;

            case R.id.button_live_report:
                out = "6";
                break;

        /*    case R.id.button_live_hide:
                out = "7";
                break;*/

            case R.id.button_reply_capture:
                out = "8";
                break;

            case R.id.button_reply_refresh:
                out = "9";
                break;

            case R.id.button_reply_report:
                out = "10";
                break;

            case R.id.button_reply_send:
                out = "11";
                break;

            case R.id.photoView:
                out = "12";
                break;

            default:
                throw new RuntimeException("Invalid View ID");
        }

        return out;
    }

    public void ReportClickEvent(View clickedView) {
        if (clickedView instanceof PhotoView) {
            ReportBehaviorEvent(ANALYTICS_ACTION_BUTTON_PRESS,
                    getButtonResourceID(clickedView.getId())
            ,((PhotoView) clickedView).getImageKey());
        } else {
            ReportBehaviorEvent(ANALYTICS_ACTION_BUTTON_PRESS, getButtonResourceID(clickedView.getId()));
        }
    }

    public String getSwipeResourceID(int viewID) {
        String out;

        switch (viewID) {
            case R.id.layout_message_root:
                out = "13";
                break;

            case R.id.layout_local_root:
                out = "14";
                break;

            case R.id.layout_camera_root:
                out = "15";
                break;

            case R.id.layout_live_root:
                out = "16";
                break;

            case R.id.layout_reply_root:
                out = "17";
                break;

            default:
                throw new RuntimeException("Invalid View ID");
        }

        return out;
    }

    private final String TAG = AnalyticsReporter.class.getSimpleName();

    private AnalyticsReportingCallbacks mListener;

    interface AnalyticsReportingCallbacks {
        boolean sendMessage(Message msg);
    }

    public AnalyticsReporter(AnalyticsReportingCallbacks methods) {
        mListener = methods;
    }

    public static AnalyticsReporter getAnalyticsReporter(MainActivity activity) {
        return activity.getAnalyticsReporter();
    }

    /**
     * method 'ReportBehaviorEvent'
     *
     * this method operates as an easy to use interface between
     *
     * @param action the action that is being performed
     * @param resource the resource which on which the action is being done
     *
     *                 WHO
     *                 WHEN
     *                 ACTION
     *                 RESOURCE
     */
    public void ReportBehaviorEvent(String action, String resource) {
        if (Constants.ANALYTICSV) {
            Log.v(TAG, "entering ReportBehaviorEvent with : " + action + " | " + resource);
        }

        final String ANALYTICS_CATEGORY = "behavior";

        Bundle b = new Bundle();
        b.putString(Constants.KEY_ANALYTICS_CATEGORY,ANALYTICS_CATEGORY);
        b.putString(Constants.KEY_ANALYTICS_ACTION,action);
        b.putString(Constants.KEY_ANALYTICS_RESOURCE,resource);
        sendMsgReportBehaviorEvent(b);

        if (Constants.ANALYTICSV) {
            Log.v(TAG, "exiting ReportBehaviorEvent...");
        }
    }

    public void ReportViewEvent(PhotoView photoView) {
        ReportBehaviorEvent(ANALYTICS_ACTION_VIEW, photoView.getImageKey());
    }

    public void ReportViewEvent(LiveThreadFragment f) {
        ReportBehaviorEvent(ANALYTICS_ACTION_VIEW, f.getThreadID());
    }

    public void ReportViewEvent(String photoKey) {
        ReportBehaviorEvent(ANALYTICS_ACTION_VIEW, photoKey);
    }

    public void ReportHideEvent(String photoKey) {
        ReportBehaviorEvent(ANALYTICS_ACTION_HIDE, photoKey);
    }

    public void ReportBehaviorEvent(String action, String resource, String extra) {
        if (Constants.ANALYTICSV) {
            Log.v(TAG, "entering ReportBehaviorEvent with : " + action + " | " + resource + " | " + extra);
        }

        final String ANALYTICS_CATEGORY = "behavior";

        Bundle b = new Bundle();
        b.putString(Constants.KEY_ANALYTICS_CATEGORY,ANALYTICS_CATEGORY);
        b.putString(Constants.KEY_ANALYTICS_ACTION,action);
        b.putString(Constants.KEY_ANALYTICS_RESOURCE,resource);
        b.putString(Constants.KEY_ANALYTICS_VALUE,extra);
        sendMsgReportBehaviorEvent(b);

        if (Constants.ANALYTICSV) {
            Log.v(TAG, "exiting ReportBehaviorEvent...");
        }
    }

    public void ReportErrorEvent(Exception e) {

    }

/***************************************************************************************************
 * ANALYTICS SUBMISSION METHODS
 */

    private void sendMsgReportBehaviorEvent(Bundle b) {
        if (Constants.ANALYTICSV) Log.d(TAG,"sending message to report analytics event");
        Message msg = Message.obtain(null, DataHandlingService.MSG_REPORT_ANALYTICS);
        msg.setData(b);

        mListener.sendMessage(msg);
    }

    private void sendMsgReportTimingEvent(Bundle b) {
        if (Constants.ANALYTICSV) Log.d(TAG, "sending message to report timing event");
        Message msg = Message.obtain(null, DataHandlingService.MSG_REPORT_ANALYTIC_TIMING);
        msg.setData(b);

        mListener.sendMessage(msg);
    }

    private void sendMsgReportError(Bundle b) {
        if (Constants.ANALYTICSV) Log.d(TAG, "sending message to report timing event");
        Message msg = Message.obtain(null, DataHandlingService.MSG_REPORT_ANALYTIC_ERROR);
        msg.setData(b);

        mListener.sendMessage(msg);
    }
}
