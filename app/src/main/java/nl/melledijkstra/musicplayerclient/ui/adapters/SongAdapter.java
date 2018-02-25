package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;

/**
 * <p>Created by melle on 7-12-2016.</p>
 */

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private static final String TAG = "SongAdapter";

    /**
     * Click itemClickListener for items in recyclerview
     */
    private RecyclerItemClickListener itemClickListener;
    private final PopupMenu.OnMenuItemClickListener menuListener;

    private ArrayList<SongModel> songModels;
    private Integer currentPopupPosition;

    public SongAdapter(ArrayList<SongModel> songModels, RecyclerItemClickListener itemClickListener,
                       PopupMenu.OnMenuItemClickListener onMenuItemClickListener) {
        this.songModels = songModels;
        this.itemClickListener = itemClickListener;
        this.menuListener = onMenuItemClickListener;
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
        holder.itemView.setOnClickListener(view -> itemClickListener.onItemClick(view, hPosition));

        holder.tvSongOptions.setOnClickListener(view -> {
            currentPopupPosition = hPosition;
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.song_item_menu);
            popup.setOnMenuItemClickListener(menuListener);
            popup.show();
        });

        holder.tvTitle.setText(songModel != null ? songModel.getTitle() : null);
        if (songModel != null && songModel.getDuration() != 0) {
            holder.tvDuration.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            holder.tvDuration.setText(Utils.secondsToDurationFormat(songModel.getDuration()));
        } else {
            holder.tvDuration.setTypeface(null, Typeface.ITALIC);
            holder.tvDuration.setText(R.string.undefined);
        }
    }

    /**
     * Gets the position of item where popup is currently shown, too bad Android doesn't make this a little easier
     * @return The position of item where popup is shown
     */
    public Integer getPosition() {
        return currentPopupPosition;
    }

    @Override
    public int getItemCount() {
        return songModels.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.song_title)
        TextView tvTitle;
        @BindView(R.id.song_duration)
        TextView tvDuration;
        @BindView(R.id.song_option_btn)
        TextView tvSongOptions;

        SongViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public interface RecyclerItemClickListener {
        void onItemClick(View view, int position);
    }
}
