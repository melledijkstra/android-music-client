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
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayerListener;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;
import nl.melledijkstra.musicplayerclient.ui.adapters.SongAdapter;

public class SongsFragment extends Fragment implements MelonPlayerListener {

    SwipeRefreshLayout refreshSwipeLayout;
    ListView songListView;
    private Album album;

    // ListAdapter that dynamically fills the music list
    public SongAdapter musicListAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        App.melonPlayer.registerListener(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Album Songs");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long albumid = getArguments().getLong("albumid", -1);
        if(albumid != -1 && getActivity() != null) {
            ((MainActivity)getActivity()).mBoundService.sendMessage(new MessageBuilder().songList(albumid).build());
        }
        album = App.melonPlayer.findAlbum(albumid);
        if(album != null) {
            getActivity().setTitle(album.getTitle());
        }

        Log.d(App.TAG,"SongsFragment created");
        musicListAdapter = new SongAdapter(getActivity(), App.melonPlayer.findAlbum(albumid).getSongList());

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
            ((MainActivity)getActivity()).mBoundService.sendMessage(new MessageBuilder().songList(album.getID()).build());
        }
    };

    private AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                // TODO: Create message with Factory pattern maybe?
                JSONObject obj = new JSONObject();
                obj.put("cmd","mplayer");
                JSONObject mplayer = new JSONObject();
                mplayer.put("cmd","play");
                mplayer.put("songid",album.getSongList().get(position).getID());
                obj.put("mplayer",mplayer);
                ((MainActivity)getActivity()).mBoundService.sendMessage(obj);
            } catch (JSONException e) {
                Toast.makeText(getActivity(), "JSON creation error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.v(App.TAG,"JSON creation error: "+e.getMessage());
            }
        }
    };

    @Override
    public void onResume() {
        musicListAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void melonPlayerUpdated() {
        musicListAdapter.notifyDataSetChanged();
        if(refreshSwipeLayout.isRefreshing())
            refreshSwipeLayout.setRefreshing(false);
    }
}
