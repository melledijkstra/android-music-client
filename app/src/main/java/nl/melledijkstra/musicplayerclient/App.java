package nl.melledijkstra.musicplayerclient;

import android.app.Application;

import nl.melledijkstra.musicplayerclient.entities.MusicPlayer;

/**
 * <p>Created by Melle Dijkstra on 19-4-2016</p>
 */
public class App extends Application {

    public static final String TAG = "musicplayerclient";
    public static MusicPlayer theMusicPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        theMusicPlayer = new MusicPlayer();
    }

}
