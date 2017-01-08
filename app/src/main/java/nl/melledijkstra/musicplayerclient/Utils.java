package nl.melledijkstra.musicplayerclient;

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

}
