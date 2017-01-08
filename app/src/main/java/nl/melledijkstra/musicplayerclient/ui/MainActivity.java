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
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MelonPlayerService;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.melonplayer.Album;
import nl.melledijkstra.musicplayerclient.messaging.MessageBuilder;
import nl.melledijkstra.musicplayerclient.ui.fragments.AlbumsFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.SongsFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.YoutubeFragment;

/**
 * Controller for the Main screen. This screen has the controls and information about the musicplayer
 */
public class MainActivity extends AppCompatActivity implements MessageReceiver {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_CODE = 828453;
    // The connection service
    public MelonPlayerService mBoundService;
    private static int activities = 0;

    // Variable for checking if service is bound
    private boolean mBound = false;

    // UI views
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private NavigationView drawerNavigation;

    private IntentFilter mBroadcastFilter;

    // Fragments
    //    MusicControllerFragment mPlayerFragment;
    //    YoutubeFragment mYTFragment;
    //    SongsFragment mSongsFragment;
    //    AlbumsFragment mAlbumsFragment;

    // SharedPreferences object to get settings from SettingsActivity
    SharedPreferences settings;

    // Variable for checking if broadcast receiver is registered
    boolean mReceiverRegistered;

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, MainActivity.this.getClass().getSimpleName() + " - BROADCAST RECEIVED: " + intent.getAction());
            switch (intent.getAction()) {
                case MelonPlayerService.DISCONNECTED:
                    // if host disconnects then go to ConnectActivity
                    Intent startConnectActivity = new Intent(MainActivity.this, ConnectActivity.class);
                    startActivity(startConnectActivity);
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activities++;
        Log.d(TAG, "there are currently " + activities + " MainActivities running");

        Log.i(TAG, getClass().getSimpleName() + " - onCreate runs");

        mBroadcastFilter = new IntentFilter();
        mBroadcastFilter.addAction(MelonPlayerService.DISCONNECTED);
        mBroadcastFilter.addAction(MelonPlayerService.UPDATE);

        ((App) getApplication()).registerMessageReceiver(this);

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

        if (getSupportActionBar() != null) {
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
            drawerNavigation.getHeaderView(0).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: create a nice about activity
                    new AlertDialog.Builder(MainActivity.this)
                            .setIcon(R.mipmap.app_logo)
                            .setTitle("Melon Music Player")
                            .setMessage("The melon music player created by Melle Dijkstra © " + Calendar.getInstance().get(Calendar.YEAR))
                            .show();
                }
            });
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        // Make sure to close drawer first before returning to previous activities
        if (drawer.isDrawerOpen(GravityCompat.START)) {
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
            switch (item.getItemId()) {
                case R.id.drawer_mplayer:
                    fragment = new AlbumsFragment();
                    break;
                case R.id.drawer_youtube:
                    fragment = new YoutubeFragment();
                    break;
                case R.id.drawer_settings:
                    openSettingsActivity();
                    break;
            }

            if (fragment != null) {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mBound && mBoundService != null && mBoundService.isConnected()) {
                    mBoundService.sendMessage(new MessageBuilder().volumeUp().build());
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mBound && mBoundService != null && mBoundService.isConnected()) {
                    mBoundService.sendMessage(new MessageBuilder().volumeDown().build());
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
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
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
                builder.setMessage("Disconnect with " + mBoundService.getRemoteIp() + "?")
                        .setPositiveButton("Disconnect", dialogClickListener)
                        .show();
                break;
            case R.id.action_settings:
                Intent openSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(openSettingsActivity);
                break;
            default:
                Log.d(TAG, "No action for: " + item.getTitle());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, getClass().getSimpleName() + " - onDestroy runs");
        ((App) getApplication()).unRegisterMessageReceiver(this);
        if (mServiceConnection != null && mBound) {
            unbindService(mServiceConnection);
            Log.v(TAG, getClass().getSimpleName() + " - unbinding service");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, getClass().getSimpleName() + " - onResume");
        if (mBoundService == null) {
            Log.v(TAG, getClass().getSimpleName() + " - Binding to service");
            bindService(new Intent(this, MelonPlayerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // Check if still connected, if not then open ConnectActivity
            if (!mBoundService.isConnected() && !App.DEBUG) {
                Intent startConnectionActivity = new Intent(this, ConnectActivity.class);
                startActivity(startConnectionActivity);
                finish();
            }
        }
        if (!mReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, mBroadcastFilter);
            Log.v(TAG, "Broadcast listener registered");
            mReceiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        if (mReceiverRegistered && bReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
            mReceiverRegistered = false;
            Log.v(TAG, "Broadcast listener unregistered");
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MelonPlayerService.LocalBinder myBinder = (MelonPlayerService.LocalBinder) binder;
            mBoundService = myBinder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    public void showSongsFragment(Album album) {
        SongsFragment songsFragment = new SongsFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("albumid", album.getID());
        Log.v(TAG, "Show songs of album: " + album.getTitle() + " (" + album.getID() + ")");
        songsFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.music_content_container, songsFragment)
                .commit();
        setTitle(album.getTitle());
    }

    @Override
    public void onReceive(JSONObject json) {
        try {
            if (json.has("message")) {
                Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show();
            }
            if(json.has("mplayer") && json.getJSONObject("mplayer").has("message")) {
                Toast.makeText(this, json.getJSONObject("mplayer").getString("message"), Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
