package nl.melledijkstra.musicplayerclient.models;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * <p>Created by melle on 19-5-2016.</p>
 */
public class Album implements Hydratable {

    private String title;
    private Bitmap cover;
    // TODO: do we need this?
    private String remote_location;
    private ArrayList<Song> song_list;

    public Album(JSONObject exchangeData) {
        this.cover = null;
        this.remote_location = null;
        this.song_list = new ArrayList<>();
        this.Hydrate(exchangeData);
    }

    public Album(String title, Bitmap cover) {
        this.title = title;
        this.cover = cover;
    }

    // TODO: remove this nonsense
    public Album(String title) {
        this.title = title;
        this.cover = null;
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            this.title = obj.getString("title");
            this.remote_location = obj.getString("location");
            JSONArray songlist = obj.getJSONArray("songlist");
            for(int i = 0; i < songlist.length(); ++i) {
                JSONObject jsonsong = songlist.getJSONObject(i);
                Song song = new Song(jsonsong);
                song_list.add(song);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public Bitmap getCover() {
        return cover;
    }
}
