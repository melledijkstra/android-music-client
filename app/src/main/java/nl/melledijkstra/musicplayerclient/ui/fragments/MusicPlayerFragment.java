package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MusicPlayerFragment extends Fragment {

    ListView songListView;
    SeekBar skMusicTime;
    ImageButton btnPreviousSong, btnPlayPause, btnNextSong;
    SwipeRefreshLayout refreshSwipeLayout;

    // ListAdapter that dynamically fills the music list
    public ArrayAdapter<String> musicListAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(App.TAG,"Fragment created");
        if(getView() != null) {
            View root = getView();

            // get views
            refreshSwipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_refresh_layout);
            skMusicTime = (SeekBar) root.findViewById(R.id.sbMusicTime);
            btnPreviousSong = (ImageButton) root.findViewById(R.id.btnPreviousSong);
            btnPlayPause = (ImageButton) root.findViewById(R.id.btnPlayPause);
            btnNextSong = (ImageButton) root.findViewById(R.id.btnNextSong);
            songListView = (ListView) root.findViewById(R.id.songListView);

            // set action listeners
            refreshSwipeLayout.setOnRefreshListener(onRefreshListener);

            //musicListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, musicPlayer.songList);
            songListView.setAdapter(musicListAdapter);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(container == null) {
            return null;
        }
        return inflater.inflate(R.layout.musicplayer_fragment_layout, container, false);
    }

    /**
     * Song ListView Refresh action that populates the ListView with songs
     */
    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.v(App.TAG,getClass().getSimpleName()+" - Sending message LIST");
            ((MainActivity)getActivity()).mBoundService.sendDebugMessage("LIST");
            refreshSwipeLayout.setRefreshing(false);
        }
    };

}
