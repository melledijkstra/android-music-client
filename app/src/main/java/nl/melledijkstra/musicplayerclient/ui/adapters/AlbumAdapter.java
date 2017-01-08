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
import nl.melledijkstra.musicplayerclient.melonplayer.Album;

/**
 * <p>Created by melle on 2-10-2016.</p>
 */

public class AlbumAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Album> albums;

    public AlbumAdapter(Context c, ArrayList<Album> albums) {
        this.mContext = c;
        this.albums = albums;
    }

    @Override
    public int getCount() {
        return albums.size();
    }

    @Override
    public Album getItem(int position) {
        return albums.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO: This method needs improvement
        View item;
        final Album album = (position <= albums.size()) ? albums.get(position) : null;
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            item = inflater.inflate(R.layout.album_item, null);
        } else {
            item = convertView;
        }

        // Album title
        TextView textView = (TextView) item.findViewById(R.id.album_title);
        // Album cover
        ImageView imageView = (ImageView) item.findViewById(R.id.album_cover);
        // Favorite btn
//        final ImageView favoriteImage = (ImageView) item.findViewById(R.id.favoriteImageView);
//        favoriteImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // TODO: Make album actually favorite
//                if(true) { //album != null && album.isFavorite()) {
//                    favoriteImage.setImageResource(R.drawable.ic_action_star_10);
//                } else {
//                    favoriteImage.setImageResource(R.drawable.ic_action_star_0);
//                }
//            }
//        });
        textView.setText(album != null ? album.getTitle() : null);
        Bitmap cover = null;
        if (album != null) {
            cover = album.getCover() != null ? album.getCover() : null;
        }
        imageView.setImageBitmap((cover != null) ? cover : ((BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_cover)).getBitmap());

        return item;
    }
}
