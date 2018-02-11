package nl.melledijkstra.musicplayerclient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import nl.melledijkstra.musicplayerclient.config.Constants;
import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.grpc.MMPResponse;
import nl.melledijkstra.musicplayerclient.grpc.MMPStatus;
import nl.melledijkstra.musicplayerclient.grpc.MMPStatusRequest;
import nl.melledijkstra.musicplayerclient.grpc.MediaControl;
import nl.melledijkstra.musicplayerclient.grpc.MusicPlayerGrpc;
import nl.melledijkstra.musicplayerclient.grpc.PlaybackControl;
import nl.melledijkstra.musicplayerclient.grpc.VolumeControl;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;
import nl.melledijkstra.musicplayerclient.melonplayer.SongModel;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

/**
 * <p>Created by Melle Dijkstra on 18-4-2016</p>
 * This service interacts with the MelonPlayer Server
 */
public class MelonPlayerService extends Service implements MelonPlayer.StateUpdateListener {

    // BROADCAST MESSAGES
    public static final String INITIATE_CONNECTION = "nl.melledijkstra.musicplayerclient.INITIATE_CONNECTION";
    public static final String READY = "nl.melledijkstra.musicplayerclient.READY";
    public static final String DISCONNECTED = "nl.melledijkstra.musicplayerclient.DISCONNECTED";
    public static final String CONNECTFAILED = "nl.melledijkstra.musicplayerclient.CONNECTFAILED";
    private static final String TAG = MelonPlayerService.class.getSimpleName();

    // ACTIONS
    private static final String ACTION_PLAY_PAUSE = "nl.melledijkstra.musicplayerclient.ACTION_PLAY_PAUSE";
    private static final String ACTION_PREV = "nl.melledijkstra.musicplayerclient.ACTION_PREV";
    private static final String ACTION_NEXT = "nl.melledijkstra.musicplayerclient.ACTION_NEXT";
    private static final String ACTION_REPEAT = "nl.melledijkstra.musicplayerclient.ACTION_REPEAT";
    private static final String ACTION_SHUFFLE = "nl.melledijkstra.musicplayerclient.ACTION_SHUFFLE";
    private static final String ACTION_CLOSE = "nl.melledijksta.melonmusicplayer.ACTION_CLOSE";

    private static final int NOTIFICATION_ID = 955;

    private static final String CHANNEL_ID = "nl.melledijkstra.melonmusicplayer.FOREGROUND_NOTIFICATION";

    // BROADCASTS
    private Integer volumeBeforeCalling;
    private PhoneStateReceiver phoneStateReceiver;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case INITIATE_CONNECTION:
                        // get current ip and port settings
                        String musicPlayerIp = mSettings.getString(PreferenceKeys.HOST_IP, null);
                        int musicPlayerPort = mSettings.getInt(PreferenceKeys.HOST_PORT, Constants.DEFAULT_PORT);
                        // if ip is the same, then don't create new channel
                        if (!previousIp.equals(musicPlayerIp) || channel == null) {
                            setupGrpc(musicPlayerIp, musicPlayerPort);
                        }
                        previousIp = musicPlayerIp;
                        Log.i(TAG, "onReceive: initiating connection with grpc server");
                        initiateConnection(true);
                }
            }
        }
    };

    private volatile MelonPlayer melonPlayer;

    // Application settings
    private SharedPreferences mSettings;
    private String previousIp;

    private final IBinder mServiceBinder = new LocalBinder();

    // NOTIFICATION
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Notification musicPlaybackNotification;

    // gRPC FIELDS
    /**
     * Managed channel where connection with MusicPlayer server lies
     */
    private ManagedChannel channel;

    /**
     * Stub to initiate calls to server
     */
    public volatile MusicPlayerGrpc.MusicPlayerStub musicPlayerStub;

    /**
     * Flag if connection result should be broadcasted
     */
    private volatile boolean broadcastConnectionResult;

    private Runnable grpcStateChangeListener = new Runnable() {
        @Override
        public void run() {
            if (channel != null) {
                ConnectivityState state = channel.getState(false);
                Log.i(TAG, "notifyWhenStateChanged: STATE CHANGED: " + state.toString());
                channel.notifyWhenStateChanged(state, grpcStateChangeListener);
                switch (state) {
                    case READY:
                        connected();
                        break;
                    case CONNECTING:
                        break;
                    case IDLE:
                    case TRANSIENT_FAILURE:
                    default:
                        if (broadcastConnectionResult) {
                            Intent intent = new Intent(CONNECTFAILED);
                            intent.putExtra("state", state.toString());
                            LocalBroadcastManager.getInstance(MelonPlayerService.this).sendBroadcast(intent);
                            Log.i(TAG, "initiateConnection: CONNECTFAILED broadcast sent");
                            broadcastConnectionResult = false;
                        }
                        break;
                    case SHUTDOWN:
                        disconnect();
                        break;
                }
            }
        }
    };

    private StreamObserver<MMPStatus> statusStreamObserver = new StreamObserver<MMPStatus>() {
        @Override
        public void onNext(MMPStatus newState) {
            melonPlayer.setState(newState);
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "onCompleted: new Status received");
        }
    };

    /**
     * Default response stream observer
     */
    public StreamObserver<MMPResponse> defaultMMPResponseStreamObserver = new StreamObserver<MMPResponse>() {
        @Override
        public void onNext(MMPResponse response) {
            if (response.getResult() == MMPResponse.Result.ERROR
                    && !response.getError().isEmpty()) {
                Log.e(TAG, "GRPC SERVER: " + response.getError());
            }
            if (!response.getMessage().isEmpty()) {
                Toast.makeText(MelonPlayerService.this, response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "onCompleted: Call completed");
        }
    };
    ;

    @Override
    public void onCreate() {
        Log.v(TAG, "Service - onCreate");
        super.onCreate();
        // retrieve application settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        melonPlayer = new MelonPlayer();

        // register broadcast receivers
        phoneStateReceiver = new PhoneStateReceiver();
        registerReceiver(phoneStateReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
        // using LocalBroadcastManager only accepts broadcast from this application
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(INITIATE_CONNECTION));

        String musicPlayerIp = mSettings.getString(PreferenceKeys.HOST_IP, null);
        int musicPlayerPort = mSettings.getInt(PreferenceKeys.HOST_PORT, Constants.DEFAULT_PORT);

        if (musicPlayerIp != null) {
            setupGrpc(musicPlayerIp, musicPlayerPort);
            previousIp = musicPlayerIp;
            initiateConnection(false);
        } else {
            previousIp = "";
        }

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        Log.v(TAG, String.format(Locale.getDefault(), "Service - onStartCommand {action: %s, flags: %d, startId: %d}", action, flags, startId));

        if (action != null) {
            switch (action) {
                case ACTION_PLAY_PAUSE:
                    musicPlayerStub.play(MediaControl.newBuilder()
                            .setState(MediaControl.State.PAUSE)
                            .build(), defaultMMPResponseStreamObserver);
                    break;
                case ACTION_PREV:
                    musicPlayerStub.previous(PlaybackControl.getDefaultInstance(), defaultMMPResponseStreamObserver);
                    break;
                case ACTION_NEXT:
                    musicPlayerStub.next(PlaybackControl.getDefaultInstance(), defaultMMPResponseStreamObserver);
                    break;
                case ACTION_CLOSE:
                    stopForeground(false);
                    stopSelf();
            }
        }

        return START_NOT_STICKY;
    }

    /**
     * Sets up the all the necessary gRPC components
     *
     * @param ip   The ip to connect to
     * @param port The port of the gRPC service
     */
    private void setupGrpc(String ip, int port) {
        // If a channel still exists, close it first
        if (channel != null) {
            try {
                channel.shutdownNow();
                channel.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // create communication with MusicPlayerServer
        Log.i(TAG, "GRPC: Connecting to " + ip + ":" + port);
        channel = ManagedChannelBuilder.forAddress(ip, port)
                .usePlaintext(true)
                .build();
        ConnectivityState state = channel.getState(false);
        Log.i(TAG, "setupGrpc: current state: " + state.toString());
        channel.notifyWhenStateChanged(state, grpcStateChangeListener);
        musicPlayerStub = MusicPlayerGrpc.newStub(channel);
    }

    /**
     * Disconnects with the server, then broadcasts a message that service disconnected with server so they can react on the event
     */
    public void disconnect() {
        if (channel != null &&
                !channel.getState(false).equals(ConnectivityState.SHUTDOWN)) {
            try {
                Log.i(TAG, "onDestroy: shutting down grpc client");
                channel.shutdownNow();
                if (channel != null && !channel.isTerminated()) channel.awaitTermination(1, TimeUnit.SECONDS);
                Log.i(TAG, "onDestroy: grpc client terminated");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                channel = null;
                musicPlayerStub = null;
            }
        }
        removeNotification();
        // Broadcast that the connection is disconnected to everyone who's listening
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DISCONNECTED));
    }

    /**
     * Initiate a connection with gRPC server
     *
     * @param broadcast If the result should be broadcasted
     */
    public void initiateConnection(boolean broadcast) {
        // request connection and get the state of gRPC channel
        broadcastConnectionResult = broadcast;
        channel.getState(true);
    }

    /**
     * Once the application is connected with gRPC server this runs
     */
    private void connected() {
        // if connection state is ready (connected) then send broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(READY));
        showNotification();
        Log.i(TAG, "initiateConnection: READY broadcast sent");
        musicPlayerStub.registerMMPNotify(MMPStatusRequest.getDefaultInstance(), new StreamObserver<MMPStatus>() {
            @Override
            public void onNext(MMPStatus status) {
                melonPlayer.setState(status);
                if(status.getState() == MMPStatus.State.PLAYING) {
                    startForeground(NOTIFICATION_ID, musicPlaybackNotification);
                } else {
                    stopForeground(false);
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.i(TAG, "registerMMPNotify: " + t.getMessage());
                disconnect();
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted: Status call done");
            }
        });
    }

    private void showNotification() {
        if (musicPlaybackNotification == null) {
            musicPlaybackNotification = generatePlaybackNotification("Test", MelonPlayer.States.NOTHINGSPECIAL);
        }
        notificationManager.notify(NOTIFICATION_ID, musicPlaybackNotification);
    }

    /**
     * Check if device is still connected with gRPC server
     */
    public boolean isConnected() {
        return channel != null && channel.getState(false).equals(ConnectivityState.READY);
    }

    /**
     * checkConnection checks the current gRPC connection state and reacts accordingly
     * run this method to check connection and send broadcasts when there is a change
     */
    public void checkConnection() {
        // TODO: create this functionality
        throw new UnsupportedOperationException("Not implemented");
    }

    private void removeNotification() {
        Log.i(TAG, "Removing notification");
        if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Creates a notification with playback options, so playback is possible from outside the app (we have more to do right ;p)
     *
     * @param message Subtext of notification
     * @param state   The current state of the MelonPlayer
     */
    private Notification generatePlaybackNotification(String message, MelonPlayer.States state) {
        notificationBuilder.setSmallIcon(R.drawable.notification_icon)
                .setDeleteIntent(generatePendingIntent(ACTION_CLOSE))
                // make notification available on lock screen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                // All Actions
                .addAction(R.drawable.ic_shuffle, "Shuffle", generatePendingIntent(ACTION_SHUFFLE)) // #0
                .addAction(R.drawable.ic_skip_previous, "Prev", generatePendingIntent(ACTION_PREV));
        if (state == MelonPlayer.States.PLAYING) {
            notificationBuilder.addAction(R.drawable.ic_pause_white_24dp, "Pause", generatePendingIntent(ACTION_PLAY_PAUSE)); // #2
        } else {
            notificationBuilder.addAction(R.drawable.ic_action_playback_play_white, "Play", generatePendingIntent(ACTION_PLAY_PAUSE)); // #2
        }
        notificationBuilder.addAction(R.drawable.ic_skip_next, "Next", generatePendingIntent(ACTION_NEXT)) // #3
                .addAction(R.drawable.ic_repeat, "Repeat", generatePendingIntent(ACTION_REPEAT)) // #4
                // Set the style to media player style
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(generatePendingIntent(ACTION_CLOSE))
                        .setShowActionsInCompactView(0, 2, 4) // Show shuffle, play/pause, and repeat in compact modes
                )
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.default_cover))
                .setContentTitle("Melon Music Player")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(message);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        // Start MainActivity when user clicks on content
        PendingIntent startMainActivityIntent = PendingIntent.getActivity(
                this,
                MainActivity.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentIntent(startMainActivityIntent);

        return notificationBuilder.build();
    }

    /**
     * Quick shortcut for creating a pending intent
     *
     * @param action The action for the pending intent
     * @return The pending intent
     */
    private PendingIntent generatePendingIntent(String action) {
        Intent intent = new Intent(this, MelonPlayerService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service - onDestroy");
        stopForeground(true);
        //App.melonPlayer.unRegisterStateChangeListener(this);
        unregisterReceiver(phoneStateReceiver);
        disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mServiceBinder;
    }

    @Override
    public void MelonPlayerStateUpdated() {
        // TODO: update notification only if state changed
        SongModel currentSongModel = melonPlayer.getCurrentSongModel();
        String notificationTitle = (currentSongModel != null) ? currentSongModel.getTitle() : "";
        updateNotification(notificationTitle, melonPlayer.getState());
    }

    /**
     * Updates the notitifation with new information
     *
     * @param newTitle The new title of the notification
     * @param state    The state the music player is currently in
     */
    private void updateNotification(String newTitle, @Nullable MelonPlayer.States state) {
        if (state != null) {
            if (state == MelonPlayer.States.PLAYING) {
                // TODO: set to pause button
            } else {
                // TODO: set to play button
            }
        }
        notificationBuilder.setContentTitle(newTitle);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public MelonPlayer getMelonPlayer() {
        return melonPlayer;
    }

    public void retrieveNewStatus() {
        musicPlayerStub.retrieveMMPStatus(MMPStatusRequest.getDefaultInstance(), statusStreamObserver);
    }

    // This binder gives the service to the binding object
    public class LocalBinder extends Binder {
        public MelonPlayerService getService() {
            return MelonPlayerService.this;
        }
    }

    // TODO: merge into single receiver!
    class PhoneStateReceiver extends BroadcastReceiver {

        private final String TAG = PhoneStateReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d(TAG, "PHONE STATE CHANGED");
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                Log.d(TAG, "State: " + state);
                // Check if phone is ringing
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && melonPlayer.getVolume() != -1) {
                    Log.d(TAG, "Incoming call detected, turning down the volume"); // UX is everything ;)
                    volumeBeforeCalling = melonPlayer.getVolume();
                    // TODO: let user choose to what volume the music player should go on incoming call
                    musicPlayerStub.changeVolume(VolumeControl.newBuilder().setVolumeLevel(15).build(), defaultMMPResponseStreamObserver);
                } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE) && volumeBeforeCalling != null) {
                    // Change back to original volume when user is done calling
                    musicPlayerStub.changeVolume(VolumeControl.newBuilder().setVolumeLevel(volumeBeforeCalling).build(), defaultMMPResponseStreamObserver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
