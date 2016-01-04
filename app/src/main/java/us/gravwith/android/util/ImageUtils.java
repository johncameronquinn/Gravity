package us.gravwith.android.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;

//import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
//import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
///import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ev0x on 11/14/15.
 */
public final class ImageUtils {

    private final String TAG = "ImageUtils";
    private final boolean VERBOSE = false;

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
    public static void copyFile(File sourceFile, File destFile) throws IOException {
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


    /**
     * method 'saveIncomingImage'
     *
     * @param destination the location uri for the image to be stored
     * @param theImage    the base64'd stringified image
     * @return whether or not the save was successful
     */
    public boolean saveIncomingImage(Uri destination, String theImage, ContentResolver resolver) {
        if (VERBOSE) Log.v(TAG, "Saving incoming to uri" + destination.toString());

        try {
            FileOutputStream fos = (FileOutputStream) resolver
                    .openOutputStream(destination, "w");
            if (VERBOSE) {
                Log.v(TAG, "saving image to file " + fos.getFD());
            }

            byte[] data = theImage.getBytes();
            ByteArrayInputStream byis = new ByteArrayInputStream(data);
            Base64InputStream bos = new Base64InputStream(byis, Base64.DEFAULT);
         //   GzipCompressorInputStream gzis = new GzipCompressorInputStream(bos);
      //      IOUtils.copy(gzis, fos);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            Log.e(TAG, "error saving incoming image to internal storage...", e);
            return false;

        }

        if (VERBOSE) Log.v(TAG, "image successfully saved");
        return true;
    }


    /**
     * method 'saveIncomingImage'
     *
     * @param fileName file name for the image to be saved to
     * @param theImage the base64'd stringified image
     * @return whether or not the save was successful
     */
    public boolean saveIncomingImage(String fileName, String theImage, Activity activity) {
        if (VERBOSE) Log.v(TAG, "Saving incoming with file name: " + fileName);

        try {
            FileOutputStream fos = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
            if (VERBOSE) {
                Log.v(TAG, "saving image to file " + fos.getFD());
            }

            byte[] data = theImage.getBytes();
            ByteArrayInputStream byis = new ByteArrayInputStream(data);
            Base64InputStream bos = new Base64InputStream(byis, Base64.DEFAULT);
        //    GzipCompressorInputStream gzis = new GzipCompressorInputStream(bos);
       //     IOUtils.copy(gzis, fos);
            fos.flush();
            fos.close();

            if (VERBOSE) Log.v(TAG, "validating image...");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);
            if (options.outWidth == -1 && options.outHeight == -1) {
                Log.e(TAG, "incoming image was not valid...");
                activity.deleteFile(fileName);
                return false;
            }

        } catch (IOException e) {
            Log.e(TAG, "error saving incoming image to internal storage...", e);
            return false;

        }

        if (VERBOSE) Log.v(TAG, "image successfully saved");
        return true;
    }


    /**
     * method 'loadImageForTransit'
     * <p/>
     * loads bitmap at selected filePath from storage, gzips and base64's it, so that it may be
     * sent as part of a JSON object
     *
     * @param filePath location of the image to load
     * @return processed image ready to be sent to the server
     */
    public String loadImageForTransit(String filePath) {
        if (VERBOSE) {
            Log.v(TAG, "loading image for transit from filePath " + filePath);
        }
        String image = null;
        try {
            //create the data into an input stream
            InputStream is = new FileInputStream(filePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            //GzipCompressorOutputStream gos = new GzipCompressorOutputStream(bos);


            //byte[] readData = IOUtils.toByteArray(is);

            //gos.write(readData);
            //gos.flush();
            //gos.close();

            image = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);

        } catch (IOException e) {
            Log.e(TAG, "file was not found", e);
        }
        return image;
    }


    /**
     * Create a File for saving an image or video
     */
    public File getInternalImageFile(Activity activity) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        File file = new File(activity.getFilesDir(), File.separator +
                "IMG_" + timeStamp + ".jpg");


        return file;
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public static String getPath(Activity activity, Uri uri) {

        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(uri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        if (res != null) {
            return res;
        } else {
            // this is our fallback here
            return uri.getPath();
        }


    }
}
