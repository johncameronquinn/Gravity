package com.jokrapp.android.util;

import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;

/**
 * Created by John C. Quinn on 10/14/15.
 *
 * Utility file for simplified printing of common outputs
 */
public final class LogUtils {

    public static void printBundle(Bundle bundle,String TAG) {
        Log.v(TAG,"printing bundle...");

        if (bundle == null) {
            Log.d(TAG, "bundle was null...");
            Log.v(TAG,"done printing bundle.");
        } else {

            for (String key : bundle.keySet()) {
                Log.v(TAG, key + " = \"" + bundle.get(key) + "\"");
            }
        }

        Log.v(TAG,"done printing bundle.");
    }

    public static void printMapToVerbose(Map<String, Object> toPrint, String TAG) {
        Log.d(TAG, "printing map...");
        for (Map.Entry<String, Object> entry : toPrint.entrySet()) {
            Log.v(TAG, ":  " + entry.getKey() + ":  " + String.valueOf(entry.getValue()));
        }
    }

    /***********************************************************************************************
     *
     *  IMAGE STORAGE
     *
     */
    /**
     * internal file filter for deleting all stored image files
     */
    public class ImageFileFilter implements FileFilter {
        private final String[] okFileExtensions =
                new String[]{"jpg", "bmp", "png"};

        public boolean accept(File file) {
            for (String extension : okFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * copies content from source file to destination file
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }

    }
}
