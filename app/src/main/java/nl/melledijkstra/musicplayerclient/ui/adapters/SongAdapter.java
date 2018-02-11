package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.grpc.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.AlbumModel;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;

/**
 * Created by melle on 7-12-2016.
 */

public class SongAdapter extends BaseAdapter {

    private static final String TAG = SongAdapter.class.getSimpleName();
    private Context mContext;
    private AlbumModel albumModel;

    public SongAdapter(Context mContext, AlbumModel album) {
        this.mContext = mContext;
        this.albumModel = album;
    }

    @Override
    public int getCount() {
        return albumModel.getSongList().size();
    }

    @Override
    public SongModel getItem(int position) {
        return albumModel.getSongList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item;
        SongModel songModel = (position <= albumModel.getSongList().size()) ? albumModel.getSongList().get(position) : null;
        if(convertView == null) {
            item = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.song_item, null);
        } else {
            item = convertView;
        }

        // song title
        TextView tvTitle = (TextView) item.findViewById(R.id.song_title);
        tvTitle.setText(songModel != null ? songModel.getTitle() : null);
        TextView tvDuration = (TextView) item.findViewById(R.id.song_duration);
        if(songModel != null && songModel.getDuration() != 0) {
            tvDuration.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            tvDuration.setText(Utils.millisecondsToDurationFormat(songModel.getDuration()));
        } else {
            tvDuration.setTypeface(null, Typeface.ITALIC);
            tvDuration.setText("Undefined");
        }

        return item;
    }
}
