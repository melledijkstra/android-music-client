package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;
import nl.melledijkstra.musicplayerclient.melonplayer.Song;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.adapters.SongAdapter;

public class SongsFragment extends ServiceBoundFragment implements
        MelonPlayer.SongListUpdateListener {

    private static final String TAG = SongsFragment.class.getSimpleName();
    SwipeRefreshLayout refreshSwipeLayout;
    ListView songListView;
    private long albumid;
    private Album album;

    // ListAdapter that dynamically fills the music list
    public SongAdapter songListAdapter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.melonPlayer.registerSongListChangeListener(this);
        Log.d(TAG,"Fragment Created");
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Retrieving Songs...");
        progressDialog.show();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_songs, container, false);

        refreshSwipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.song_swipe_refresh_layout);
        songListView = (ListView) layout.findViewById(R.id.songListView);
        registerForContextMenu(songListView);

        // set action listeners
        refreshSwipeLayout.setOnRefreshListener(onRefreshListener);
        songListView.setOnItemClickListener(onItemClick);

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
        final Song song = album.getSongList().get(info.position);
        switch(item.getItemId()) {
            case R.id.menu_play_next:
                sendMessageIfBound(new MessageBuilder().playNext(song.getID()).build());
                break;
            case R.id.menu_rename:
                View renameSongDialog = getActivity().getLayoutInflater().inflate(R.layout.rename_song_dialog, null);
                final EditText edRenameSong = ((EditText)renameSongDialog.findViewById(R.id.edRenameSong));
                edRenameSong.setText(song.getTitle());
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_mode_edit)
                        .setTitle("Rename")
                        .setView(renameSongDialog)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendMessageIfBound(new MessageBuilder().renameSong(song.getID(), edRenameSong.getText().toString()).build());
                            }
                        }).show();
                break;
            case R.id.menu_move:
                View moveSongDialog = getActivity().getLayoutInflater().inflate(R.layout.move_song_dialog, null);
                final Spinner spinnerAlbums = ((Spinner) moveSongDialog.findViewById(R.id.spinnerAlbums));
                spinnerAlbums.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, App.melonPlayer.albums));
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_reply)
                        .setTitle("Move")
                        .setView(moveSongDialog)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Move", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendMessageIfBound(new MessageBuilder().moveSong(song.getID(), ((Album)spinnerAlbums.getSelectedItem()).getID()).build());
                            }
                        }).show();
                break;
            case R.id.menu_delete:
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_action_trash)
                        .setTitle("Delete")
                        .setMessage("Do you really want to delete '"+song.getTitle()+"'?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendMessageIfBound(new MessageBuilder().deleteSong(song.getID()).build());
                            }
                        }).show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onBounded() {
        super.onBounded();
        boundService.sendMessage(new MessageBuilder().songList(albumid).build());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        albumid = getArguments().getLong("albumid", -1);
        album = App.melonPlayer.findAlbum(albumid);
        Log.d(TAG, "albumid: "+albumid);
        songListAdapter = new SongAdapter(getActivity(), albumid);
        songListView.setAdapter(songListAdapter);
    }

    /**
     * Song ListView Refresh action that populates the ListView with songs
     */
    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            boundService.sendMessage(new MessageBuilder().songList(albumid).build());
        }
    };

    private AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            boundService.sendMessage(new MessageBuilder().playSong(App.melonPlayer.findAlbum(albumid).getSongList().get(position).getID()).build());
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if(isBound) {
            boundService.sendMessage(new MessageBuilder().songList(albumid).build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.melonPlayer.unRegisterSongListChangeListener(this);
    }

    @Override
    public void SongListUpdated() {
        if(songListAdapter != null) songListAdapter.notifyDataSetChanged();
        if(progressDialog.isShowing()) progressDialog.dismiss();
        if(refreshSwipeLayout.isRefreshing()) refreshSwipeLayout.setRefreshing(false);
    }
}
