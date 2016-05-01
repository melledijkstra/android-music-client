package nl.melledijkstra.musicplayerclient.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.ConnectionService;
import nl.melledijkstra.musicplayerclient.MessageReceiver;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.ui.fragments.MusicPlayerFragment;
import nl.melledijkstra.musicplayerclient.ui.fragments.ViewPagerAdapter;
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
    private ViewPagerAdapter mPagerAdapter;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private IntentFilter mBroadcastFilter;

    // Fragments
    ArrayList<Fragment> fragments;
    MusicPlayerFragment mPlayerFragment;
    YoutubeFragment mYTFragment;

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
        setContentView(R.layout.activity_main);

        Log.i(App.TAG, getClass().getSimpleName()+" - onCreate runs");

        mBroadcastFilter = new IntentFilter();
        mBroadcastFilter.addAction(ConnectionService.DISCONNECTED);
        mBroadcastFilter.addAction(ConnectionService.MESSAGERECEIVED);
        mBroadcastFilter.addAction(ConnectionService.UPDATE);

        // Get SharedPreferences object to get settings from SettingsActivity
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        initializeUI();
        // Setup paging (swiping between fragments)
        initializePaging();
    }

    public void initializeUI() {
        // Setup toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup tabs and viewpager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        viewPager = (ViewPager) findViewById(R.id.viewpager);

        if(settings.getBoolean(PreferenceKeys.DEBUG, false)) {
            RelativeLayout root = (RelativeLayout) findViewById(R.id.activity_main_container);
            ListView debugList = new ListView(this);
            debugList.setBackgroundColor(Color.BLACK);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0,0);
            debugList.setLayoutParams(params);
            if (root != null) {
                root.addView(debugList);
            }
        }
    }

    private void registerMessageReceiver(MessageReceiver receiver) {
        this.mMessageReceivers.add(receiver);
    }

    private void initializePaging() {
        fragments = new ArrayList<>();

        mPlayerFragment = new MusicPlayerFragment();
        registerMessageReceiver(mPlayerFragment);
        mYTFragment = new YoutubeFragment();
        registerMessageReceiver(mYTFragment);

        fragments.add(mPlayerFragment);
        fragments.add(mYTFragment);

        mPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);

        viewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_music_note_colored_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_action_youtube);
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
        switch(item.getItemId()) {
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
                Toast.makeText(MainActivity.this, "Not implemented "+item.getTitle(), Toast.LENGTH_SHORT).show();
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
            if(!mBoundService.isConnected()) {
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
