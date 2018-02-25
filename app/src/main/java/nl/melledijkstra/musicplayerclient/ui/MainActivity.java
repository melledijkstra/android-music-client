package nl.melledijkstra.musicplayerclient.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MelonPlayerService;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.Utils;
import nl.melledijkstra.musicplayerclient.grpc.VolumeControl;
import nl.melledijkstra.musicplayerclient.melonplayer.AlbumModel;
import nl.melledijkstra.musicplayerclient.ui.fragments.AlbumsFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.MediaDownloadFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.SongsFragment;

/**
 * Controller for the Main screen. This screen has the controls and information about the musicplayer
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int REQUEST_CODE = 828453;

    // TODO: remove this, debug check for amount of active activities
    private static int activities = 0;

    // UI Components
    @BindView(R.id.main_drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_navigation)
    NavigationView drawerNavigation;

    private ActionBarDrawerToggle toggle;

    // Fragments
    //    MusicControllerFragment mPlayerFragment;
    MediaDownloadFragment mMediaDownloadFragment;
    AlbumsFragment mAlbumsFragment;

    // SharedPreferences object to get settings from SettingsActivity
    SharedPreferences settings;

    // The connection service
    public MelonPlayerService mBoundService;
    // Variable for checking if service is bound
    private boolean mBound = false;

    // Variable for checking if broadcast receiver is registered
    boolean mReceiverRegistered;
    private IntentFilter mBroadcastFilter;

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "BROADCAST RECEIVED: " + intent.getAction());
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case MelonPlayerService.DISCONNECTED:
                        // if host disconnects then go to ConnectActivity
                        Intent startConnectActivity = new Intent(MainActivity.this, ConnectActivity.class);
                        startActivity(startConnectActivity);
                        finish();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Auto bind all views
        ButterKnife.bind(this);

        // TODO: remove debug statements
        activities++;
        Log.d(TAG, "there are currently " + activities + " MainActivities running");

        Log.i(TAG, "onCreate");

        startService(new Intent(this, MelonPlayerService.class));
        bindService(new Intent(this, MelonPlayerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        // Setup broadcast
        mBroadcastFilter = new IntentFilter();
        mBroadcastFilter.addAction(MelonPlayerService.DISCONNECTED);

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
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // DrawerLayout
        toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerNavigation.setNavigationItemSelectedListener(onNavigationItemClick);
        drawerNavigation.getHeaderView(0).setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                .setIcon(R.mipmap.app_logo)
                .setTitle("Melon Music Player")
                .setMessage("The melon music player created by Melle Dijkstra © " + Calendar.getInstance().get(Calendar.YEAR))
                .show());
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
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = null;
            switch (item.getItemId()) {
                case R.id.drawer_mplayer:
                    if (mAlbumsFragment == null) {
                        mAlbumsFragment = new AlbumsFragment();
                    }
                    fragment = mAlbumsFragment;
                    break;
                case R.id.drawer_youtube:
                    if (mMediaDownloadFragment == null) {
                        mMediaDownloadFragment = new MediaDownloadFragment();
                    }
                    fragment = mMediaDownloadFragment;
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
                if (mBound && mBoundService != null) {
                    int vol = Utils.Constrain(mBoundService.getMelonPlayer().getVolume() + 5, 0, 100);
                    mBoundService.musicPlayerStub.changeVolume(VolumeControl.newBuilder()
                            .setVolumeLevel(vol)
                            .build(), mBoundService.defaultMMPResponseStreamObserver);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mBound && mBoundService != null) {
                    int vol = Utils.Constrain(mBoundService.getMelonPlayer().getVolume() - 5, 0, 100);
                    mBoundService.musicPlayerStub.changeVolume(VolumeControl.newBuilder()
                            .setVolumeLevel(vol)
                            .build(), mBoundService.defaultMMPResponseStreamObserver);
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
        Log.i(TAG, "onDestroy");
        if (mServiceConnection != null && mBound) {
            unbindService(mServiceConnection);
            Log.v(TAG, "Unbinding service");
        }
        unregisterBroadcastReceiver();
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
        registerBroadcastReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        unregisterBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        if (!mReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, mBroadcastFilter);
            Log.v(TAG, "Broadcast listener registered");
            mReceiverRegistered = true;
        }
    }

    private void unregisterBroadcastReceiver() {
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

    public void showSongsFragment(AlbumModel albumModel) {
        SongsFragment songsFragment = new SongsFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("albumid", albumModel.getID());
        Log.v(TAG, "Show songs of albumModel: " + albumModel.getTitle() + " (" + albumModel.getID() + ")");
        songsFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.music_content_container, songsFragment)
                .commit();
        setTitle(albumModel.getTitle());
    }
}
