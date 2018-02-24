package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.grpc.stub.StreamObserver;
import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.grpc.MediaControl;
import nl.melledijkstra.musicplayerclient.grpc.MediaData;
import nl.melledijkstra.musicplayerclient.grpc.MediaType;
import nl.melledijkstra.musicplayerclient.grpc.MoveData;
import nl.melledijkstra.musicplayerclient.grpc.RenameData;
import nl.melledijkstra.musicplayerclient.grpc.Song;
import nl.melledijkstra.musicplayerclient.grpc.SongList;
import nl.melledijkstra.musicplayerclient.melonplayer.AlbumModel;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;
import nl.melledijkstra.musicplayerclient.ui.adapters.SongAdapter;

public class SongsFragment extends ServiceBoundFragment implements SongAdapter.RecyclerItemClickListener, PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "SongsFragment";

    @BindView(R.id.song_swipe_refresh_layout) SwipeRefreshLayout refreshSwipeLayout;
    @BindView(R.id.songListView) RecyclerView songListRecyclerView;
    private Unbinder unbinder;

    private long albumid;

    private ArrayList<SongModel> songModels;

    // ListAdapter that dynamically fills the music list
    private SongAdapter songListAdapter;
    private ProgressDialog progressDialog;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songModels = new ArrayList<>();
        Log.d(TAG, "Fragment Created");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_songs, container, false);
        unbinder = ButterKnife.bind(this, layout);

        // refreshlayout
        refreshSwipeLayout.setOnRefreshListener(onRefreshListener);

        // recyclerview
        songListAdapter = new SongAdapter(songModels, this, this);
        songListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        songListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        songListRecyclerView.setAdapter(songListAdapter);
        songListRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));

        // dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Retrieving Songs...");

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Integer position = songListAdapter.getPosition();
        if(position != null) {
            final SongModel songModel = songModels.get(position);
            switch (item.getItemId()) {
                case R.id.menu_play_next:
                    if (isBound) {
                        boundService.musicPlayerStub.addNext(MediaData.newBuilder()
                                .setType(MediaType.SONG)
                                .setId(songModel.getID()).build(), boundService.defaultMMPResponseStreamObserver);
                    }
                    break;
                case R.id.menu_rename:
                    View renameSongDialog = getActivity().getLayoutInflater().inflate(R.layout.rename_song_dialog, null);
                    final EditText edRenameSong = ((EditText) renameSongDialog.findViewById(R.id.edRenameSong));
                    edRenameSong.setText(songModel.getTitle());
                    new AlertDialog.Builder(getContext()).setIcon(R.drawable.ic_mode_edit)
                            .setTitle(R.string.rename)
                            .setView(renameSongDialog)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.rename, (dialog, which) -> {
                                if (isBound) {
                                    boundService.dataManagerStub.renameSong(RenameData.newBuilder()
                                            .setId(songModel.getID())
                                            .setNewTitle(edRenameSong.getText().toString()).build(), boundService.defaultMMPResponseStreamObserver);
                                }
                            }).show();
                    break;
                case R.id.menu_move:
                    if (isBound) {
                        View moveSongDialog = getActivity().getLayoutInflater().inflate(R.layout.move_song_dialog, null);
                        final Spinner spinnerAlbums = ((Spinner) moveSongDialog.findViewById(R.id.spinnerAlbums));
                        spinnerAlbums.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, boundService.getMelonPlayer().albumModels));
                        new AlertDialog.Builder(getContext())
                                .setIcon(R.drawable.ic_reply)
                                .setTitle(R.string.move)
                                .setView(moveSongDialog)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.move, (dialog, which) -> {
                                    AlbumModel selectedAlbum = ((AlbumModel) spinnerAlbums.getSelectedItem());
                                    if(selectedAlbum != null) {
                                        boundService.dataManagerStub.moveSong(MoveData.newBuilder()
                                                .setSongId(songModel.getID())
                                                .setAlbumId(selectedAlbum.getID())
                                                .build(), boundService.defaultMMPResponseStreamObserver);
                                    }
                                }).show();
                    }
                    break;
                case R.id.menu_delete:
                    new AlertDialog.Builder(getContext())
                            .setIcon(R.drawable.ic_action_trash)
                            .setTitle(R.string.delete)
                            .setMessage("Do you really want to delete '" + songModel.getTitle() + "'?")
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.delete, (dialog, which) -> {
                                if (isBound) {
                                    boundService.dataManagerStub.deleteSong(MediaData.newBuilder()
                                            .setId(songModel.getID()).build(), boundService.defaultMMPResponseStreamObserver);
                                }
                            }).show();
                    break;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onBounded() {
        retrieveSongList();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        albumid = getArguments().getLong("albumid", -1);
        Log.d(TAG, "albumid: " + albumid);
        retrieveSongList();
    }

    private void retrieveSongList() {
        if (isBound) {
            progressDialog.show();
            // TODO: move this to service
            boundService.musicPlayerStub.retrieveSongList(MediaData.newBuilder().setId(albumid).build(), new StreamObserver<SongList>() {
                @Override
                public void onNext(final SongList response) {
                    songModels.clear();
                    for (Song song : response.getSongListList()) {
                        songModels.add(new SongModel(song));
                    }
                    getActivity().runOnUiThread(() -> songListAdapter.notifyDataSetChanged());
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "grpc onError: ", t);
                }

                @Override
                public void onCompleted() {
                    Log.i(TAG, "onCompleted: retrieving songs done");
                    getActivity().runOnUiThread(() -> {
                        progressDialog.hide();
                        refreshSwipeLayout.setRefreshing(false);
                    });
                }
            });
        } else if (App.DEBUG) {
            songModels.clear();
            Collections.addAll(songModels,
                    new SongModel(0, "Artist - Test Song #1", 1000),
                    new SongModel(1, "Artist - Test Song #1", 1000),
                    new SongModel(2, "Artist - Test Song #1", 1000),
                    new SongModel(3, "Artist - Test Song #1", 1000),
                    new SongModel(4, "Artist - Test Song #1", 1000));
            songListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * SongModel ListView Refresh action that populates the ListView with songs
     */
    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = this::retrieveSongList;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (isBound) {
            boundService.musicPlayerStub.play(MediaControl.newBuilder()
                    .setState(MediaControl.State.PLAY)
                    .setSongId(songModels.get(position).getID())
                    .build(), boundService.defaultMMPResponseStreamObserver);
        }
    }
}
