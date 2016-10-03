package nl.melledijkstra.musicplayerclient.models;

import org.json.JSONObject;

/**
 * <p>Created by melle on 19-5-2016.</p>
 */
public interface Hydratable {

    /**
     * This method makes sure the object implementing this interface is able to hydrate itself
     * @param obj The json object to get the data from
     */
    void Hydrate(JSONObject obj);

}
