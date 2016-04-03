package nl.melledijkstra.musicplayerclient;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "nl.melledijkstra.musicplayerclient";

    // UI views
    ListView songListView;
    EditText editText;
    Toolbar toolbar;
    Button sendBtn;

    // List with songs
    List<String> songList = new ArrayList<String>();

    // musicAdapter that dynamically fills the music list
    ArrayAdapter<String> musicAdapter;

    // The music player object which send commands to the python musicplayer
    MusicPlayer musicPlayer;
    SwipeRefreshLayout refreshSwipeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Default stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate runs");

        // get views
        editText = (EditText) findViewById(R.id.editText);
        songListView = (ListView) findViewById(R.id.songListView);
        sendBtn = (Button) findViewById(R.id.button);
        refreshSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        refreshSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                musicPlayer.refreshMusicList();
            }
        });

        // Toolbar stuff
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        /*
         link adapter to ListView so that updating a
         single source (ArrayList<String> songList) can be used for updating
         */
        musicAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, songList);
        songListView.setAdapter(musicAdapter);

        // Create MusicPlayer, doens't connect to python server yet
        musicPlayer = new MusicPlayer(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        switch(item.getItemId()) {
            case R.id.action_connect:
                // make connection with python music server
                if(!musicPlayer.isConnected()) {
                    musicPlayer.connect();
                } else {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    // disconnect with server if user want's to disconnect
                                    Toast.makeText(MainActivity.this, "Disconnecting...", Toast.LENGTH_SHORT).show();
                                    musicPlayer.disconnect();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    // do nothing if user want's to keep connection
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    String remoteAddress = musicPlayer.socket.getRemoteSocketAddress().toString().substring(1);
                    builder.setMessage("Disconnect with '"+remoteAddress+"'?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
                break;
            case R.id.action_refresh_songlist:
                Log.i(TAG, "Refresh menu item clicked");
                refreshSwipeLayout.setRefreshing(true);
                musicPlayer.refreshMusicList();
                break;
            case R.id.action_send_command:
                if(musicPlayer.isConnected()) {
                    // Dialog builder for MainActivity
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Send command:");

                    // Input box for the input
                    final EditText dialoginput = new EditText(this);
                    // Specify the type of input expected
                    dialoginput.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(dialoginput);

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    musicPlayer.sendMessageResponseToToast(dialoginput.getText().toString());
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    // do nothing if user want's to keep connection
                                    break;
                            }
                        }
                    };
                    // Register button clicks
                    builder.setPositiveButton("Send",dialogClickListener).setNegativeButton("Cancel",dialogClickListener);
                    builder.show();
                } else {
                    Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Toast.makeText(MainActivity.this, "Clicked: "+item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void click(View view) {
        String msg = editText.getText().toString();
        musicPlayer.sendMessageResponseToToast(msg);
        editText.setText("");
        //musicPlayer.refreshMusicList(songList);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy runs");
        musicPlayer.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume runs, check if still connected");
        musicPlayer.checkConnection();
        super.onResume();
    }
}
