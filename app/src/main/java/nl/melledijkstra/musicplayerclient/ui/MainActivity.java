package nl.melledijkstra.musicplayerclient.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.ConnectionService;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.ui.fragments.AlbumsFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.MusicControllerFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.SongsFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.YoutubeFragment;

/**
 * Controller for the Main screen. This screen has the controls and information about the musicplayer
 */
public class MainActivity extends AppCompatActivity {

    // The connection service
    public ConnectionService mBoundService;

    // Variable for checking if service is bound
    private boolean mBound = false;

    // UI views
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private NavigationView drawerNavigation;
    private ImageView menuAction;

    private IntentFilter mBroadcastFilter;

    // Fragments
    ArrayList<Fragment> fragments;
    MusicControllerFragment mPlayerFragment;
    YoutubeFragment mYTFragment;
    SongsFragment mSongsFragment;
    AlbumsFragment mAlbumsFragment;

    // SharedPreferences object to get settings from SettingsActivity
    SharedPreferences settings;

    // Variable for checking if broadcast receiver is registered
    boolean mReceiverRegistered;

    // List of MessageReceivers that need to know about new messages from the server
    private ArrayList<MessageReceiver> mMessageReceivers = new ArrayList<>();

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(App.TAG, MainActivity.this.getClass().getSimpleName()+" - BROADCAST RECEIVED: "+intent.getAction());
            switch(intent.getAction()) {
                case ConnectionService.DISCONNECTED:
                    // if host disconnects then go to ConnectActivity
                    Intent startConnectActivity = new Intent(MainActivity.this,ConnectActivity.class);
                    startActivity(startConnectActivity);
                    finish();
                    break;
                case ConnectionService.MESSAGERECEIVED:
                    String raw_json = intent.getStringExtra("msg");
                    if(raw_json != null) {
                        Log.v(App.TAG,"Got message: "+raw_json);
                        try {
                            JSONObject json = new JSONObject(raw_json);
                            // notify all IRemoteMessageReceivers that a new message was received
                            for(int i = 0; i < mMessageReceivers.size(); i++) {
                                mMessageReceivers.get(i).onReceive(json);
                            }
                        } catch (JSONException e) {
                            Log.e(App.TAG,"Corrupted json data: "+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Default stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);

        Log.i(App.TAG, getClass().getSimpleName()+" - onCreate runs");

        mBroadcastFilter = new IntentFilter();
        mBroadcastFilter.addAction(ConnectionService.DISCONNECTED);
        mBroadcastFilter.addAction(ConnectionService.MESSAGERECEIVED);
        mBroadcastFilter.addAction(ConnectionService.UPDATE);

        // Get SharedPreferences object to get settings from SettingsActivity
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        initializeUI();
        // Start off with a album view
        getSupportFragmentManager().beginTransaction()
                .add(R.id.music_content_container, new AlbumsFragment())
                .commit();
    }

    public void initializeUI() {
        // Setup toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // DrawerLayout
        drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerNavigation = (NavigationView) findViewById(R.id.drawer_navigation);
        if (drawerNavigation != null) {
            drawerNavigation.setNavigationItemSelectedListener(onNavigationItemClick);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    NavigationView.OnNavigationItemSelectedListener onNavigationItemClick = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = null;
            switch(item.getItemId()) {
                case R.id.drawer_mplayer:
                    Log.d(App.TAG,"drawer_player");
                    // TODO: change to music player fragment
                    Toast.makeText(MainActivity.this, "music player selected", Toast.LENGTH_SHORT).show();
                    fragment = new AlbumsFragment();
                    break;
                case R.id.drawer_youtube:
                    Log.d(App.TAG,"drawer_youtube");
                    // TODO: Change to youtube fragment
                    Toast.makeText(MainActivity.this, "youtube selected", Toast.LENGTH_SHORT).show();
                    fragment = new YoutubeFragment();
                    break;
                case R.id.drawer_settings:
                    openSettingsActivity();
                    break;
            }

            if(fragment != null) {
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.music_content_container, fragment);
                ft.commit();
            }
            drawer.closeDrawers();
            return true;
        }
    };

    private void openSettingsActivity() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public void registerMessageReceiver(MessageReceiver receiver) {
        this.mMessageReceivers.add(receiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(mBound && mBoundService != null && mBoundService.isConnected()) {
                    // TODO: Create messages through factory!
                    try{
                        JSONObject obj = new JSONObject();
                        JSONObject mplayer = new JSONObject();
                        mplayer.put("cmd","changevol");
                        mplayer.put("vol","up");
                        obj.put("cmd","mplayer");
                        obj.put("mplayer",mplayer);
                        mBoundService.sendMessage(obj);
                    } catch(JSONException e) {
                        Log.v(App.TAG,"Could not create json message - "+e.getMessage());
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(mBound && mBoundService != null && mBoundService.isConnected()) {
                    // TODO: Create messages through factory!
                    try{
                        JSONObject obj = new JSONObject();
                        JSONObject mplayer = new JSONObject();
                        mplayer.put("cmd","changevol");
                        mplayer.put("vol","down");
                        obj.put("cmd","mplayer");
                        obj.put("mplayer",mplayer);
                        mBoundService.sendMessage(obj);
                    } catch(JSONException e) {
                        Log.v(App.TAG,"Could not create json message - "+e.getMessage());
                    }
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        if(toggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch(item.getItemId()) {
            case R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_connect:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                mBoundService.disconnect();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Disconnect with "+mBoundService.getRemoteIp()+"?")
                        .setPositiveButton("Disconnect",dialogClickListener)
                        .show();
                break;
            case R.id.action_settings:
                Intent openSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(openSettingsActivity);
                break;
            default:
                Toast.makeText(this, "No action for: "+item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(App.TAG, getClass().getSimpleName()+" - onDestroy runs");
        if(mBoundService != null) {
            mBoundService.disconnect();
        }
        if(mServiceConnection != null && mBound) {
            unbindService(mServiceConnection);
            Log.v(App.TAG,getClass().getSimpleName()+" - unbinding service");
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.i(App.TAG, getClass().getSimpleName()+" - onResume");
        if(mBoundService == null) {
            Log.v(App.TAG,getClass().getSimpleName()+" - Binding to service");
            bindService(new Intent(this, ConnectionService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // Check if still connected, if not then open ConnectActivity
            if(!mBoundService.isConnected() && !App.DEBUG) {
                Intent startConnectionActivity = new Intent(this,ConnectActivity.class);
                startActivity(startConnectionActivity);
                finish();
            }
        }
        if(!mReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver,mBroadcastFilter);
            Log.v(App.TAG,getClass().getSimpleName()+" - Broadcast listener registered");
            mReceiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(App.TAG,getClass().getSimpleName() + " - onPause");
        if(mReceiverRegistered && bReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
            mReceiverRegistered = false;
            Log.v(App.TAG,getClass().getSimpleName()+" - Broadcast listener unregistered");
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ConnectionService.LocalBinder myBinder = (ConnectionService.LocalBinder) binder;
            mBoundService = myBinder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
}
