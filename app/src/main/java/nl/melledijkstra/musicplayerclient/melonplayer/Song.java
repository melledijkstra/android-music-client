package nl.melledijkstra.musicplayerclient.melonplayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * <p>Song Model class that has all information about a specific song</p>
 * <p>Created by Melle Dijkstra on 14-4-2016</p>
 */
public class Song implements Hydratable {

    private long ID;
    private String title;
    private Long duration;

    public Song(JSONObject json) {
        this.Hydrate(json);
    }

    public Song(long ID, String title) {
        this.ID = ID;
        this.title = title;
        this.duration = null;
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            this.ID = obj.getLong("id");
            this.title = obj.getString("title");
            this.duration = obj.getLong("duration");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public long getID() {
        return ID;
    }

    @Override
    public String toString() {
        return title;
    }

    public Long getDuration() {
        return duration;
    }
}
