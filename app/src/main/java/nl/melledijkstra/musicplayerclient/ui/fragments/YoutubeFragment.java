package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.melonplayer.YTDLDownload;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.adapters.YoutubeDownloadAdapter;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class YoutubeFragment extends ServiceBoundFragment {

    private static final String TAG = YoutubeFragment.class.getSimpleName();

    // UI
    FloatingActionButton fabNewDownload;
    ListView listViewDownloadQueue;

    YoutubeDownloadAdapter adapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Youtube Downloader");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_youtube, container, false);

        fabNewDownload = (FloatingActionButton) layout.findViewById(R.id.fabNewDownload);
        listViewDownloadQueue = (ListView) layout.findViewById(R.id.listDownloadQueue);
        listViewDownloadQueue.setAdapter(new YoutubeDownloadAdapter(getContext(), new ArrayList<YTDLDownload>()));

        setListeners();

        return layout;
    }

    private void setListeners() {
        fabNewDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View dialogLayout = getLayoutInflater(null).inflate(R.layout.new_download_dialog, null);

                final EditText etYoutubeUrl = (EditText) dialogLayout.findViewById(R.id.etYoutubeUrl);
                ClipboardManager clipManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipManager.getPrimaryClip();
                if(clipData != null) {
                    for(int i = 0; i < clipData.getItemCount();i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        if(Patterns.WEB_URL.matcher(item.getText()).matches()) {
                            // TODO: Let user choose to auto fill input
                            etYoutubeUrl.setText(item.getText());
                        }
                    }
                }

                final Spinner albumSpinner = (Spinner) dialogLayout.findViewById(R.id.spinChooseAlbum);

                albumSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, App.melonPlayer.albums));

                AlertDialog dialog = new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_action_youtube)
                        .setView(dialogLayout)
                        .setTitle("New Download")
                        .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Selected album: name("+albumSpinner.getSelectedItem()+") id("+((Album)albumSpinner.getSelectedItem()).getID()+")");
                                sendMessageIfBound(new MessageBuilder().download(etYoutubeUrl.getText().toString(), ((Album)albumSpinner.getSelectedItem()).getID()).build());
                            }
                        })
                        .create();

                dialog.show();

            }
        });
    }

}
