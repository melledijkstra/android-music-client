package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class YoutubeFragment extends Fragment implements MessageReceiver {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(container == null) {
            return null;
        }
        return inflater.inflate(R.layout.youtube_fragment_layout, container, false);
    }

    @Override
    public void onReceive(JSONObject json) {
        // TODO: react on new json message from main activity
    }

}
