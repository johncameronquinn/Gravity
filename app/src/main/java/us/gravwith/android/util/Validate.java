package us.gravwith.android.util;

/**
 * Created by John C. Quinn on 1/4/16.
 *
 * ripped methods from the facebook SDK. Helper methods to perform common checks in a
 * highly readable manner.
 */
public class Validate {

    public static void notNullOrEmpty(String arg, String name) {
        if (Utility.isNullOrEmpty(arg)) {
            throw new IllegalArgumentException("Argument '" + name + "' cannot be null or empty");
        }
    }
}
