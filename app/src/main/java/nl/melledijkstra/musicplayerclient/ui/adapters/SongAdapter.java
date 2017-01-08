package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.renderscript.Type;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.Song;

/**
 * Created by melle on 7-12-2016.
 */

public class SongAdapter extends BaseAdapter {

    private static final String TAG = SongAdapter.class.getSimpleName();
    private Context mContext;
    private Album album;

    public SongAdapter(Context mContext, long albumid) {
        this.mContext = mContext;
        this.album = App.melonPlayer.findAlbum(albumid);
    }

    @Override
    public int getCount() {
        return album.getSongList().size();
    }

    @Override
    public Song getItem(int position) {
        return album.getSongList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item;
        Song song = (position <= album.getSongList().size()) ? album.getSongList().get(position) : null;
        if(convertView == null) {
            item = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.song_item, null);
        } else {
            item = convertView;
        }

        // song title
        TextView tvTitle = (TextView) item.findViewById(R.id.song_title);
        tvTitle.setText(song != null ? song.getTitle() : null);
        TextView tvDuration = (TextView) item.findViewById(R.id.song_duration);
        if(song != null && song.getDuration() != null) {
            tvDuration.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            tvDuration.setText(Utils.millisecondsToDurationFormat(song.getDuration()));
        } else {
            tvDuration.setTypeface(null, Typeface.ITALIC);
            tvDuration.setText("Undefined");
        }

        return item;
    }
}
