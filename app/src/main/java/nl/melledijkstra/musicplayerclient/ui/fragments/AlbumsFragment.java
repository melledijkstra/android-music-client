package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.Collections;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;
import nl.melledijkstra.musicplayerclient.ui.adapters.AlbumAdapter;

public class AlbumsFragment extends ServiceBoundFragment implements
        SwipeRefreshLayout.OnRefreshListener,
        AdapterView.OnItemClickListener,
        MelonPlayer.AlbumListUpdateListener {

    public static String TAG = "AlbumsFragment";

    GridView albumGridView;
    SwipeRefreshLayout swipeLayout;

    ArrayList<Album> albums;
    AlbumAdapter albumGridAdapter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        albums = new ArrayList<>();
        albumGridAdapter = new AlbumAdapter(getActivity(), App.melonPlayer.albums);
        App.melonPlayer.registerAlbumListChangeListener(this);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Retrieving Albums...");
        progressDialog.show();
        Log.v(TAG,"Fragment created");
    }

    @Override
    protected void onBounded() {
        boundService.sendMessage(new MessageBuilder().albumList().build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Albums");
        View layout = inflater.inflate(R.layout.fragment_albums, container, false);

        // get views
        albumGridView = (GridView) layout.findViewById(R.id.gv_album_list);
        albumGridView.setAdapter(albumGridAdapter);
        albumGridView.setOnItemClickListener(this);

        swipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.album_swipe_refresh_layout);

        setListeners();

        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(App.DEBUG) {
            App.melonPlayer.albums.clear();
            Collections.addAll(App.melonPlayer.albums,
                new Album(0,"Chill", true),
                new Album(1,"House", false),
                new Album(2,"Classic", true),
                new Album(3,"Future House", false),
                new Album(4,"Test", false),
                new Album(5,"Another Album", false));
        }
    }

    private void setListeners() {
        swipeLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        boundService.sendMessage(new MessageBuilder().albumList().build());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Album album = (position <= App.melonPlayer.albums.size()) ? App.melonPlayer.albums.get(position) : null;
        if(album != null) {
            ((MainActivity)getActivity()).showSongsFragment(album);
        }
    }

    @Override
    public void AlbumListUpdated() {
        albumGridAdapter.notifyDataSetChanged();
        if(progressDialog.isShowing()) progressDialog.dismiss();
        if(swipeLayout != null && swipeLayout.isRefreshing()) swipeLayout.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.melonPlayer.unRegisterAlbumListChangeListener(this);
    }
}
