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
import nl.melledijkstra.musicplayerclient.MelonPlayerService;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;
import nl.melledijkstra.musicplayerclient.melonplayer.MelonPlayer;

/**
 * <p>Created by Melle Dijkstra on 10-4-2016</p>
 */
public class ConnectActivity extends AppCompatActivity {

    private static final String TAG = ConnectActivity.class.getSimpleName();
    SharedPreferences mSettings;

    EditText mEditTextIP;
    Button mBtnConnect;

    ProgressDialog mConnectDialog;

    MelonPlayerService mBoundService;
    boolean isBound = false;

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, getClass().getSimpleName()+" - MESSAGE RECEIVED: "+intent.getAction());

            switch(intent.getAction()) {
                case MelonPlayerService.CONNECTED:
                    // If service says it's connected then open MainActivity
                    if(mConnectDialog.isShowing()) {
                        mConnectDialog.dismiss();
                    }
                    startMainScreen();
                case MelonPlayerService.CONNECTFAILED:
                    if(mConnectDialog.isShowing()) {
                        mConnectDialog.dismiss();
                        String reason = (intent.getStringExtra("exception") != null) ? ": "+intent.getStringExtra("exception") : "";
                        Toast.makeText(ConnectActivity.this, "Connect failed"+reason, Toast.LENGTH_SHORT).show();
                    }
            }
        }
    };

    private boolean mReceiverRegistered = false;

    private IntentFilter mBroadcastFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Log.i(TAG,"onCreate in ConnectActivity");

        mBroadcastFilter = new IntentFilter();
        mBroadcastFilter.addAction(MelonPlayerService.CONNECTED);
        mBroadcastFilter.addAction(MelonPlayerService.DISCONNECTED);
        mBroadcastFilter.addAction(MelonPlayerService.CONNECTFAILED);
        mBroadcastFilter.addAction(MelonPlayerService.MESSAGERECEIVED);
        mBroadcastFilter.addAction(MelonPlayerService.UPDATE);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        initializeUI();

    }

    private void initializeUI() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mEditTextIP = (EditText) findViewById(R.id.edittext_ip);
        if(mEditTextIP != null) {
            mEditTextIP.setText(mSettings.getString(PreferenceKeys.HOST_IP, MelonPlayer.DEFAULT_IP));
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

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MelonPlayerService.LocalBinder myBinder = (MelonPlayerService.LocalBinder) binder;
            mBoundService = myBinder.getService();
            isBound = true;
            if(mBoundService.isConnected()) {
                startMainScreen();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onPause() {
        Log.i(TAG,"onPause in ConnectActivity");
        super.onPause();
        if(mReceiverRegistered && bReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
            mReceiverRegistered = false;
            Log.v(TAG,getClass().getSimpleName()+" - Broadcast listener unregistered");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG,getClass().getSimpleName()+" - onResume");
        if (mBoundService != null && mBoundService.isConnected()) {
            Log.v(TAG, "Already connected, opening main activity");
            startMainScreen();
        } else {
            Log.v(TAG,"Binding to service");
            bindService(new Intent(this, MelonPlayerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            if(!mReceiverRegistered) {
                LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver,mBroadcastFilter);
                Log.v(TAG,getClass().getSimpleName()+" - Broadcast listener registered");
                mReceiverRegistered = true;
            }
            Log.v(TAG, "Not connected, staying in connect activity");
        }
    }

    private void startMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slidein, R.anim.slideout);
    }

    private View.OnClickListener onConnectBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(checkBound()) {
                if(!App.DEBUG) {
                    if(mBoundService.isConnected()) {
                        startMainScreen();
                    } else {
                        final String ip = mEditTextIP.getText().toString();
                        Log.v(TAG,"mEditTextIP content: "+ip);

                        // Save the data
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString(PreferenceKeys.HOST_IP, ip);
                        editor.apply();
                        Log.v(TAG,"IP Saved to preferences");

                        mConnectDialog.setMessage("Connecting to "+ip+" ...");
                        mConnectDialog.setCancelable(false);
                        mConnectDialog.show();

                        mBoundService.connect();
                    }
                } else {
                    startMainScreen();
                }
            }
        }
    };

    private boolean checkBound() {
        if(isBound) {
            Log.v(TAG,getClass().getSimpleName()+" - Activity is still bound");
        } else {
            Log.v(TAG,getClass().getSimpleName()+" - Service is not bound to "+getClass().getSimpleName());
        }
        return isBound;
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG,getClass().getSimpleName()+" - onDestroy");
        if(isBound) {
            if(mBoundService != null) {
                unbindService(mServiceConnection);
                Log.v(TAG,getClass().getSimpleName()+" - unbinding from service");
            }
            isBound = false;
        }
        super.onDestroy();
    }
}
