package nl.melledijkstra.musicplayerclient.melonplayer;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * <p>Created by melle on 19-5-2016.</p>
 */
public class Album implements Hydratable {

    /**
     * Identifier of an album
     */
    private long ID;

    /**
     * Title of album
     */
    private String title;

    /**
     * The cover of the album
     */
    @Nullable
    private Bitmap cover;

    /**
     * The list of songs that this album contains
     */
    private ArrayList<Song> songList;

    /**
     * Check if this is a favorite album
     */
    private boolean favorite;

    public Album(JSONObject exchangeData) {
        this.songList = new ArrayList<>();
        this.Hydrate(exchangeData);
    }

    public Album(long id, String title, @Nullable Bitmap cover, boolean favorite) {
        this(id, title, favorite);
        this.favorite = favorite;
    }

    public Album(long id, String title, boolean favorite) {
        this.ID = id;
        this.title = title;
        this.favorite = favorite;
        this.cover = null;
        this.songList = new ArrayList<>();
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            if(obj.has("id")) {
                this.ID = obj.getLong("id");
            }
            if(obj.has("title")) {
                this.title = obj.getString("title");
            }
            if(obj.has("songlist")) {
                fillSongList(obj.getJSONArray("songlist"));
            }
            if(obj.has("favorite")) {
                this.favorite = obj.getBoolean("favorite");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    @Nullable
    public Bitmap getCover() {
        return cover;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public long getID() {
        return ID;
    }

    public ArrayList<Song> getSongList() {
        return songList;
    }

    public void fillSongList(JSONArray songlist) throws JSONException {
        songList.clear();
        for(int i = 0; i < songlist.length(); ++i) {
            JSONObject jsonsong = songlist.getJSONObject(i);
            Song song = new Song(jsonsong);
            songList.add(song);
        }
    }

    @Override
    public String toString() {
        return title;
    }

}
