package nl.melledijkstra.musicplayerclient.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by melle on 19-5-2016.
 */
public class Album implements Hydratable {

    protected String name;
    protected String remote_location;
    protected ArrayList<Song> song_list;

    public Album(JSONObject exchangeData) {
        this.Hydrate(exchangeData);
    }

    public Album() {
        // empty constructor
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            this.name = obj.getString("name");
            this.remote_location = obj.getString("location");
            JSONArray songlist = obj.getJSONArray("songlist");
            for(int i = 0; i < songlist.length(); ++i) {
                JSONObject jsonsong = songlist.getJSONObject(i);
                Song song = new Song(jsonsong);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
