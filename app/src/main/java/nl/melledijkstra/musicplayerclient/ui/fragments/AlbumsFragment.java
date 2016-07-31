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

public class AlbumsFragment extends Fragment implements MessageReceiver {

    @Override
    public void onAttach(Context context) {
        if(context instanceof MainActivity) {
            ((MainActivity)context).registerMessageReceiver(this);
        } else { Log.d(App.TAG, getClass().getSimpleName()+" - Could not retrieve Activity"); }
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onReceive(JSONObject obj) {

    }
}
