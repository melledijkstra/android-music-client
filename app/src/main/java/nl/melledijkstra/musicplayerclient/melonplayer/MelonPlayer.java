package nl.melledijkstra.musicplayerclient.melonplayer;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MessageReceiver;

/**
 * <p>
 *     This class represents the Model object for the music player.
 *     It's knows the state of the python music player because it makes use of the MusicPlayerConnection class
 * </p>
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MelonPlayer implements MessageReceiver {

    // CONSTANTS
    public static final String DEFAULT_IP = "192.168.1.200";
    public static final int DEFAULT_PORT = 1010;
    public static final int DEFAULT_TIMEOUT = 5000;

    public enum States {
        BUFFERING,
        PLAYING,
        ENDED,
        ERROR,
        IDLE,
        OPENING,
        PAUSED,
        STOPPED
    }

    private int volume;

    private boolean mute;

    private States state;

    public ArrayList<Album> albums;

    // current playing song
    @Nullable
    private Song currentSong;

    private float songPosition;

    @Nullable
    public Song getCurrentSong() {
        return currentSong;
    }

    public float getSongPosition() {
        return songPosition;
    }

    public States getState() {
        return state;
    }

    public int getVolume() {
        return volume;
    }

    public boolean getMute() {
        return mute;
    }

    private ArrayList<MelonPlayerListener> listeners;

    public MelonPlayer() {
        listeners = new ArrayList<>();
        albums = new ArrayList<>();
    }

    @Override
    public void onReceive(JSONObject obj) {
        Log.i(App.TAG, "Setting new state of MelonPlayer");
        try {
            // UPDATE CONTROL
            if(obj.has("control")) {
                JSONObject control = obj.getJSONObject("control");
                volume = control.getInt("volume");
                songPosition = BigDecimal.valueOf(control.getDouble("position")).floatValue();
                mute = control.getBoolean("mute");
                currentSong = findSongByID(control.getLong("current_song"));
            }
            // UPDATE ALBUMLIST
            if(obj.has("albumlist") && obj.getJSONArray("albumlist") != null) {
                JSONArray albumlist = obj.getJSONArray("albumlist");
                albums.clear();
                for(int i = 0; i < albumlist.length();i++) {
                    try {
                        Album album = new Album(albumlist.getJSONObject(i));
                        albums.add(album);
                    } catch (JSONException e) {
                        Log.v(App.TAG,"Could not add song to listview - Exception:" +e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            // UPDATE SONGLIST
            if(obj.has("songlist") && obj.has("albumid") && obj.getJSONArray("songlist").length() > 0) {
                for (Album album : albums) {
                    if(album.getID() == obj.getLong("albumid")) {
                        album.fillSongList(obj.getJSONArray("songlist"));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Let every listener know that data changed in MelonPlayer instance
        for(MelonPlayerListener listener : listeners) {
            listener.melonPlayerUpdated();
        }
    }

    public void registerListener(MelonPlayerListener listener) {
        listeners.add(listener);
    }

    public Album findAlbum(long id) {
        for (Album album : albums) {
            if (album.getID() == id) {
                return album;
            }
        }
        return null;
    }

    @Nullable
    private Song findSongByID(long songid) {
        for(Album album: albums) {
            for (Song song : album.getSongList()) {
                if(song.getID() == songid) return song;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[state: %s, volume: %d, albumcount: %d]",state, volume, albums.size());
    }

}
