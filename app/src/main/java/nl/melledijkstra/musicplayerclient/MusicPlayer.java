package nl.melledijkstra.musicplayerclient;

import android.graphics.drawable.Drawable;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * Created by Melle Dijkstra on 24-3-2016.
 */
public class MusicPlayer {

    // variable to check if socket is connected
    private boolean connected = false;

    // The activity for updating UI
    private MainActivity context;

    private Thread conThread;
    private boolean isConnectionThreadRunning = false;

    Socket socket;

    PrintWriter out = null;
    BufferedReader in = null;

    /**
     * Constructor
     * @param context The application context (Activity)
     */
    public MusicPlayer(MainActivity context) {
        this.context = context;
        conThread = new Thread(new ConnectionThread());
    }

    /**
     * Create connection with python server
     */
    public void connect() {
        // check if thread is already running
        // if not start the thread for connection
        if(!isConnectionThreadRunning) {
            conThread = new Thread(new ConnectionThread());
            conThread.start();
        } else {
            Toast.makeText(context, "Connection thread is already running", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkConnection() {
        if(isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.setSoTimeout(5000);
                        out.println(Commands.PING);
                        in.readLine();
                        Log.i(MainActivity.TAG,"Still connected");
                        socket.setSoTimeout(0);
                    } catch(SocketTimeoutException e) {
                        // Socket connection didn't respond after an amount of seconds, so aborting connection
                        Log.i(MainActivity.TAG,"Ping command took to long, probably because server is down");
                        disconnect();
                    } catch(IOException e) {
                        Log.i(MainActivity.TAG,"Error checking connection: "+e.toString());
                        disconnect();
                    }
                }
            }).start();
        } else {
            Log.i(MainActivity.TAG, "Not checking connection, because not connected yet");
        }
    }

    /**
     * Connection thread for making the connection with python server and keep constant connection
     */
    public class ConnectionThread implements Runnable {

        @Override
        public void run() {
            isConnectionThreadRunning = true;
            try {
                Log.i(MainActivity.TAG, "Connection Thread running");
                socket = new Socket();
                // try connecting to socket with timeout
                socket.connect(new InetSocketAddress(Config.HOST, Config.PORT), Config.TIMEOUT);
                connected = true;

                ToastOnMainThread("Connected to "+Config.HOST);

                // Update toolbar icon with connected
                updateUI((ActionMenuItemView)context.toolbar.findViewById(R.id.action_connect),
                        context.getResources().getDrawable(R.drawable.connected));

                // get input and output streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            } catch(IOException e) {
                Log.i(MainActivity.TAG,e.getMessage());
                Log.i(MainActivity.TAG, "Connect failed with '" + Config.HOST + "'");
                if(e.getClass() == SocketTimeoutException.class) {
                    ToastOnMainThread("Can't connect (timeout: "+Config.TIMEOUT+")");
                }
                connected = false;
                updateUI((ActionMenuItemView) context.toolbar.findViewById(R.id.action_connect),
                        context.getResources().getDrawable(android.R.drawable.stat_sys_warning));
            }
            isConnectionThreadRunning = false;
        }

    }

    public void disconnect() {
        try {
            out.flush();
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            ToastOnMainThread("Could not close socket");
            e.printStackTrace();
        }
        connected = false;
        Log.i(MainActivity.TAG, "Connection closed");
        updateUI((ActionMenuItemView) context.toolbar.findViewById(R.id.action_connect),
                context.getResources().getDrawable(android.R.drawable.stat_sys_warning));
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * The different types of commands to execute on the server
     */
    public enum Commands {
        LIST,
        QUIT,
        HELLO,
        PING, OPTIONS
    }

    /**
     * Refreshes the music ListView
     */
    public void refreshMusicList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get list from socket connection
                String serverSongList = sendMessageForResponse(Commands.LIST.toString());
                String[] serverSongListArray = new String[0];
                if (serverSongList != null) {
                    serverSongListArray = serverSongList.split(":");
                }
                final String[] finalServerSongListArray = serverSongListArray;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Set the server song list to the ListView
                        context.songList.addAll(Arrays.asList(finalServerSongListArray));
                        // Tell the adapter that info changed
                        context.musicAdapter.notifyDataSetChanged();
                        // stop the refreshing animation which was (hopefully) called before this method
                        context.refreshSwipeLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    public void sendMessageResponseToToast(final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get list from socket connection
                String response = sendMessageForResponse(msg);
                if(response != null) {
                    ToastOnMainThread(response);
                }
            }
        }).start();
    }

    private String sendMessageForResponse(final String msg) {
        final String[] response = {null};
        if(isConnected()) {
            try{
                // send message
                Log.i(MainActivity.TAG, "Sending: " + msg);
                out.println(msg);
                Log.i(MainActivity.TAG, "Message send, waiting for response...");
                // get message and return
                response[0] = in.readLine();
                Log.i(MainActivity.TAG,"Response: "+ response[0]);
            } catch(IOException e) {
                Log.i(MainActivity.TAG,"IOException: "+e.getMessage());
                ToastOnMainThread("IOException: "+e.getMessage());
            }
        } else {
            ToastOnMainThread("Not connected");
        }
        return response[0];
    }

    /**
     * This method sends a message to the server and return a response as string
     * @param cmd The command to send to server
     * @return The response from the server
     */
    private String sendCommandForResponse(Commands cmd) {
        if(isConnected()) {
            try{
                Log.i(MainActivity.TAG, "Sending command: " + cmd);
                // Send message
                out.println(cmd.toString());
                Log.i(MainActivity.TAG, "Message send, waiting for response...");

                // wait for message...
                String response = in.readLine();

                Log.i(MainActivity.TAG,"Response: "+response);

                return response;

            } catch (IOException e) {
                Log.i(MainActivity.TAG, "Send or getting messages error: "+e.getMessage());
            }
        } else {
            ToastOnMainThread("Not connected");
            Log.i(MainActivity.TAG,"Not connected, so can't send command '" + cmd + "'");
        }
        return null;
    }

    private void ToastOnMainThread(final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(final ActionMenuItemView item, final Drawable icon) {
        Log.i(MainActivity.TAG,"Updating '"+item.toString()+"' with value '"+icon.toString()+"'");
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                item.setIcon(icon);
            }
        });
    }

    private void pushToSongList(final String item) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                context.songList.add(item);
                context.musicAdapter.notifyDataSetChanged();
            }
        });
    }

}
