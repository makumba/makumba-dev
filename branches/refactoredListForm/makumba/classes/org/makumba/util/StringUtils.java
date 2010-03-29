package org.makumba.util;

import java.util.Collection;

/**
 * This class provides utility methods for String manipulation.
 * 
 * @author rudi
 * @version $Id$
 */
public class StringUtils {

    /** Returns a string with lower-cased first letter. */
    public static String lowerCaseBeginning(String s) {
        return String.valueOf(s.charAt(0)).toLowerCase() + s.substring(1);
    }

    /** Returns a string with upper-cased first letter. */
    public static String upperCaseBeginning(String s) {
        return String.valueOf(s.charAt(0)).toUpperCase() + s.substring(1);
    }

    /** Checks whether a String is not null and has, after trimming, a length > 0. */
    public static boolean notEmpty(String s) {
        return s != null && s.length() > 0;
    }

    /** Checks whether a String is not null and has, after trimming, a length > 0. */
    public static boolean notEmpty(Object o) {
        return o != null && o instanceof String && o.toString().length() > 0;
    }

    /**
     * Converts an array to a String represenation, using the toString() method of each array element.
     */
    public static String toString(Object[] array) {
        return toString(array, true);
    }

    public static String toString(Object[] array, boolean frame) {
        StringBuffer b = new StringBuffer();
        if (frame) {
            b.append('[');
        }
        for (int i = 0; i < array.length; i++) {
            b.append(array[i]);
            if (i < (array.length - 1)) {
                b.append(", ");
            }
        }
        if (frame) {
            b.append(']');
        }
        return b.toString();
    }

    public static String toString(Collection collection) {
        return toString((Object[]) collection.toArray(new Object[collection.size()]));
    }

    public static String concatAsString(Object[] array) {
        return concatAsString(array, "_");

    }

    public static String concatAsString(Object[] array, String delim) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            b.append(array[i]);
            if (i < (array.length - 1)) {
                b.append(delim);
            }
        }
        return b.toString();
    }

    public static boolean equals(String s, String s2) {
        return s != null && s.equals(s2);
    }

    public static boolean equals(String s, Object o) {
        return o instanceof String && equals(s, (String) o);
    }

    public static boolean equals(Object o, String s) {
        return equals(s, o);
    }

    public static boolean equals(String s, String[] options) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < options.length; i++) {
            if (s.equals(options[i])) {
                return true;
            }
        }
        return false;
    }

}