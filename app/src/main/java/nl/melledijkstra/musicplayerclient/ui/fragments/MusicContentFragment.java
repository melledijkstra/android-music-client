package nl.melledijkstra.musicplayerclient.UI.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.UI.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public class MusicContentFragment extends Fragment implements MessageReceiver {


    public MusicContentFragment() {
        // Required empty public constructor
        ((MainActivity)getActivity()).registerMessageReceiver(this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_music_content, container, false);
    }

    @Override
    public void onReceive(JSONObject obj) {

    }

}
