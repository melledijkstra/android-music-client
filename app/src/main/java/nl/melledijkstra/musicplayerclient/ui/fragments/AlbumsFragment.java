package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.ProgressDialog;
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
import android.widget.GridView;

import java.util.Collections;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayerListener;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;
import nl.melledijkstra.musicplayerclient.ui.adapters.AlbumAdapter;

public class AlbumsFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener,
        AdapterView.OnItemClickListener,
        MelonPlayerListener {

    public static String TAG = "AlbumsFragment";

    GridView albumGridView;
    SwipeRefreshLayout swipeLayout;

    AlbumAdapter albumAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        App.melonPlayer.registerListener(this);
        albumAdapter = new AlbumAdapter(getActivity(), App.melonPlayer.albums);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Albums");
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(App.TAG,"Fragment created");

        if(App.DEBUG) {
            App.melonPlayer.albums.clear();
            Collections.addAll(App.melonPlayer.albums,
                new Album("Chill", true),
                new Album("House", false),
                new Album("Classic", true),
                new Album("Future House", false),
                new Album("Test", false),
                new Album("Another Album", false));
        }

        if(getView() != null) {
            View root = getView();

            // get views
            albumGridView = (GridView) root.findViewById(R.id.gv_album_list);
            albumGridView.setAdapter(albumAdapter);
            albumGridView.setOnItemClickListener(this);

            swipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.album_swipe_refresh_layout);
        }

        setListeners();
    }

    private void setListeners() {
        swipeLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        ((MainActivity)getActivity()).mBoundService.sendMessage(new MessageBuilder()
                .albumList()
                .build());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Album album = (position <= App.melonPlayer.albums.size()) ? App.melonPlayer.albums.get(position) : null;
        if(album != null) {
            ((MainActivity)getActivity()).showSongsFragment(album);
        }
    }

    @Override
    public void melonPlayerUpdated() {
        Log.v(TAG, "Update Data");
        if(swipeLayout.isRefreshing())
            swipeLayout.setRefreshing(false);

        albumAdapter.notifyDataSetChanged();
    }
}
