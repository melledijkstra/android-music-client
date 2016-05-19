package nl.melledijkstra.musicplayerclient.entities;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Song Model class that has all information about a specific song</p>
 * <p>Created by Melle Dijkstra on 14-4-2016</p>
 */
public class Song implements Hydratable {

    protected String name;
    protected String remote_location;

    public Song(JSONObject json) {
        this.Hydrate(json);
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            this.name = obj.getString("name");
            this.remote_location = obj.getString("file");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
