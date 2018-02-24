package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.AlbumModel;

/**
 * <p>Created by melle on 2-10-2016.</p>
 */

public class AlbumAdapter extends BaseAdapter {

    private static final String TAG = "AlbumAdapter";

    private Context mContext;
    private ArrayList<AlbumModel> albumModels;

    public AlbumAdapter(Context c, ArrayList<AlbumModel> albumModels) {
        this.mContext = c;
        this.albumModels = albumModels;
    }

    @Override
    public int getCount() {
        return albumModels.size();
    }

    @Override
    public AlbumModel getItem(int position) {
        return albumModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO: This method needs improvement
        View item;
        AlbumModel albumModel = (position <= albumModels.size()) ? albumModels.get(position) : null;
        if(convertView == null) {
            item = LayoutInflater.from(mContext).inflate(R.layout.album_item, null);
        } else {
            item = convertView;
        }

        // AlbumModel title
        TextView textView = (TextView) item.findViewById(R.id.album_title);
        // AlbumModel cover
        ImageView imageView = (ImageView) item.findViewById(R.id.album_cover);
        // Favorite btn
//        final ImageView favoriteImage = (ImageView) item.findViewById(R.id.favoriteImageView);
//        favoriteImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onPreviousClick(View v) {
//                // TODO: Make albumModel actually favorite
//                if(true) { //albumModel != null && albumModel.isFavorite()) {
//                    favoriteImage.setImageResource(R.drawable.ic_action_star_10);
//                } else {
//                    favoriteImage.setImageResource(R.drawable.ic_action_star_0);
//                }
//            }
//        });
        textView.setText(albumModel != null ? albumModel.getTitle() : null);
        Bitmap cover = null;
        if (albumModel != null) {
            cover = albumModel.getCover();
        }
        imageView.setImageBitmap((cover != null) ? cover : ((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_cover)).getBitmap());

        return item;
    }
}
