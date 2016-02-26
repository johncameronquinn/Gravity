package us.gravwith.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonGenerator;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    public static String getRelativeTimeStringFromLong(int inSeconds) {

        String unitchar = "d";
        Long out = 0L;

        Long timePassed = System.currentTimeMillis() - inSeconds*1000L;

        out = TimeUnit.MILLISECONDS.toSeconds(timePassed);

        //return now if the time is negative
        if (out < 0) {
            return "now";
        }

        if (out > 59) {
            out = TimeUnit.MILLISECONDS.toMinutes(timePassed);
            if (out > 59) {
                out = TimeUnit.MILLISECONDS.toHours(timePassed);
                if (out > 23) {
                    out = TimeUnit.MILLISECONDS.toDays(timePassed);
                } else {
                    unitchar = "h";
                }
            } else {
                unitchar = "m";
            }
        } else  {
            unitchar = "s";
        }

        return out + unitchar;
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

    public static void clearTextAndFocus(TextView tv) {
        tv.setText("");
        tv.clearFocus();
    }

    public static UUID getUUIDfromStringWithoutHyphens(String instring) {
        // -----|  Without Hyphens  |----------------------
        String hexStringWithoutHyphens = instring;
        // Use regex to format the hex string by inserting hyphens in the canonical format: 8-4-4-4-12
        String hexStringWithInsertedHyphens = hexStringWithoutHyphens.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
        return java.util.UUID.fromString(hexStringWithInsertedHyphens);
    }

    public static String dehyphenUUID(UUID uuid) {
        return uuid.toString().replaceAll("-","");
    }


    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
