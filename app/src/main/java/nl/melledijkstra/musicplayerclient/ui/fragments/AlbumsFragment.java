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

import io.grpc.stub.StreamObserver;
import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.grpc.Album;
import nl.melledijkstra.musicplayerclient.grpc.AlbumList;
import nl.melledijkstra.musicplayerclient.grpc.MediaData;
import nl.melledijkstra.musicplayerclient.melonplayer.AlbumModel;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;
import nl.melledijkstra.musicplayerclient.ui.adapters.AlbumAdapter;

public class AlbumsFragment extends ServiceBoundFragment implements
        SwipeRefreshLayout.OnRefreshListener,
        AdapterView.OnItemClickListener {

    public static String TAG = "AlbumsFragment";

    GridView albumGridView;
    SwipeRefreshLayout swipeLayout;

    ArrayList<AlbumModel> albumModels;
    AlbumAdapter albumGridAdapter;
    ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        albumModels = new ArrayList<>();
        albumGridAdapter = new AlbumAdapter(getActivity(), albumModels);
        Log.v(TAG, "Fragment created");
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

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Retrieving Albums...");

        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (App.DEBUG && isBound) {
            albumModels.clear();
            Collections.addAll(albumModels,
                    new AlbumModel(0, "Chill", true),
                    new AlbumModel(1, "House", false),
                    new AlbumModel(2, "Classic", true),
                    new AlbumModel(3, "Future House", false),
                    new AlbumModel(4, "Test", false),
                    new AlbumModel(5, "Another AlbumModel", false));
        }
    }

    @Override
    protected void onBounded() {
        retrieveAlbumList();
    }

    private void retrieveAlbumList() {
        if (isBound && boundService != null) {
            progressDialog.show();
            boundService.musicPlayerStub.retrieveAlbumList(MediaData.getDefaultInstance(), new StreamObserver<AlbumList>() {
                @Override
                public void onNext(final AlbumList response) {
                    Log.i(TAG, "Retrieved albums, count: " + response.getAlbumListCount());
                    // TODO: find better way to store all album and song data, this is garbage code!!!!
                    albumModels.clear();
                    //boundService.getMelonPlayer().albumModels.clear();
                    for (Album album : response.getAlbumListList()) {
                        albumModels.add(new AlbumModel(album));
                        //boundService.getMelonPlayer().albumModels.add(new AlbumModel(album));
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            albumGridAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "grpc onError: ", t);
                }

                @Override
                public void onCompleted() {
                    Log.i(TAG, "onCompleted: album list call done");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                            swipeLayout.setRefreshing(false);
                        }
                    });
                }
            });
        }
    }

    private void setListeners() {
        swipeLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        retrieveAlbumList();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AlbumModel albumModel = (position < albumModels.size()) ? albumModels.get(position) : null;
        Log.i(TAG, "AlbumModel: " + albumModel);
        if (albumModel != null) {
            ((MainActivity) getActivity()).showSongsFragment(albumModel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
