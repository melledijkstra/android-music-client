package nl.melledijkstra.musicplayerclient.melonplayer;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import nl.melledijkstra.musicplayerclient.grpc.Album;
import nl.melledijkstra.musicplayerclient.grpc.Song;

/**
 * <p>Created by melle on 19-5-2016.</p>
 */
public class AlbumModel implements Protoble<Album> {

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
    private ArrayList<SongModel> songModelList;

    /**
     * Check if this is a favorite album
     */
    private boolean favorite;

    public AlbumModel(Album exchangeData) {
        this.songModelList = new ArrayList<>();
        this.Hydrate(exchangeData);
    }

    public AlbumModel(long id, String title, @Nullable Bitmap cover, boolean favorite) {
        this(id, title, favorite);
        this.cover = cover;
        this.favorite = favorite;
    }

    public AlbumModel(long id, String title, boolean favorite) {
        this.ID = id;
        this.title = title;
        this.favorite = favorite;
        this.cover = null;
        this.songModelList = new ArrayList<>();
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

    public ArrayList<SongModel> getSongList() {
        return songModelList;
    }

    public void fillSongList(List<Song> songlist) {
        songModelList.clear();
        for (Song song : songlist) {
            songModelList.add(new SongModel(song));
        }
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public void Hydrate(Album obj) {
        ID = obj.getId();
        title = obj.getTitle();
        // TODO: implement favorite in proto file
        favorite = false;
        fillSongList(obj.getSongListList());
    }
}
