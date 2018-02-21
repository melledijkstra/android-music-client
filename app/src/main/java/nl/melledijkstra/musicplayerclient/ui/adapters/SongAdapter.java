package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;

/**
 * <p>Created by melle on 7-12-2016.</p>
 */

public class SongAdapter extends BaseAdapter {

    private static final String TAG = "SongAdapter";

    private Context mContext;
    private ArrayList<SongModel> songModels;

    public SongAdapter(Context mContext, ArrayList<SongModel> songModels) {
        this.mContext = mContext;
        this.songModels = songModels;
    }

    @Override
    public int getCount() {
        return songModels.size();
    }

    @Override
    public SongModel getItem(int position) {
        return songModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item;
        SongModel songModel = (position <= songModels.size()) ? songModels.get(position) : null;
        if(convertView == null) {
            item = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.song_item, null);
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
