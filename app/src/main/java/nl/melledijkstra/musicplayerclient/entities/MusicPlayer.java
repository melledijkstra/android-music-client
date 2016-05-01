package nl.melledijkstra.musicplayerclient.entities;

import android.graphics.drawable.Drawable;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

/**
 * <p>
 *     This class represents the Model object for the music player.
 *     It's knows the state of the python music player because it makes use of the MusicPlayerConnection class
 * </p>
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MusicPlayer {

    // CONSTANTS
    public static final String DEFAULT_IP = "192.168.1.40";
    public static final int DEFAULT_PORT = 1010;
    public static final int DEFAULT_TIMEOUT = 5000;

    // List with songs
    public ArrayList<String> songList = new ArrayList<>();

    // current playing song
    Song currentSong;

    public MusicPlayer() {

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

}
