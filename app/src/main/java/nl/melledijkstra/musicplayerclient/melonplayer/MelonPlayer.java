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

    private static final String TAG = "MelonPlayer";

    /** The singleton instance */
    private static MelonPlayer instance;

    /** The different states the melon player can reside in */
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

    /** The current volume of the melon player */
    private int volume = -1;

    /** Flag if the melon player is muted */
    private boolean mute = false;

    /** Current state of the melon player */
    private States state = States.NOTHINGSPECIAL;

    /** Current list of albums */
    public ArrayList<AlbumModel> albumModels;

    /** The currently playing song */
    @Nullable
    private SongModel currentSongModel = null;

    /** The current position of the song */
    private float songPosition = -1f;

    /** The current time of the playing song */
    private long currentTime = -1;

    // GETTERS

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

    /**
     * Private constructor
     */
    private MelonPlayer() {
        albumModels = new ArrayList<>();
        stateListeners = new HashSet<>();
    }

    /**
     * Get the one and only instance of MelonPlayer object
     * see Singleton Pattern
     * @return The one and only instance of this class
     */
    public static MelonPlayer getInstance() {
        if(instance == null) {
            instance = new MelonPlayer();
        }
        return instance;
    }

    /**
     * Set the new state of the melon player
     * @param status The new status
     */
    public void setState(MMPStatus status) {
        Log.i(TAG, "setting new state of MelonPlayer");
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
        return String.format(Locale.getDefault(), "[state: %s, volume: %d, albumcount: %d]", state, volume, albumModels.size());
    }

    public interface StateUpdateListener {
        /**
         * Is invoked when status is changed
         */
        void MelonPlayerStateUpdated();
    }

}
