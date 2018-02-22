package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;

/**
 * <p>Created by melle on 7-12-2016.</p>
 */

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private static final String TAG = "SongAdapter";

    /**
     * Click listener for items in recyclerview
     */
    private final RecyclerItemClickListener listener;

    private ArrayList<SongModel> songModels;

    public SongAdapter(ArrayList<SongModel> songModels, RecyclerItemClickListener listener) {
        this.songModels = songModels;
        this.listener = listener;
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {
        SongModel songModel = (position <= songModels.size()) ? songModels.get(position) : null;

        final int hPosition = holder.getAdapterPosition();
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(view, hPosition);
            }
        });

        holder.tvTitle.setText(songModel != null ? songModel.getTitle() : null);
        if (songModel != null && songModel.getDuration() != 0) {
            holder.tvDuration.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            holder.tvDuration.setText(Utils.millisecondsToDurationFormat(songModel.getDuration()));
        } else {
            holder.tvDuration.setTypeface(null, Typeface.ITALIC);
            holder.tvDuration.setText("Undefined");
        }
    }

    @Override
    public int getItemCount() {
        return songModels.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDuration;

        SongViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.song_title);
            tvDuration = (TextView) view.findViewById(R.id.song_duration);
        }
    }

    public interface RecyclerItemClickListener {
        void onItemClick(View view, int position);
    }
}
