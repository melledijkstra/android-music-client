package nl.melledijkstra.musicplayerclient;

import org.json.JSONObject;

/**
 * Created by melle on 28-4-2016.
 */
public interface MessageReceiver {
    void onReceive(JSONObject json);
}
