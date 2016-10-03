package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.models.Song;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

public class SongsFragment extends Fragment implements MessageReceiver {

    SwipeRefreshLayout refreshSwipeLayout;
    ListView songListView;

    // ListAdapter that dynamically fills the music list
    public ArrayAdapter<String> musicListAdapter;

    List<String> songs;

    @Override
    public void onAttach(Context context) {
        Log.d(App.TAG,"Attaching");
        if(context instanceof MainActivity) {
            ((MainActivity)context).registerMessageReceiver(this);
        } else { Log.d(App.TAG, getClass().getSimpleName()+" - Could not retrieve Activity"); }
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songs = new ArrayList<>();

        songs.add("Eddie Vedder - Long Nights");
        songs.add("John Mayer - Gravity");
        songs.add("Eddie Vedder - Guaranteed");
        songs.add("Eddie Vedder - Long Nights");
        songs.add("Eddie Vedder - Long Nights");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(App.TAG,"SongsFragment created");

        musicListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, songs);

        if(getView() != null) {
            View root = getView();

            refreshSwipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.song_swipe_refresh_layout);
            songListView = (ListView) root.findViewById(R.id.songListView);

            // set action listeners
            refreshSwipeLayout.setOnRefreshListener(onRefreshListener);

            songListView.setAdapter(musicListAdapter);
            songListView.setOnItemClickListener(onItemClick);
        }
    }

    /**
     * Song ListView Refresh action that populates the ListView with songs
     */
    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.v(App.TAG,getClass().getSimpleName()+" - Sending list command");
            ((MainActivity)getActivity()).mBoundService.sendMessage(Utils.generateJSONMessage(Utils.MessageTypes.SONGLIST));
            refreshSwipeLayout.setRefreshing(false);
        }
    };

    private AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getActivity(), "Selected: "+ songs.get(position), Toast.LENGTH_SHORT).show();
            try {
                // TODO: Create message with Factory pattern maybe?
                JSONObject obj = new JSONObject();
                obj.put("cmd","mplayer");
                JSONObject mplayer = new JSONObject();
                mplayer.put("cmd","play");
                mplayer.put("song_id",position);
                obj.put("mplayer",mplayer);
                ((MainActivity)getActivity()).mBoundService.sendMessage(obj);
            } catch (JSONException e) {
                Toast.makeText(getActivity(), "JSON creation error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.v(App.TAG,"JSON creation error: "+e.getMessage());
            }
        }
    };

    public void updateSongList(ArrayList<String> songs) {
        App.musicClient.songList.clear();
        App.musicClient.songList.addAll(songs);
        this.musicListAdapter.notifyDataSetChanged();
    }

    public void updateSongList(JSONArray songs) {
        App.musicClient.songList.clear();
        for(int i = 0; i < songs.length();i++) {
            try {
                App.musicClient.songList.add(songs.getString(i));
            } catch (JSONException e) {
                Log.v(App.TAG,"Could not add song to listview - Exception:" +e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReceive(JSONObject obj) {
        try {
            if(obj.has("songlist") && obj.getJSONArray("songlist") != null) {
                updateSongList(obj.getJSONArray("songlist"));
            }
        } catch (JSONException e) {
            Log.v(App.TAG, "JSONException: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        musicListAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        // Remove all music song from the list when fragment is destroyed
        App.musicClient.songList.clear();
        super.onDestroy();
    }
}
