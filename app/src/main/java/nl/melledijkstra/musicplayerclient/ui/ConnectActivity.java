package nl.melledijkstra.musicplayerclient.ui;

import android.app.ProgressDialog;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.ConnectionService;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.entities.MusicPlayer;

/**
 * <p>Created by Melle Dijkstra on 10-4-2016</p>
 */
public class ConnectActivity extends AppCompatActivity {

    SharedPreferences mSettings;

    EditText mEditTextIP;
    Button mBtnConnect;

    ProgressDialog mConnectDialog;

    ConnectionService mBoundService;
    boolean mBound = false;

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(App.TAG, getClass().getSimpleName()+" - MESSAGE RECEIVED: "+intent.getAction());

            switch(intent.getAction()) {
                case ConnectionService.CONNECTED:
                    // If service says it's connected then open MainActivity
                    if(mConnectDialog.isShowing()) {
                        mConnectDialog.dismiss();
                    }
                    startMainScreen();
                case ConnectionService.CONNECTFAILED:
                    if(mConnectDialog.isShowing()) {
                        mConnectDialog.dismiss();
                        String reason = (intent.getStringExtra("exception") != null) ? ": "+intent.getStringExtra("exception") : "";
                        Toast.makeText(ConnectActivity.this, "Connect failed"+reason, Toast.LENGTH_SHORT).show();
                    }
            }
        }
    };

    private boolean mReceiverRegistered = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Log.i(App.TAG,"onCreate in ConnectActivity");

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        startService(new Intent(this, ConnectionService.class));

        initializeUI();

    }

    private void initializeUI() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mEditTextIP = (EditText) findViewById(R.id.edittext_ip);
        if(mEditTextIP != null) {
            mEditTextIP.setText(mSettings.getString(PreferenceKeys.HOST_IP, MusicPlayer.DEFAULT_IP));
        }
        mBtnConnect = (Button) findViewById(R.id.button_connect);
        if (mBtnConnect != null) {
            mBtnConnect.requestFocus();
            mBtnConnect.setOnClickListener(onConnectBtnClick);
        }

        mConnectDialog = new ProgressDialog(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_connect_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent startSettings = new Intent(ConnectActivity.this,SettingsActivity.class);
                startActivity(startSettings);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    @Override
    protected void onPause() {
        Log.i(App.TAG,"onPause in ConnectActivity");
        super.onPause();
        if(mReceiverRegistered && bReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
            mReceiverRegistered = false;
            Log.v(App.TAG,getClass().getSimpleName()+" - Broadcast listener unregistered");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(App.TAG,getClass().getSimpleName()+" - onResume");
        if (mBoundService != null && mBoundService.isConnected()) {
            Log.v(App.TAG, "Already connected, opening main activity");
            startMainScreen();
        } else {
            Log.v(App.TAG,"Binding to service");
            bindService(new Intent(this, ConnectionService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            if(!mReceiverRegistered) {
                // TODO: find better way to add actions to broadcast receiver
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectionService.CONNECTED);
                filter.addAction(ConnectionService.DISCONNECTED);
                filter.addAction(ConnectionService.CONNECTFAILED);
                filter.addAction(ConnectionService.MESSAGERECEIVED);
                filter.addAction(ConnectionService.UPDATE);
                LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver,filter);
                Log.v(App.TAG,getClass().getSimpleName()+" - Broadcast listener registered");
                mReceiverRegistered = true;
            }
            Log.v(App.TAG, "Not connected, staying in connect activity");
        }
    }

    private void startMainScreen() {
        Intent startMainActivity = new Intent(this, MainActivity.class);
        startMainActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        startActivity(startMainActivity);
        finish();
    }

    private View.OnClickListener onConnectBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(checkBound()) {
                final String ip = mEditTextIP.getText().toString();
                Log.v(App.TAG,"mEditTextIP content: "+ip);

                // Save the data
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putString(PreferenceKeys.HOST_IP, ip);
                editor.apply();
                Log.v(App.TAG,"IP Saved to preferences");

                mConnectDialog.setMessage("Connecting to "+ip+" ...");
                mConnectDialog.setCancelable(false);
                mConnectDialog.show();

                mBoundService.connect();
            }
        }
    };

    private boolean checkBound() {
        if(mBound) {
            Log.v(App.TAG,getClass().getSimpleName()+" - Activity is still bound");
        } else {
            Log.v(App.TAG,getClass().getSimpleName()+" - Service is not bound to "+getClass().getSimpleName());
        }
        return mBound;
    }

    @Override
    protected void onDestroy() {
        Log.i(App.TAG,getClass().getSimpleName()+" - onDestroy");
        if(mBound) {
            if(mBoundService != null && mBoundService.isConnected()) {
                unbindService(mServiceConnection);
                Log.v(App.TAG,getClass().getSimpleName()+" - unbinding from service");
            }
            mBound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.i(App.TAG,"onStop in ConnectActivity");
        super.onStop();
    }
}
