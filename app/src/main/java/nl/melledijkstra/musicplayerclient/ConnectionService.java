package nl.melledijkstra.musicplayerclient;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import nl.melledijkstra.musicplayerclient.Config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.Models.MusicClient;

/**
 * <p>Created by Melle Dijkstra on 18-4-2016</p>
 * TODO: CREATE DOCUMENTATION!!!
 */
public class ConnectionService extends Service {

    // BROADCAST MESSAGES
    public static final String CONNECTED = "nl.melledijkstra.musicplayerclient.CONNECTED";
    public static final String DISCONNECTED = "nl.melledijkstra.musicplayerclient.DISCONNECTED";
    public static final String MESSAGERECEIVED = "nl.melledijkstra.musicplayerclient.MRECEIVED";
    public static final String UPDATE = "nl.melledijkstra.musicplayerclient.UPDATE";
    public static final String CONNECTFAILED = "nl.melledijkstra.musicplayerclient.CONNECTFAILED";

    private SharedPreferences settings;

    public Socket mSocket;
    // Input and out streams for communication
    protected BufferedReader mIn;
    protected PrintWriter mOut;

    private SocketListeningThread mListeningThread;

    private final IBinder mBinder = new LocalBinder();

    NotificationManager notifyman;

    @Override
    public void onCreate() {
        Log.v(App.TAG, "Service - onCreate");

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        notifyman = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(App.TAG, "Service - onStartCommand");
        // Check if already connected
        return START_NOT_STICKY;
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected() && mOut != null;
    }

    public void sendMessage(JSONObject message) {
        if (isConnected()) {
            Log.v(App.TAG,"Sending: "+message.toString());
            mOut.println(message.toString());
            mOut.flush();
        } else {
            Log.e(App.TAG, "Not connected, couldn't send message" + message.toString());
            disconnect();
        }
    }

    public void connect() {
        if (!isConnected()) {
            mSocket = new Socket();
            Thread connectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String ip = settings.getString(PreferenceKeys.HOST_IP, MusicClient.DEFAULT_IP);
                    int port = settings.getInt(PreferenceKeys.HOST_PORT, MusicClient.DEFAULT_PORT);
                    int timeout = settings.getInt(PreferenceKeys.TIMEOUT, MusicClient.DEFAULT_TIMEOUT);
                    SocketAddress address = new InetSocketAddress(ip, port);

                    Log.v(App.TAG, "Connecting to " + ip + ":" + port + " with timeout: " + timeout);
                    try {
                        mSocket.connect(address, timeout);

                        mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                        mOut = new PrintWriter(mSocket.getOutputStream());

                        LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast((new Intent(CONNECTED)));

                        Log.v(App.TAG, "Connected to " + ip + ":" + port);

                        mListeningThread = new SocketListeningThread(mIn);
                        mListeningThread.start();

                    } catch (IOException e) {
                        Intent i = new Intent(CONNECTFAILED);
                        i.putExtra("exception",e.getMessage());
                        LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast(i);
                        Log.i(App.TAG, "Broadcast send: " + i.toString());
                        Log.i(App.TAG, "Could not connect to " + ip + " - Exception: " + e.getMessage());
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
        if (mSocket != null && mSocket.isConnected()) {
            Log.v(App.TAG,"Disconnecting socket!! (DISCONNECT)");
            try {
                mSocket.close();
                mOut.close();
                mIn.close();
                mSocket = null;
                mOut = null;
                mIn = null;
            } catch (IOException e) {
                Log.v(App.TAG, "Could not dispose mSocket, mOut or mIn - Exception: " + e.getMessage());
            }
        }
        // Broadcast that the connection is disconnected to everyone who's listening
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DISCONNECTED));
        stopSelf();
    }

    public void sendDebugMessage(String msg) {
        if(isConnected()) {
            mOut.println(msg);
            mOut.flush();
            Log.v(App.TAG,"Message '"+msg+"' send");
        } else {
            Log.v(App.TAG,"not connected");
        }
    }

    public String getRemoteIp() {
        if(isConnected())  {
            return mSocket.getRemoteSocketAddress().toString().replace("/", "");
        } else {
            disconnect();
            return null;
        }
    }

    // This binder gives the service to the binding object
    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(App.TAG, "Service - onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(App.TAG, "Service - onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(App.TAG, getClass().getSimpleName()+" - onUnBind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(App.TAG, "Service - onDestroy");
        Toast.makeText(this, "Service - Destroyed", Toast.LENGTH_SHORT).show();
        disconnect();
        super.onDestroy();
    }

    private class SocketListeningThread extends Thread {

        private BufferedReader inputStream;

        public SocketListeningThread(BufferedReader mIn) {
            this.inputStream = mIn;
        }

        @Override
        public void run() {
            Log.v(App.TAG, "Listening thread started in: " + currentThread().getClass().getSimpleName());
            String inputMsg;
            if (inputStream != null) {
                try {
                    while ((inputMsg = inputStream.readLine()) != null) {
                        Log.v(App.TAG, "Message received: " + inputMsg);
                        Intent i = new Intent(MESSAGERECEIVED);
                        i.putExtra("msg",inputMsg);
                        LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast(i);
                        Log.v(App.TAG,"BROADCAST send: "+i.toString());
                    }
                } catch (IOException e) {
                    Log.v(App.TAG, "Failure when listening/reading from incoming reader in: " + currentThread().getClass().getSimpleName() + " | Exception: "+e.getMessage());
                    e.printStackTrace();
                }
            }
            // Disconnect is this thread closes. Why should you still be connected if you can't get input anymore?
            disconnect();
        }
    }
}
