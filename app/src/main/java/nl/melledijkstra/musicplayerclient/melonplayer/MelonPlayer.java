package nl.melledijkstra.musicplayerclient.melonplayer;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

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
    private String TAG = MelonPlayer.class.getSimpleName();

    public enum States {
        BUFFERING,
        PLAYING,
        ENDED,
        ERROR,
        NOTHINGSPECIAL,
        OPENING,
        PAUSED,
        STOPPED
    }

    private int volume = -1;

    private boolean mute = false;

    private States state = States.NOTHINGSPECIAL;

    public ArrayList<Album> albums;

    // current playing song
    @Nullable
    private Song currentSong = null;

    private float songPosition = -1f;

    private long currentTime = -1;

    @Nullable
    public Song getCurrentSong() {
        return currentSong;
    }

    public float getSongPosition() {
        return songPosition;
    }

    public long getCurrentTime() {
        return currentTime;
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

    private HashSet<StateUpdateListener> stateListeners;
    public void registerStateChangeListener(StateUpdateListener listener) {
        stateListeners.add(listener);
    }
    public void unRegisterStateChangeListener(StateUpdateListener listener) {
        stateListeners.remove(listener);
    }

    private HashSet<AlbumListUpdateListener> albumListUpdateListeners;
    public void registerAlbumListChangeListener(AlbumListUpdateListener listener) {
        albumListUpdateListeners.add(listener);
    }
    public void unRegisterAlbumListChangeListener(AlbumListUpdateListener listener) {
        albumListUpdateListeners.remove(listener);
    }

    private HashSet<SongListUpdateListener> songListUpdateListeners;
    public void registerSongListChangeListener(SongListUpdateListener listener) {
        songListUpdateListeners.add(listener);
    }
    public void unRegisterSongListChangeListener(SongListUpdateListener listener) {
        songListUpdateListeners.remove(listener);
    }

    public MelonPlayer() {
        stateListeners              = new HashSet<>();
        albumListUpdateListeners    = new HashSet<>();
        songListUpdateListeners     = new HashSet<>();

        albums = new ArrayList<>();
    }

    @Override
    public void onReceive(JSONObject obj) {
        Log.i(TAG, "Setting new state of MelonPlayer");
        try {
            // UPDATE CONTROL
            if(obj.has("control")) {
                JSONObject control = obj.getJSONObject("control");
                if(control.has("state")) {
                    switch(control.getString("state")) {
                        case "State.Playing":
                            state = States.PLAYING;
                            break;
                        case "State.Paused":
                            state = States.PAUSED;
                            break;
                        case "State.Stopped":
                            state = States.STOPPED;
                            break;
                        case "State.Opening":
                            state = States.OPENING;
                            break;
                        case "State.Ended":
                            state = States.ENDED;
                            break;
                        case "State.Buffering":
                            state = States.BUFFERING;
                            break;
                        case "State.Error":
                            state = States.ERROR;
                            break;
                        case "State.NothingSpecial":
                            state = States.NOTHINGSPECIAL;
                            break;
                        default:
                            state = States.NOTHINGSPECIAL;
                            break;
                    }
                }
                volume = control.getInt("volume");
                songPosition = BigDecimal.valueOf(control.getDouble("position")).floatValue();
                if(!control.isNull("time")) {
                    currentTime = control.getLong("time");
                } else {
                    currentTime = 0;
                }
                mute = control.getBoolean("mute");
                if(!control.isNull("current_song")) {
                    currentSong = new Song(control.getJSONObject("current_song"));
                } else {
                    currentSong = null;
                }

                Log.d(TAG, String.format(Locale.getDefault(), "volume: %d, position: %f, mute: %b, currentSong: %s",volume, songPosition, mute, currentSong));
                for(StateUpdateListener listener : stateListeners) {
                    listener.MelonPlayerStateUpdated();
                }
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
                        Log.v(TAG,"Could not add song to listview - Exception:" +e.getMessage());
                        e.printStackTrace();
                    }
                }
                for(AlbumListUpdateListener listener : albumListUpdateListeners) {
                    listener.AlbumListUpdated();
                }
            }
            // UPDATE SONGLIST
            if(obj.has("songlist") && obj.has("albumid")) {
                Log.d(TAG, "albumid from message: "+obj.getLong("albumid"));
                if(obj.getJSONArray("songlist").length() > 0) {
                    for (Album album : albums) {
                        if(album.getID() == obj.getLong("albumid")) {
                            album.fillSongList(obj.getJSONArray("songlist"));
                        }
                    }
                }
                for(SongListUpdateListener listener : songListUpdateListeners) {
                    listener.SongListUpdated();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    public interface StateUpdateListener {
        void MelonPlayerStateUpdated();
    }

    public interface AlbumListUpdateListener {
        void AlbumListUpdated();
    }

    public interface SongListUpdateListener {
        void SongListUpdated();
    }

}
