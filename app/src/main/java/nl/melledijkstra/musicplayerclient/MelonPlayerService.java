package nl.melledijkstra.musicplayerclient;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Locale;

import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.MainActivity;

/**
 * <p>Created by Melle Dijkstra on 18-4-2016</p>
 * TODO: CREATE DOCUMENTATION!!!
 */
public class MelonPlayerService extends Service implements MelonPlayer.StateUpdateListener {

    // BROADCAST MESSAGES
    public static final String CONNECTED = "nl.melledijkstra.musicplayerclient.CONNECTED";
    public static final String DISCONNECTED = "nl.melledijkstra.musicplayerclient.DISCONNECTED";
    public static final String MESSAGERECEIVED = "nl.melledijkstra.musicplayerclient.MESSAGERECEIVED";
    public static final String UPDATE = "nl.melledijkstra.musicplayerclient.UPDATE";
    public static final String CONNECTFAILED = "nl.melledijkstra.musicplayerclient.CONNECTFAILED";
    private static final String TAG = MelonPlayerService.class.getSimpleName();
    private static final int NOTIFICATION_CODE = 5345332;

    // ACTIONS
    private static final String ACTION_PLAY_PAUSE = "nl.melledijkstra.musicplayerclient.ACTION_PLAY_PAUSE";
    private static final String ACTION_PREV = "nl.melledijkstra.musicplayerclient.ACTION_PREV";
    private static final String ACTION_NEXT = "nl.melledijkstra.musicplayerclient.ACTION_NEXT";
    private static final String ACTION_REPEAT = "nl.melledijkstra.musicplayerclient.ACTION_REPEAT";
    private static final String ACTION_SHUFFLE = "nl.melledijkstra.musicplayerclient.ACTION_SHUFFLE";

    private SharedPreferences settings;

    public Socket mSocket;
    // Input and out streams for communication
    protected BufferedReader mIn;
    protected PrintWriter mOut;

    private SocketListeningThread mListeningThread;
    private Handler handler;

    private Integer volumeBeforeCalling;
    private PhoneStateReceiver phoneStateReceiver;

    private final IBinder mBinder = new LocalBinder();

    public class PhoneStateReceiver extends BroadcastReceiver {

        private final String TAG = PhoneStateReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d(TAG, "PHONE STATE CHANGED");
                Log.d(TAG, "intent: "+intent);
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                Log.d(TAG, "State: "+state);
                // Check if phone is ringing
                if(state.equals(TelephonyManager.EXTRA_STATE_RINGING) && App.melonPlayer.getVolume() != -1) {
                    Log.d(TAG, "Incoming call detected, turning down the volume");
                    volumeBeforeCalling = App.melonPlayer.getVolume();
                    // TODO: let user choose to what volume the music player should go on incoming call
                    // UX is everything ;)
                    sendMessage(new MessageBuilder().changeVol(15).build());
                } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE) && volumeBeforeCalling != null) {
                    Log.d(TAG, "Call ended, putting volume back up");
                    sendMessage(new MessageBuilder().changeVol(volumeBeforeCalling).build());
                }
            } catch (Exception e) {
                Log.e(TAG, "PHONE STATE ERROR");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Service - onCreate");
        super.onCreate();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        handler = new Handler(Looper.getMainLooper());
        phoneStateReceiver = new PhoneStateReceiver();
        registerReceiver(phoneStateReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
        if(App.melonPlayer != null) App.melonPlayer.registerStateChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        Log.v(TAG, String.format(Locale.getDefault(), "Service - onStartCommand {action: %s, flags: %d, startId: %d}",action, flags, startId));

        if(action != null) {
            switch (action) {
                case ACTION_PLAY_PAUSE:
                    sendMessage(new MessageBuilder().pause().build());
                    break;
                case ACTION_PREV:
                    sendMessage(new MessageBuilder().previous().build());
                    break;
                case ACTION_NEXT:
                    sendMessage(new MessageBuilder().next().build());
                    break;
            }
        }

        return START_STICKY;
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected() && mOut != null;
    }

    public void sendMessage(JSONObject message) {
        if(!App.DEBUG) {
            if (isConnected()) {
                Log.v(TAG, "Sending: " + message.toString());
                mOut.println(message.toString());
                mOut.flush();
            } else {
                Log.e(TAG, "Not connected, couldn't send message" + message.toString());
                disconnect();
            }
        }
    }

    /**
     * Sends a message to the server but waits for a response.
     * Run this in a new thread to prevent NetworkOnMainThreadException
     * @param message The message to send to the server
     */
    public synchronized String sendMessageWaitResponse(JSONObject message) {
        sendMessage(message);
        try {
            return listen(mIn);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
        return null;
    }

    public void connect() {
        if (!isConnected()) {
            mSocket = new Socket();
            Thread connectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String ip = settings.getString(PreferenceKeys.HOST_IP, MelonPlayer.DEFAULT_IP);
                    int port = settings.getInt(PreferenceKeys.HOST_PORT, MelonPlayer.DEFAULT_PORT);
                    int timeout = settings.getInt(PreferenceKeys.TIMEOUT, MelonPlayer.DEFAULT_TIMEOUT);
                    SocketAddress address = new InetSocketAddress(ip, port);

                    Log.v(TAG, "Connecting to " + ip + ":" + port + " with timeout: " + timeout);
                    try {
                        mSocket.connect(address, timeout);

                        mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                        mOut = new PrintWriter(mSocket.getOutputStream());

                        // CONNECTION ESTABLISHED
                        // create the playback notification
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String message = (App.melonPlayer.getCurrentSong() != null) ? App.melonPlayer.getCurrentSong().getTitle() : "";
                                createPlaybackNotification(message, App.melonPlayer.getState());
                                // startForeground(NOTIFICATION_CODE, createPlaybackNotification());
                            }
                        });
                        // broadcast all over the mobile that we are connected!
                        LocalBroadcastManager.getInstance(MelonPlayerService.this).sendBroadcast((new Intent(CONNECTED)));


                        Log.v(TAG, "Connected to " + ip + ":" + port);

                        mListeningThread = new SocketListeningThread(mIn);
                        mListeningThread.start();

                    } catch (IOException e) {
                        Intent i = new Intent(CONNECTFAILED);
                        i.putExtra("exception",e.getMessage());
                        LocalBroadcastManager.getInstance(MelonPlayerService.this).sendBroadcast(i);
                        Log.i(TAG, "Broadcast send: " + i.toString());
                        Log.i(TAG, "Could not connect to " + ip + " - Exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            connectThread.start();
        }
    }

    /**
     * Disconnects with the server, then broadcasts a message that service disconnected with server so they can react on the event
     */
    public void disconnect() {
        if (!App.DEBUG) {
            Log.v(TAG,"Disconnecting socket!! (DISCONNECT)");
            try {
                if(mSocket != null && mSocket.isConnected()) mSocket.close();
                if(mOut != null) mOut.close();
                if(mIn != null) mIn.close();
                mSocket = null;
                mOut = null;
                mIn = null;
            } catch (IOException e) {
                Log.v(TAG, "Could not dispose mSocket, mOut or mIn - Exception: " + e.getMessage());
            }
        }
        removeNotification();
        // Broadcast that the connection is disconnected to everyone who's listening
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DISCONNECTED));
        stopSelf();
    }

    private void removeNotification() {
        Log.d(TAG,"Removing notification");
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_CODE);
    }

    public String getRemoteIp() {
        if(App.DEBUG) return null;
        if(isConnected())  {
            return mSocket.getRemoteSocketAddress().toString().replace("/", "");
        } else {
            disconnect();
            return null;
        }
    }

    /**
     * Creates a notification with playback options, so playback is possible from outside the app (we have more to do right ;p)
     * @param message Subtext of notification
     * @param state The current state of the MelonPlayer
     */
    private void createPlaybackNotification(String message, MelonPlayer.States state) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

        builder.setSmallIcon(R.drawable.notification_icon)
                .setDeleteIntent(PendingIntent.getService(getApplicationContext(), 63456, new Intent(getApplicationContext(), MelonPlayerService.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // All Actions
                .addAction(R.drawable.ic_shuffle , "Shuffle", generatePendingIntent(ACTION_SHUFFLE))                                // #0
                .addAction(R.drawable.ic_skip_previous, "Prev", generatePendingIntent(ACTION_PREV));                                // #1
                if(state == MelonPlayer.States.PLAYING) {
                    builder.addAction(R.drawable.ic_pause_white_24dp, "Pause", generatePendingIntent(ACTION_PLAY_PAUSE));                      // or #2
                    // Make notification not dismissible when playing music
                    builder.setOngoing(true);
                } else {
                    builder.addAction(R.drawable.ic_action_playback_play_white, "Play", generatePendingIntent(ACTION_PLAY_PAUSE));                  // or #2
                    builder.setOngoing(false);
                }
                builder.addAction(R.drawable.ic_skip_next, "Next", generatePendingIntent(ACTION_NEXT))                              // #3
                .addAction(R.drawable.ic_repeat, "Repeat", generatePendingIntent(ACTION_REPEAT))                                    // #4
                // Set the style to media player style
                .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0,2,4) // Show shuffle, play/pause, and repeat in compact modes
                                .setCancelButtonIntent(PendingIntent.getService(getApplicationContext(), 340, new Intent(getApplicationContext(), MelonPlayerService.class), PendingIntent.FLAG_CANCEL_CURRENT))
                )
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.default_cover))
                .setContentTitle("Melon Music Player")
                .setPriority(NotificationCompat.PRIORITY_MAX);

        builder.setContentText(message);

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent startMainActivityIntent = PendingIntent.getActivity(
                getApplicationContext(),
                MainActivity.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(startMainActivityIntent);

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_CODE, builder.build());
    }

    private PendingIntent generatePendingIntent(String action) {
        Intent intent = new Intent(getApplicationContext(), MelonPlayerService.class);
        intent.setAction(action);

        return PendingIntent.getService(getApplicationContext(),
                NOTIFICATION_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    // This binder gives the service to the binding object
    public class LocalBinder extends Binder {
        public MelonPlayerService getService() {
            return MelonPlayerService.this;
        }
    }

    private synchronized String listen(BufferedReader inputStream) throws IOException {
        return inputStream.readLine();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service - onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "Service - onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, getClass().getSimpleName()+" - onUnBind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service - onDestroy");
        App.melonPlayer.unRegisterStateChangeListener(this);
        unregisterReceiver(phoneStateReceiver);
        disconnect();
        removeNotification();
        super.onDestroy();
    }

    private class SocketListeningThread extends Thread {

        private BufferedReader inputStream;

        public SocketListeningThread(BufferedReader mIn) {
            this.inputStream = mIn;
        }

        @Override
        public void run() {
            Log.v(TAG, "Listening thread started in: " + currentThread().getClass().getSimpleName());
            String inputMsg;
            if (inputStream != null) {
                try {
                    // Keep listening as long as the incoming message isn't null
                    while ((inputMsg = MelonPlayerService.this.listen(mIn)) != null) {
                        Log.v(TAG, "Service - Message received: " + inputMsg);
                        Intent i = new Intent(MESSAGERECEIVED);
                        i.putExtra("msg",inputMsg);
                        LocalBroadcastManager.getInstance(MelonPlayerService.this).sendBroadcast(i);
                    }
                } catch (IOException e) {
                    Log.v(TAG, "Failure when listening/reading from incoming reader in: " + currentThread().getClass().getSimpleName() + " | Exception: "+e.getMessage());
                    e.printStackTrace();
                }
            }
            // Disconnect if this thread closes. Why should the app still be connected if you can't get input anymore?
            disconnect();
        }
    }

    @Override
    public void MelonPlayerStateUpdated() {
        // TODO: update notification only if state changed
        String message = (App.melonPlayer.getCurrentSong() != null) ? App.melonPlayer.getCurrentSong().getTitle() : "";
        createPlaybackNotification(message, App.melonPlayer.getState());
    }

}
