package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;
import nl.melledijkstra.musicplayerclient.melonplayer.Song;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class MusicControllerFragment extends ServiceBoundFragment implements MelonPlayer.StateUpdateListener {

    private static final String TAG = MusicControllerFragment.class.getSimpleName();
    private static int numberOfFrags = 0;

    SeekBar sbMusicTime, sbVolume;
    ImageButton btnPrev, btnPlayPause, btnNext, btnChangeVolume;
    TextView tvCurrentSong, tvCurPos, tvSongDuration;
    Timer timer;

    boolean isDragging;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.melonPlayer.registerStateChangeListener(this);
        numberOfFrags++;
        Log.d(TAG, "There are currently "+numberOfFrags+" control fragments running");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG,"Fragment created");
    }

    @Override
    protected void onBounded() {
        super.onBounded();
        boundService.sendMessage(new MessageBuilder().status().build());
    }

    private View.OnClickListener onPlayPauseClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendMessageIfBound(new MessageBuilder().pause().build());
        }
    };

    private View.OnClickListener onPreviousClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendMessageIfBound(new MessageBuilder().previous().build());
        }
    };

    private View.OnClickListener onNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendMessageIfBound(new MessageBuilder().next().build());
        }
    };

    private View.OnClickListener onChangeVolumeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View dialogView = getActivity().getLayoutInflater().inflate(R.layout.change_volume_dialog, null);
            sbVolume = (SeekBar) dialogView.findViewById(R.id.sbVolume);
            sbVolume.setProgress(App.melonPlayer.getVolume());
            sbVolume.setOnSeekBarChangeListener(onVolumeSeekbarChange);

            new AlertDialog.Builder(getContext())
                    .setTitle("Change Volume")
                    .setView(dialogView)
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            return event.getAction() == KeyEvent.ACTION_DOWN && getActivity().onKeyDown(keyCode, event);
                        }
                    })
                    .show();
        }
    };

    private SeekBar.OnSeekBarChangeListener onVolumeSeekbarChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser && progress % 3 == 0) {
                sendMessageIfBound(new MessageBuilder().changeVol(progress).build());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            sendMessageIfBound(new MessageBuilder().changeVol(seekBar.getProgress()).build());
        }
    };

    private SeekBar.OnSeekBarChangeListener onMusicTimeSeekbarChange = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                Song curSong = App.melonPlayer.getCurrentSong();
                if(curSong != null) {
                    long time = Math.round(curSong.getDuration() * (progress / 100f));
                    tvCurPos.setText(Utils.millisecondsToDurationFormat(time));
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
            boundService.sendMessage(new MessageBuilder().changePos(seekBar.getProgress()).build());
        }
    };

    private void startTimer() {
        if(timer == null && App.melonPlayer.getState() == MelonPlayer.States.PLAYING) {
            Log.d(TAG, "Starting status update timer");
            timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if(isBound) {
                        boundService.sendMessage(new MessageBuilder().status().build());
                    }
                }
            };
            timer.schedule(timerTask, 0, 950);
        }
    }

    private void stopTimer() {
        if(timer != null) {
            Log.d(TAG, "Stopping update timer");
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_music_controller, container, false);

        // get views
        sbMusicTime = (SeekBar) layout.findViewById(R.id.sbMusicTime);
        sbMusicTime.setOnSeekBarChangeListener(onMusicTimeSeekbarChange);

        btnPrev = (ImageButton) layout.findViewById(R.id.btnPreviousSong);
        btnPrev.setOnClickListener(onPreviousClick);
        btnPlayPause = (ImageButton) layout.findViewById(R.id.btnPlayPause);
        btnPlayPause.setOnClickListener(onPlayPauseClick);
        btnNext = (ImageButton) layout.findViewById(R.id.btnNextSong);
        btnNext.setOnClickListener(onNextClick);
        btnChangeVolume = (ImageButton) layout.findViewById(R.id.btnChangeVolume);
        btnChangeVolume.setOnClickListener(onChangeVolumeClick);

        tvCurrentSong = (TextView) layout.findViewById(R.id.tvCurrentSong);
        tvCurPos = (TextView) layout.findViewById(R.id.tvSongCurPos);
        tvSongDuration = (TextView) layout.findViewById(R.id.tvSongDuration);

        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.melonPlayer.unRegisterStateChangeListener(this);
        Log.d(TAG, "Fragment destroyed");
    }

    @Override
    public void MelonPlayerStateUpdated() {
        int curPosition = Math.round(App.melonPlayer.getSongPosition());
        // Don't update the music time seekbar when user is dragging
        if (curPosition > 0 && !isDragging) {
            sbMusicTime.setProgress(curPosition);
        } else {
            sbMusicTime.setProgress(0);
        }

        if(App.melonPlayer.getState() == MelonPlayer.States.PLAYING) {
            // TODO: make svg animation
            btnPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
            startTimer();
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_action_playback_play_white);
            stopTimer();
        }

        Song currentSong = App.melonPlayer.getCurrentSong();

        if(currentSong != null && currentSong.getTitle() != null) {
            tvCurrentSong.setText(currentSong.getTitle());
            tvCurrentSong.setVisibility(View.VISIBLE);
        } else {
            tvCurrentSong.setText("-");
            tvCurrentSong.setVisibility(View.GONE);
        }

        if(currentSong != null && currentSong.getDuration() > 0) {
            tvSongDuration.setText(Utils.millisecondsToDurationFormat(currentSong.getDuration()));
        } else {
            tvSongDuration.setText("00:00:00");
        }

        long currentTime = App.melonPlayer.getCurrentTime();
        if(currentTime > 0) {
            tvCurPos.setText(Utils.millisecondsToDurationFormat(currentTime));
        } else {
            tvCurPos.setText("00:00:00");
        }

        if(sbVolume != null) {
            sbVolume.setProgress(App.melonPlayer.getVolume());
        }

        if(App.melonPlayer.getState() == MelonPlayer.States.STOPPED ||
                App.melonPlayer.getState() == MelonPlayer.States.ENDED) {
            tvCurPos.setText("00:00:00");
            tvSongDuration.setText("00:00:00");
            tvCurrentSong.setText("");
            tvCurrentSong.setVisibility(View.GONE);
            sbMusicTime.setProgress(0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimer();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopTimer();
    }
}
