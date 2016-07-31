package nl.melledijkstra.musicplayerclient.Models;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MessageReceiver;

/**
 * <p>
 *     This class represents the Model object for the music player.
 *     It's knows the state of the python music player because it makes use of the MusicPlayerConnection class
 * </p>
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MusicClient implements MessageReceiver {

    // CONSTANTS
    public static final String DEFAULT_IP = "192.168.1.200";
    public static final int DEFAULT_PORT = 1010;
    public static final int DEFAULT_TIMEOUT = 5000;

    // List with songs
    public ArrayList<String> songList = new ArrayList<>();

    // current playing song
    Song currentSong;

    public MusicClient() {

    }

    public MusicClient(JSONObject state) {

    }

    public void play() {

    }

    public void pause() {

    }

    public void set_volume(int vol) {

    }

    public Song getCurrentSong() {
        return currentSong;
    }

    @Override
    /**
     * Hydrate objects with JSON from remote server
     */
    public void onReceive(JSONObject obj) {
        Log.i(App.TAG, "MusicClient got JSON Object, trying to fill musicplayer now...");
    }

    public void update(JSONObject json) {

    }
}
