package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Song;

/**
 * Created by melle on 7-12-2016.
 */

public class SongAdapter extends BaseAdapter implements ListAdapter {

    private Context mContext;
    private ArrayList<Song> songs;

    public SongAdapter(Context mContext, ArrayList<Song> songs) {
        this.mContext = mContext;
        this.songs = songs;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Song getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO: This method needs improvement
        View item;
        Song song = (position <= songs.size()) ? songs.get(position) : null;
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            item = inflater.inflate(R.layout.song_item, null);
        } else {
            item = convertView;
        }

        // song title
        TextView tv = (TextView) item.findViewById(R.id.song_title);
        tv.setText(song != null ? song.getTitle() : null);

        return item;
    }
}
