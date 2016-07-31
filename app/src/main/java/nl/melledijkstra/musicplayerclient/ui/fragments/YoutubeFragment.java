package nl.melledijkstra.musicplayerclient.UI.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.UI.MainActivity;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class YoutubeFragment extends Fragment implements MessageReceiver {

    @Override
    public void onAttach(Context context) {
        if(context instanceof MainActivity) {
            ((MainActivity)context).registerMessageReceiver(this);
        } else { Log.d(App.TAG, getClass().getSimpleName()+" - Could not retrieve Activity"); }
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(container == null) {
            return null;
        }
        return inflater.inflate(R.layout.youtube_fragment_layout, container, false);
    }

    @Override
    public void onReceive(JSONObject obj) {
        // TODO: react on new json message from main activity
    }

}
