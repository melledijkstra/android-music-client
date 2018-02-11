package nl.melledijkstra.musicplayerclient;

import java.util.Locale;

/**
 * <p>Created by melle on 28-4-2016.</p>
 */
public class Utils {

    private Utils() {}

    /**
     * Maps a value between a range (out_min and out_max)
     * @param val The value to map
     * @param in_min minimum value that $val can be
     * @param in_max maximum value that $val can be
     * @param out_min minimum value that needs to be returned
     * @param out_max maximum value that needs to be returned
     * @return The mapped value
     */
    public static long Map(long val, long in_min, long in_max, long out_min, long out_max)
    {
        return (val - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    /**
     * Constrain a value to min and max
     * @param value The value to constrain
     * @param min The minimum value of the result
     * @param max The maximum value of the result
     * @return the constrained value
     */
    public static int Constrain(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * Check if a value is in in specific range
     * @param value Value to check in range
     * @param min Minimum number in range
     * @param max Maximum number in range
     * @return If the value is inside the range
     */
    public static boolean inRange(int value, int min, int max) {
        return (value>= min) && (value<= max);
    }

    public static String millisecondsToDurationFormat(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        return String.format(Locale.getDefault(),"%02d:%02d:%02d", hour, minute, second);
    }

    public static String secondsToDurationFormat(long seconds) {
        long second = seconds % 60;
        long minute = (seconds / 60) % 60;
        long hour = (seconds / (60 * 60)) % 24;

        return String.format(Locale.getDefault(),"%02d:%02d:%02d", hour, minute, second);
    }
}
