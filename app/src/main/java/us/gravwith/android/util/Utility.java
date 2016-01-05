package us.gravwith.android.util;

import android.os.Bundle;

import com.fasterxml.jackson.core.JsonGenerator;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
}
