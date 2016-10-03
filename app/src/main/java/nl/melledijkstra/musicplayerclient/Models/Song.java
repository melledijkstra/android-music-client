package nl.melledijkstra.musicplayerclient.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Song Model class that has all information about a specific song</p>
 * <p>Created by Melle Dijkstra on 14-4-2016</p>
 */
public class Song implements Hydratable {

    private String name;
    private String remote_location;

    public Song(JSONObject json) {
        this.Hydrate(json);
    }

    public Song(String name) {
        this.name = name;
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            this.name = obj.getString("title");
            this.remote_location = obj.getString("file");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }
}
