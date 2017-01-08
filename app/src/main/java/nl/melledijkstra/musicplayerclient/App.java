package nl.melledijkstra.musicplayerclient;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;

/**
 * <p>Created by Melle Dijkstra on 19-4-2016</p>
 */
public class App extends Application {

    public static final String TAG = "musicplayerclient";
    public static MelonPlayer melonPlayer;
    private HashSet<MessageReceiver> jsonReceivers;

    /**
     * If app is in DEBUG mode then no connection is needed and dummy data is used
     */
    public static boolean DEBUG = false;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String raw_json = intent.getStringExtra("msg");
            if(raw_json != null) {
                try {
                    JSONObject json = new JSONObject(raw_json);
                    if(json.has("mplayer")) melonPlayer.onReceive(json.getJSONObject("mplayer"));
                    for (MessageReceiver receiver : jsonReceivers) {
                        receiver.onReceive(json);
                    }
                } catch (JSONException e) {
                    Log.e(TAG,"Incorrect json data: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    };

    public App() {
        melonPlayer = new MelonPlayer();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        jsonReceivers = new HashSet<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = prefs.getBoolean(PreferenceKeys.DEBUG, false);
        // the musicclient should be notified when message comes in. It can then update it's state
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(MelonPlayerService.MESSAGERECEIVED));
    }

    /**
     * Checks if the debug state has changed and sets the debug state for the application
     */
    public void updateDebugState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = prefs.getBoolean(PreferenceKeys.DEBUG, false);
    }

    public void registerMessageReceiver(MessageReceiver receiver) {
        jsonReceivers.add(receiver);
    }

    public void unRegisterMessageReceiver(MessageReceiver receiver) {
        jsonReceivers.remove(receiver);
    }

}
