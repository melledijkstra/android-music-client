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
import android.widget.GridView;
import android.widget.Toast;

import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.models.Album;
import nl.melledijkstra.musicplayerclient.ui.AlbumAdapter;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

public class AlbumsFragment extends Fragment implements MessageReceiver, SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {

    public static String TAG = "AlbumsFragment";

    GridView albumGridView;
    SwipeRefreshLayout swipeLayout;

    AlbumAdapter adapter;
    Album[] albums;

    @Override
    public void onAttach(Context context) {
        if(context instanceof MainActivity) {
            ((MainActivity)context).registerMessageReceiver(this);
        } else { Log.d(App.TAG, getClass().getSimpleName()+" - Could not retrieve Activity"); }
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        getActivity().setTitle("Albums");
        albums = new Album[] {
                new Album("Chill"),
                new Album("House"),
                new Album("Classic"),
                new Album("Future House"),
                new Album("Test"),
                new Album("Another Album")
        };
        adapter = new AlbumAdapter(getActivity(), albums);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(App.TAG,"Fragment created");
        if(getView() != null) {
            View root = getView();

            // get views
            albumGridView = (GridView) root.findViewById(R.id.gv_album_list);
            albumGridView.setAdapter(adapter);
            albumGridView.setOnItemClickListener(this);

            swipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.album_swipe_refresh_layout);
        }

        setListeners();
    }

    private void setListeners() {
        swipeLayout.setOnRefreshListener(this);
    }

    @Override
    public void onReceive(JSONObject obj) {

    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Album album = (position <= albums.length) ? albums[position] : null;
        if(album != null) {
            SongsFragment songsFragment = new SongsFragment();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.music_content_container, songsFragment)
                    .commit();
            Toast.makeText(getActivity(), "Clicked "+album.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }
}
