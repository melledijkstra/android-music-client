package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collections;

import io.grpc.stub.StreamObserver;
import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.grpc.MediaControl;
import nl.melledijkstra.musicplayerclient.grpc.MediaData;
import nl.melledijkstra.musicplayerclient.grpc.Song;
import nl.melledijkstra.musicplayerclient.grpc.SongList;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;
import nl.melledijkstra.musicplayerclient.ui.adapters.SongAdapter;

public class SongsFragment extends ServiceBoundFragment implements SongAdapter.RecyclerItemClickListener {

    private static final String TAG = "SongsFragment";

    SwipeRefreshLayout refreshSwipeLayout;
    RecyclerView songListRecyclerView;
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

        // refreshlayout
        refreshSwipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.song_swipe_refresh_layout);
        refreshSwipeLayout.setOnRefreshListener(onRefreshListener);

        // recyclerview
        songListAdapter = new SongAdapter(songModels, this);
        songListRecyclerView = (RecyclerView) layout.findViewById(R.id.songListView);
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Options for individual songs
        menu.setHeaderTitle("Song Options");
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.song_item_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final SongModel songModel = songModels.get(info.position);
        switch (item.getItemId()) {
            case R.id.menu_play_next:
                //sendMessageIfBound(new MessageBuilder().playNext(songModel.getID()).build());
                break;
            case R.id.menu_rename:
                View renameSongDialog = getActivity().getLayoutInflater().inflate(R.layout.rename_song_dialog, null);
                final EditText edRenameSong = ((EditText) renameSongDialog.findViewById(R.id.edRenameSong));
                edRenameSong.setText(songModel.getTitle());
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_mode_edit)
                        .setTitle("Rename")
                        .setView(renameSongDialog)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //sendMessageIfBound(new MessageBuilder().renameSong(songModel.getID(), edRenameSong.getText().toString()).build());
                            }
                        }).show();
                break;
            case R.id.menu_move:
                View moveSongDialog = getActivity().getLayoutInflater().inflate(R.layout.move_song_dialog, null);
                final Spinner spinnerAlbums = ((Spinner) moveSongDialog.findViewById(R.id.spinnerAlbums));
                spinnerAlbums.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, boundService.getMelonPlayer().albumModels));
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_reply)
                        .setTitle("Move")
                        .setView(moveSongDialog)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Move", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //sendMessageIfBound(new MessageBuilder().moveSong(songModel.getID(), ((AlbumModel)spinnerAlbums.getSelectedItem()).getID()).build());
                            }
                        }).show();
                break;
            case R.id.menu_delete:
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_action_trash)
                        .setTitle("Delete")
                        .setMessage("Do you really want to delete '" + songModel.getTitle() + "'?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //sendMessageIfBound(new MessageBuilder().deleteSong(songModel.getID()).build());
                            }
                        }).show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onBounded() {
        retrieveSongList();
    }

    @Override
    protected void onUnbound() {
        super.onUnbound();
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
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            songListAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "grpc onError: ", t);
                }

                @Override
                public void onCompleted() {
                    Log.i(TAG, "onCompleted: retrieving songs done");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                            refreshSwipeLayout.setRefreshing(false);
                        }
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
    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            retrieveSongList();
        }
    };

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
