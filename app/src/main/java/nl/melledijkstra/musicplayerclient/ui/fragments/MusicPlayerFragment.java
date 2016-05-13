package nl.melledijkstra.musicplayerclient.ui.fragments;

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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MusicPlayerFragment extends Fragment implements MessageReceiver {

    ListView songListView;
    SeekBar skMusicTime;
    ImageButton btnPreviousSong, btnPlayPause, btnNextSong;
    SwipeRefreshLayout refreshSwipeLayout;
    TextView tvCurrentSong, tvCurPos, tvSongDuration;

    // ListAdapter that dynamically fills the music list
    public ArrayAdapter<String> musicListAdapter;

    // TODO: remove this and store music information when retrieving the list
    int songLength;

    boolean isDragging;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(App.TAG,"Fragment created");
        if(getView() != null) {
            View root = getView();

            // get views
            refreshSwipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_refresh_layout);
            skMusicTime = (SeekBar) root.findViewById(R.id.sbMusicTime);
            skMusicTime.setOnSeekBarChangeListener(onSeekbarChange);
            btnPreviousSong = (ImageButton) root.findViewById(R.id.btnPreviousSong);
            btnPlayPause = (ImageButton) root.findViewById(R.id.btnPlayPause);
            btnPlayPause.setOnClickListener(onPlayPauseClick);
            btnNextSong = (ImageButton) root.findViewById(R.id.btnNextSong);
            songListView = (ListView) root.findViewById(R.id.songListView);
            tvCurrentSong = (TextView) root.findViewById(R.id.tvCurrentSong);
            tvCurPos = (TextView) root.findViewById(R.id.tvSongCurPos);
            tvSongDuration = (TextView) root.findViewById(R.id.tvSongDuration);

            // set action listeners
            refreshSwipeLayout.setOnRefreshListener(onRefreshListener);

            musicListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, App.theMusicPlayer.songList);

            songListView.setAdapter(musicListAdapter);
            songListView.setOnItemClickListener(onItemClick);

        }
    }

    private View.OnClickListener onPlayPauseClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO: Use message factory!
            try {
                JSONObject obj = new JSONObject();
                JSONObject mplayer = new JSONObject();
                obj.put("cmd","mplayer");
                mplayer.put("cmd","pause");
                obj.put("mplayer",mplayer);
                ((MainActivity)getActivity()).mBoundService.sendMessage(obj);
            } catch (JSONException e) {
                Log.v(App.TAG,"Could not create play/pause message - "+e.getMessage());
                e.printStackTrace();
            }
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
                mplayer.put("song_id",position);
                obj.put("mplayer",mplayer);
                ((MainActivity)getActivity()).mBoundService.sendMessage(obj);
            } catch (JSONException e) {
                Toast.makeText(getActivity(), "JSON creation error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.v(App.TAG,"JSON creation error: "+e.getMessage());
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener onSeekbarChange = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                Log.v(App.TAG,"Seekbar change: "+progress);
            }
            tvCurPos.setText(String.format(Locale.getDefault(),
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(progress) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(progress) % TimeUnit.MINUTES.toSeconds(1)));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isDragging = false;
            try {
                // TODO: use message factory!
                JSONObject obj = new JSONObject();
                JSONObject mplayer = new JSONObject();
                mplayer.put("cmd","changepos");
                mplayer.put("pos",seekBar.getProgress());
                obj.put("cmd","mplayer");
                obj.put("mplayer",mplayer);
                ((MainActivity)getActivity()).mBoundService.sendMessage(obj);
            } catch (JSONException e) {
                Log.v(App.TAG, "Could not create changepos message");
                e.printStackTrace();
            }
        }
    };

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
            Log.v(App.TAG,getClass().getSimpleName()+" - Sending LIST command");
            JSONObject command = new JSONObject();
            try {
                command.put("cmd","LIST");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ((MainActivity)getActivity()).mBoundService.sendMessage(command);
            refreshSwipeLayout.setRefreshing(false);
        }
    };

    public void updateSongList(ArrayList<String> songs) {
        App.theMusicPlayer.songList.clear();
        App.theMusicPlayer.songList.addAll(songs);
        this.musicListAdapter.notifyDataSetChanged();
    }

    public void updateSongList(JSONArray songs) {
        App.theMusicPlayer.songList.clear();
        for(int i = 0; i < songs.length();i++) {
            try {
                App.theMusicPlayer.songList.add(songs.getString(i));
            } catch (JSONException e) {
                Log.v(App.TAG,"Could not add song to listview - Exception:" +e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReceive(JSONObject obj) {
        try {
            if(obj.has("cur_pos")) {
                int curPosition = obj.getInt("cur_pos");
                Log.e(App.TAG, "Map("+curPosition+",1,"+songLength+",1,"+skMusicTime.getMax()+")");
                if(curPosition <= 0) curPosition = 1;
                curPosition = ((int) Utils.Map(curPosition, 1, songLength, 1, skMusicTime.getMax()));
                Log.e(App.TAG, "Mapped pos: "+curPosition);
                if(!isDragging) {
                    if (curPosition > 0) {
                        skMusicTime.setProgress(curPosition);
                    } else {
                        skMusicTime.setProgress(0);
                    }
                }
            }

            if(obj.has("songlist") && obj.getJSONArray("songlist") != null) {
                updateSongList(obj.getJSONArray("songlist"));
            }

            if(obj.has("cur_song")) {
                if(obj.isNull("cur_song")) {
                    tvCurrentSong.setVisibility(View.GONE);
                } else {
                    tvCurrentSong.setVisibility(View.VISIBLE);
                    int curSongIndex = obj.getInt("cur_song");
                    String songName = App.theMusicPlayer.songList.get(curSongIndex);
                    tvCurrentSong.setText(songName);
                }
            }

            if(obj.has("length")) {
                // Store song length when gathering songlist data for the first time
                // That would save a lot of network communication
                int songLength = obj.getInt("length");
                skMusicTime.setMax(songLength);

                tvSongDuration.setText(String.format(Locale.getDefault(),
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(songLength) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(songLength) % TimeUnit.MINUTES.toSeconds(1)));

                Log.v(App.TAG,"Length of song: "+songLength);
                this.songLength = songLength;
            }

        } catch (JSONException e) {
            Log.v(App.TAG,"JSON error: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        // Remove all music song from the list when fragment is destroyed
        App.theMusicPlayer.songList.clear();
        musicListAdapter.notifyDataSetChanged();
        super.onDestroy();
    }
}
