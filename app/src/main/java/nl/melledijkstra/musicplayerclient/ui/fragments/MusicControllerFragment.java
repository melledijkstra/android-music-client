package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.grpc.MediaControl;
import nl.melledijkstra.musicplayerclient.grpc.PlaybackControl;
import nl.melledijkstra.musicplayerclient.grpc.PositionControl;
import nl.melledijkstra.musicplayerclient.grpc.VolumeControl;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MusicControllerFragment extends ServiceBoundFragment implements MelonPlayer.StateUpdateListener {

    private static final String TAG = MusicControllerFragment.class.getSimpleName();
    private static int numberOfFrags = 0;

    @BindView(R.id.sbMusicTime)
    SeekBar sbMusicTime;
    // not managed by ButterKnife
    SeekBar sbVolume;
    @BindView(R.id.btnPreviousSong)
    ImageButton btnPrev;
    @BindView(R.id.btnPlayPause)
    ImageButton btnPlayPause;
    @BindView(R.id.btnNextSong)
    ImageButton btnNext;
    @BindView(R.id.btnChangeVolume)
    ImageButton btnChangeVolume;
    @BindView(R.id.tvCurrentSong)
    TextView tvCurrentSong;
    @BindView(R.id.tvSongCurPos)
    TextView tvCurPos;
    @BindView(R.id.tvSongDuration)
    TextView tvSongDuration;
    private Unbinder unbinder;

    Timer timer;

    boolean isDragging;

    AlertDialog volumeDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        numberOfFrags++;
        Log.d(TAG, "There are currently " + numberOfFrags + " control fragments running");

        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.change_volume_dialog, null);
        // not managed by ButterKnife because it resides inside a dialog
        sbVolume = (SeekBar) dialogView.findViewById(R.id.sbVolume);

        volumeDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.change_volume)
                .setView(dialogView)
                .setOnKeyListener((dialog, keyCode, event) -> event.getAction() == KeyEvent.ACTION_DOWN && getActivity().onKeyDown(keyCode, event)).create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_music_controller, container, false);
        unbinder = ButterKnife.bind(this, layout);

        // get views
        sbMusicTime.setOnSeekBarChangeListener(onMusicTimeSeekbarChange);
        sbVolume.setOnSeekBarChangeListener(onVolumeSeekbarChange);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG, "Fragment created");
    }

    @Override
    protected void onBounded() {
        super.onBounded();
        boundService.getMelonPlayer().registerStateChangeListener(this);
        boundService.retrieveNewStatus();
    }

    @Override
    protected void onUnbound() {
        super.onUnbound();
        boundService.getMelonPlayer().unRegisterStateChangeListener(this);
    }

    @OnClick(R.id.btnPlayPause)
    public void onPlayPauseClick() {
        if (isBound) {
            boundService.musicPlayerStub.play(MediaControl.newBuilder()
                    .setState(MediaControl.State.PAUSE).build(), boundService.defaultMMPResponseStreamObserver);
        }
    }

    @OnClick(R.id.btnPreviousSong)
    public void onPreviousSongClick() {
        if (isBound) {
            boundService.musicPlayerStub.previous(PlaybackControl.getDefaultInstance(), boundService.defaultMMPResponseStreamObserver);
        }
    }

    @OnClick(R.id.btnNextSong)
    public void onNextSongClick() {
        if (isBound) {
            boundService.musicPlayerStub.next(PlaybackControl.getDefaultInstance(), boundService.defaultMMPResponseStreamObserver);
        }
    }

    @OnClick(R.id.btnChangeVolume)
    public void onChangeVolumeClick() {
        if (isBound && sbVolume != null) {
            sbVolume.setProgress(boundService.getMelonPlayer().getVolume());
        }
        volumeDialog.show();
    }

    private SeekBar.OnSeekBarChangeListener onVolumeSeekbarChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && progress % 3 == 0) {
                if (isBound) {
                    boundService.musicPlayerStub.changeVolume(VolumeControl.newBuilder()
                            .setVolumeLevel(progress)
                            .build(), boundService.defaultMMPResponseStreamObserver);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (isBound) {
                boundService.musicPlayerStub.changeVolume(VolumeControl.newBuilder()
                        .setVolumeLevel(seekBar.getProgress())
                        .build(), boundService.defaultMMPResponseStreamObserver);
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener onMusicTimeSeekbarChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                SongModel curSongModel = boundService.getMelonPlayer().getCurrentSongModel();
                if (curSongModel != null) {
                    long time = Math.round(curSongModel.getDuration() * (progress / 100f));
                    tvCurPos.setText(Utils.secondsToDurationFormat(time));
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isDragging = false;
            boundService.musicPlayerStub.changePosition(PositionControl.newBuilder()
                    .setPosition(seekBar.getProgress())
                    .build(), boundService.defaultMMPResponseStreamObserver);
        }
    };

    /**
     * Starts the timer which updates the song current time progress
     */
    private void startTimerIfPlaying() {
        if (isBound &&
                timer == null &&
                boundService.getMelonPlayer().getState() == MelonPlayer.States.PLAYING) {
            Log.d(TAG, "Starting status update timer");
            timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (isBound) {
                        boundService.retrieveNewStatus();
                    }
                }
            };
            timer.schedule(timerTask, 0, 950);
        }
    }

    /**
     * Stops the timer progress
     */
    private void stopTimer() {
        if (timer != null) {
            Log.d(TAG, "Stopping update timer");
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (boundService != null) {
            boundService.getMelonPlayer().unRegisterStateChangeListener(this);
        }
        if (volumeDialog.isShowing()) {
            volumeDialog.dismiss();
        }
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void MelonPlayerStateUpdated() {
        // This call can be invoked from another thread
        // So make sure we are update UI on the UI thread
        getActivity().runOnUiThread(() -> {
            MelonPlayer melonPlayer = boundService.getMelonPlayer();
            int curPosition = Math.round(melonPlayer.getSongPosition());
            // Don't update the music time seekbar when user is dragging
            if (curPosition > 0 && !isDragging) {
                sbMusicTime.setProgress(curPosition);
            } else {
                sbMusicTime.setProgress(0);
            }

            if (melonPlayer.getState() == MelonPlayer.States.PLAYING) {
                // TODO: make svg animation
                btnPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
                startTimerIfPlaying();
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_action_playback_play_white);
                stopTimer();
            }

            SongModel currentSongModel = melonPlayer.getCurrentSongModel();

            if (currentSongModel != null && currentSongModel.getTitle() != null) {
                tvCurrentSong.setText(currentSongModel.getTitle());
                tvCurrentSong.setVisibility(View.VISIBLE);
            } else {
                tvCurrentSong.setText("-");
                tvCurrentSong.setVisibility(View.GONE);
            }

            if (currentSongModel != null && currentSongModel.getDuration() > 0) {
                tvSongDuration.setText(Utils.secondsToDurationFormat(currentSongModel.getDuration()));
            } else {
                tvSongDuration.setText(R.string.duration_default_text);
            }

            long elapsedTime = melonPlayer.getElapsedTime();
            if (elapsedTime > 0 && !isDragging) {
                tvCurPos.setText(Utils.secondsToDurationFormat(elapsedTime));
            } else {
                tvCurPos.setText(R.string.duration_default_text);
            }

            if (sbVolume != null) {
                sbVolume.setProgress(melonPlayer.getVolume());
            }

            if (melonPlayer.getState() == MelonPlayer.States.STOPPED ||
                    melonPlayer.getState() == MelonPlayer.States.ENDED) {
                tvCurPos.setText(R.string.duration_default_text);
                tvSongDuration.setText(R.string.duration_default_text);
                tvCurrentSong.setText("-");
                tvCurrentSong.setVisibility(View.GONE);
                sbMusicTime.setProgress(0);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimerIfPlaying();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopTimer();
    }
}
