package nl.melledijkstra.musicplayerclient.Models;

import org.json.JSONObject;

/**
 * Created by melle on 19-5-2016.
 */
public interface Hydratable {

    /**
     * This method makes sure the object implementing this interface is able to hydrate itself
     * @param obj The json object to get the data from
     */
    void Hydrate(JSONObject obj);

}
