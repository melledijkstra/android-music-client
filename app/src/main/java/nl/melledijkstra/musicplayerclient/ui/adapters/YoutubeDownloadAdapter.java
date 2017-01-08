package nl.melledijkstra.musicplayerclient.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.YTDLDownload;

/**
 * Created by melle on 19-12-2016.
 */
public class YoutubeDownloadAdapter extends BaseAdapter{

    private ArrayList<YTDLDownload> downloads;
    private Context context;

    public YoutubeDownloadAdapter(Context context, ArrayList<YTDLDownload> downloads) {
        this.context = context;
        this.downloads = downloads;
    }

    @Override
    public int getCount() {
        return downloads.size();
    }

    @Override
    public Object getItem(int position) {
        return downloads.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item;
        YTDLDownload download_item = (position <= downloads.size()) ? downloads.get(position) : null;
        if(convertView == null) {
            item = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.download_item, null);
        } else {
            item = convertView;
        }

        // TODO: set new values
        // item.findViewById(R.id.speedTextView).setText(download_item.getSpeed());

        return item;
    }
}
