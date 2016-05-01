package nl.melledijkstra.musicplayerclient;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;

/**
 * Created by melle on 28-4-2016.
 */
public class Utils {

    public enum MessageTypes {
        LIST,
    }

    private Utils() {}

    /**
     * Factory for creating JSONObject messages
     * @param cmd The type of message to create within the factory
     * @return JSONObject with all information needed for the requested message
     * @throws InvalidParameterException if MessageType is not known
     */
    public static JSONObject JSONMessageFactory(MessageTypes cmd) throws InvalidParameterException {
        try {
            switch (cmd) {
                case LIST:
                    JSONObject a = new JSONObject();
                    a.put("cmd", "LIST");
                    return a;
                default:
                    throw new InvalidParameterException("Can't create message out of "+cmd+" command");
            }
        } catch (JSONException e) {
            return null;
        }
    }

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
