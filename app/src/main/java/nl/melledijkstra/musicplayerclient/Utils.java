package nl.melledijkstra.musicplayerclient;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;

/**
 * <p>Created by melle on 28-4-2016.</p>
 */
public class Utils {

    public enum MessageTypes {
        ALBUMLIST,
        SONGLIST
    }

    private Utils() {}

    /**
     * Factory for creating JSONObject messages
     * @param cmd The type of message to create within the factory
     * @return JSONObject with all information needed for the requested message
     * @throws InvalidParameterException if MessageType is not known
     */
    public static JSONObject generateJSONMessage(MessageTypes cmd) throws InvalidParameterException {
        try {
            JSONObject root = new JSONObject();
            JSONObject mplayer = new JSONObject();
            switch (cmd) {
                case ALBUMLIST:
                    root.put("cmd", "mplayer");
                    mplayer.put("cmd", "albumlist");
                    root.put("mplayer", mplayer);
                    break;
                case SONGLIST:
                    root.put("cmd", "list");
                    break;
                default:
                    throw new InvalidParameterException("Can't create message from "+cmd+" command");
            }
            return root;
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
