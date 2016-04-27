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

    // The activity for updating UI
    private MainActivity context;

    // List with songs
    public List<String> songList = new ArrayList<>();

    // current playing song
    Song currentSong;

    /**
     * Constructor
     * @param context The application context (Activity)
     */
    public MusicPlayer(MainActivity context) {
        this.context = context;
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

    /**
     * Show a toast on the main thread
     * @param message The message for the toast
     * @param duration The duration for the length it has to show
     */
    private void ToastOnMainThread(final String message, final int duration) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, duration).show();
            }
        });
    }

    /**
     * Show a toast on the main thread
     * @param message The message for the toast
     */
    private void ToastOnMainThread(final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUI(final ActionMenuItemView item, final Drawable icon) {
        Log.i(App.TAG,"Updating '"+item.toString()+"' with value '"+icon.toString()+"'");
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                item.setIcon(icon);
            }
        });
    }

}
