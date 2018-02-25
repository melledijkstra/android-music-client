package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.grpc.stub.StreamObserver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.grpc.DownloadStatus;
import nl.melledijkstra.musicplayerclient.grpc.MediaDownload;
import nl.melledijkstra.musicplayerclient.melonplayer.AlbumModel;
import nl.melledijkstra.musicplayerclient.melonplayer.YTDLDownload;
import nl.melledijkstra.musicplayerclient.ui.adapters.YoutubeDownloadAdapter;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MediaDownloadFragment extends ServiceBoundFragment {

    private static final String TAG = MediaDownloadFragment.class.getSimpleName();

    // UI Components
    @BindView(R.id.fabNewDownload)
    FloatingActionButton fabNewDownload;
    @BindView(R.id.listDownloadQueue)
    ListView listViewDownloadQueue;
    private Unbinder unbinder;


    ArrayList<YTDLDownload> downloadModels;

    YoutubeDownloadAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Youtube Downloader");
        downloadModels = new ArrayList<>();
        adapter = new YoutubeDownloadAdapter(getContext(), downloadModels);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_youtube, container, false);
        unbinder = ButterKnife.bind(this, layout);

        listViewDownloadQueue.setAdapter(adapter);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.fabNewDownload)
    public void onClick() {
        View dialogLayout = getActivity().getLayoutInflater().inflate(R.layout.new_download_dialog, null);

        final EditText etYoutubeUrl = (EditText) dialogLayout.findViewById(R.id.etYoutubeUrl);
        ClipboardManager clipManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipManager != null ? clipManager.getPrimaryClip() : null;
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                if (Patterns.WEB_URL.matcher(item.getText()).matches()) {
                    // TODO: Let user choose to auto fill input
                    etYoutubeUrl.setText(item.getText());
                }
            }
        }

        final Spinner albumSpinner = (Spinner) dialogLayout.findViewById(R.id.spinChooseAlbum);

        albumSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, boundService.getMelonPlayer().albumModels));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setIcon(R.drawable.ic_action_youtube)
                .setView(dialogLayout)
                .setTitle(R.string.new_download)
                .setPositiveButton(R.string.download, (dialog1, which) -> {
                    AlbumModel selectedAlbum = ((AlbumModel) albumSpinner.getSelectedItem());
                    if (isBound && selectedAlbum != null) {
                        Log.d(TAG, "Selected album: "+selectedAlbum);
                        boundService.mediaDownloaderStub.downloadMedia(MediaDownload.newBuilder()
                                .setMediaUrl(etYoutubeUrl.getText().toString())
                                .setAlbumId(((AlbumModel) albumSpinner.getSelectedItem()).getID())
                                .build(), new StreamObserver<DownloadStatus>() {
                            @Override
                            public void onNext(DownloadStatus value) {
                                //downloadModels.add(new YTDLDownload(value));
                                Log.i(TAG, "onNext: "+value);
                            }

                            @Override
                            public void onError(Throwable t) {
                                t.printStackTrace();
                            }

                            @Override
                            public void onCompleted() {}
                        });
                    }
                })
                .create();

        dialog.show();
    }

}
