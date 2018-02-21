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

import nl.melledijkstra.musicplayerclient.grpc.MMPStatus;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 * <p>
 * This class represents the Model object for the music player.
 * It's stores and updates the state of the music player server
 * </p>
 */
public class MelonPlayer {

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

    public ArrayList<AlbumModel> albumModels;

    // current playing song
    @Nullable
    private SongModel currentSongModel = null;

    private float songPosition = -1f;

    private long currentTime = -1;

    @Nullable
    public SongModel getCurrentSongModel() {
        return currentSongModel;
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

    public MelonPlayer() {
        stateListeners = new HashSet<>();

        albumModels = new ArrayList<>();
    }

    public void setState(MMPStatus status) {
        Log.v(TAG, "Setting new state of MelonPlayer");
        currentSongModel = new SongModel(status.getCurrentSong());
        try {
            // Check if enum with same name exists for MelonPlayer.States, this should be the same
            state = States.valueOf(status.getState().toString());
        } catch (IllegalArgumentException e) {
            // otherwise this exception is thrown and a default value is given to current state
            state = States.NOTHINGSPECIAL;
        }
        volume = status.getVolume();
        songPosition = status.getPosition();
        // currentTime = status.getCurrentTime();
        mute = status.getMute();

        Log.d(TAG, String.format(Locale.getDefault(), "%s '%s' volume: %d, position: %f, mute: %b", state, currentSongModel, volume, songPosition, mute));
        for (StateUpdateListener listener : stateListeners) {
            listener.MelonPlayerStateUpdated();
        }
        // UPDATE ALBUMLIST
//        if (status.has("albumlist") && status.getJSONArray("albumlist") != null) {
//            JSONArray albumlist = status.getJSONArray("albumlist");
//            albumModels.clear();
//            for (int i = 0; i < albumlist.length(); i++) {
//                try {
//                    albumlist.getJSONObject(i);
//                    //AlbumModel albumModel =  new AlbumModel();
//                    //albumModels.add(albumModel);
//                } catch (JSONException e) {
//                    Log.v(TAG, "Could not add song to listview - Exception:" + e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//            for (AlbumListUpdateListener listener : albumListUpdateListeners) {
//                listener.AlbumListUpdated();
//            }
//        }
        // UPDATE SONGLIST
//        if (status.has("songlist") && status.has("albumid")) {
//            Log.d(TAG, "albumid from message: " + status.getLong("albumid"));
//            if (status.getJSONArray("songlist").length() > 0) {
//                for (AlbumModel albumModel : albumModels) {
//                    if (albumModel.getID() == status.getLong("albumid")) {
//                        //albumModel.fillSongList(status.getJSONArray("songlist"));
//                    }
//                }
//            }
//            for (SongListUpdateListener listener : songListUpdateListeners) {
//                listener.SongListUpdated();
//            }
//        }
    }

    /**
     * Finds an Album by ID
     * @param id The ID of the album
     * @return The album or null if album not found
     */
    @Nullable
    public AlbumModel findAlbum(long id) {
        for (AlbumModel albumModel : albumModels) {
            if (albumModel.getID() == id) {
                return albumModel;
            }
        }
        return null;
    }

    /**
     * Find song by given ID
     * @param songid The song ID
     * @return The song or null if not found
     */
    @Nullable
    private SongModel findSongByID(long songid) {
        for (AlbumModel albumModel : albumModels) {
            for (SongModel songModel : albumModel.getSongList()) {
                if (songModel.getID() == songid) return songModel;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[state: %s, volume: %d, albumcount: %d]", state, volume, albumModels.size());
    }

    public interface StateUpdateListener {
        /**
         * Is invoked when status is changed
         */
        void MelonPlayerStateUpdated();
    }

}
