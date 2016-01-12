package us.gravwith.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.fasterxml.jackson.core.JsonGenerator;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import us.gravwith.android.R;

/**
 * Created by John C. Quinn on 1/4/16.
 *
 * Class which performs common functions, in a manner which is easily readable
 */
public class Utility {
    public static boolean isNullOrEmpty(String s) {
        return (s == null) || (s.length() == 0);
    }

    public static Date getBundleLongAsDate(Bundle bundle, String key, Date dateBase) {
        if (bundle == null) {
            return null;
        }

        long secondsFromBase = Long.MIN_VALUE;

        Object secondsObject = bundle.get(key);
        if (secondsObject instanceof Long) {
            secondsFromBase = (Long) secondsObject;
        } else if (secondsObject instanceof String) {
            try {
                secondsFromBase = Long.parseLong((String) secondsObject);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        if (secondsFromBase == 0) {
            return new Date(Long.MAX_VALUE);
        } else {
            return new Date(dateBase.getTime() + (secondsFromBase * 1000L));
        }
    }

    public static String getDateStringFromLong(Long inSeconds) {
        Date date = new Date(inSeconds*1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d   h:mm a", Locale.getDefault()); // the format of your date
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }

    public static void writeBundleAsJsonStringObject(Bundle inbundle, JsonGenerator jGen)
            throws IOException {
        jGen.writeStartObject();
        for (String key : inbundle.keySet()) {
            jGen.writeStringField(key,inbundle.getString(key,""));
        }
        jGen.writeEndObject();
    }

    public static List<String> jsonArrayToStringList(JSONArray jsonArray) throws JSONException {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(jsonArray.getString(i));
        }

        return result;
    }

    public static void hideViewsInArray(int[] idList, View parentView) {
        for (int i : idList) {
            parentView.findViewById(i).setVisibility(View.GONE);
        }
    }

    public static void showViewsInArray(int[] idList, View parentView) {
        for (int i : idList) {
            parentView.findViewById(i).setVisibility(View.VISIBLE);
        }
    }

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
            dialog.setTitle(R.string.gps_network_not_enabled_title);
            dialog.setMessage(R.string.gps_network_not_enabled_message);
            dialog.setPositiveButton(
                    R.string.open_location_settings, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            context.startActivity(myIntent);
                        }
                    });
            dialog.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    paramDialogInterface.dismiss();
                }
            });
            dialog.show();

        }
    }
}
