package nl.melledijkstra.musicplayerclient;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.Models.MusicClient;

/**
 * <p>Created by Melle Dijkstra on 19-4-2016</p>
 */
public class App extends Application {

    public static final String TAG = "musicplayerclient";
    public static MusicClient musicClient;

    private static ArrayList<MessageReceiver> listeners;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String raw_json = intent.getStringExtra("msg");
            if(raw_json != null) {
                Log.v(App.TAG,"Application - got message: "+raw_json);
                try {
                    JSONObject json = new JSONObject(raw_json);
                    // notify all MessageReceivers that a new message was received
                    for (MessageReceiver listener : listeners) {
                        listener.onReceive(json);
                    }
                } catch (JSONException e) {
                    Log.e(App.TAG,"Corrupted json data: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        listeners = new ArrayList<>();
        musicClient = new MusicClient();
        registerMessageReceiver(musicClient);
        registerReceiver(receiver, new IntentFilter(ConnectionService.MESSAGERECEIVED));
    }

    /**
     * Register a receiver that gets notified if the Application gets a message
     * @param listener
     */
    public void registerMessageReceiver(MessageReceiver listener) {
        listeners.add(listener);
    }

    public void notifyMusicClient(String data) {
        try {
            JSONObject json = new JSONObject(data);
            musicClient.update(json);
        } catch (JSONException e) {
            Log.d(App.TAG, "Malformed JSON: "+e.getMessage());
            e.printStackTrace();
        }

    }

}
