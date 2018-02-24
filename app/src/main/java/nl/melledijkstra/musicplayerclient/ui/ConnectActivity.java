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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.melledijkstra.musicplayerclient.App;
import nl.melledijkstra.musicplayerclient.MelonPlayerService;
import nl.melledijkstra.musicplayerclient.R;
import nl.melledijkstra.musicplayerclient.config.Constants;
import nl.melledijkstra.musicplayerclient.config.PreferenceKeys;

/**
 * <p>Created by Melle Dijkstra on 10-4-2016</p>
 */
public class ConnectActivity extends AppCompatActivity {

    private static final String TAG = ConnectActivity.class.getSimpleName();

    /**
     * Application specific settings
     */
    SharedPreferences mSettings;

    /**
     * UI Components
     */
    @BindView(R.id.edittext_ip)
    EditText mEditTextIP;
    @BindView(R.id.button_connect)
    Button mBtnConnect;

    /**
     * Dialog which is shown when connecting
     */
    ProgressDialog mConnectDialog;

    private boolean mReceiverRegistered = false;

    // The connection service
    public MelonPlayerService mBoundService;
    // Variable for checking if service is bound
    private boolean mBound = false;

    private IntentFilter mBroadcastFilter;
    private LocalBroadcastManager mBroadcastManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        ButterKnife.bind(this);
        Log.i(TAG, "onCreate");

        startService(new Intent(this, MelonPlayerService.class));
        bindService(new Intent(this, MelonPlayerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        mBroadcastFilter = new IntentFilter();
        mBroadcastFilter.addAction(MelonPlayerService.READY);
        mBroadcastFilter.addAction(MelonPlayerService.DISCONNECTED);
        mBroadcastFilter.addAction(MelonPlayerService.CONNECTFAILED);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        initializeUI();
    }

    /**
     * Initializes all UI components
     */
    private void initializeUI() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mEditTextIP.setText(mSettings.getString(PreferenceKeys.HOST_IP, Constants.DEFAULT_IP));
        mBtnConnect.requestFocus();

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
                Intent startSettings = new Intent(ConnectActivity.this, SettingsActivity.class);
                startActivity(startSettings);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause in ConnectActivity");
        super.onPause();
        unregisterBroadcastReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, getClass().getSimpleName() + " - onResume");
        if (mBoundService != null && mBoundService.isConnected()) {
            Log.v(TAG, "Already connected, opening main activity");
            startMainScreen();
        } else {
            registerBroadcastReceiver();
            Log.v(TAG, "Not connected, staying in connect activity");
        }
    }

    /**
     * Broadcast receiver which gets notified of events in the service
     */
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "BROADCAST RECEIVED: " + intent.getAction());
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case MelonPlayerService.READY:
                        // If service says it's connected then open MainActivity
                        if (mConnectDialog.isShowing()) {
                            mConnectDialog.dismiss();
                        }
                        startMainScreen();
                        break;
                    case MelonPlayerService.CONNECTFAILED:
                        if (mConnectDialog.isShowing()) {
                            mConnectDialog.dismiss();
                        }
                        String reason = (intent.getStringExtra("state") != null) ? ": " + intent.getStringExtra("state") : "";
                        Toast.makeText(ConnectActivity.this, "Connect failed" + reason, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };

    private void registerBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, mBroadcastFilter);
        Log.v(TAG, "Broadcast listener registered");
        mReceiverRegistered = true;
    }

    private void unregisterBroadcastReceiver() {
        if (mReceiverRegistered && bReceiver != null) {
            mBroadcastManager.unregisterReceiver(bReceiver);
            mReceiverRegistered = false;
            Log.v(TAG, "Broadcast listener unregistered");
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.i(TAG, "onServiceConnected");
            MelonPlayerService.LocalBinder myBinder = (MelonPlayerService.LocalBinder) binder;
            mBoundService = myBinder.getService();
            mBound = true;
            checkConnection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    /**
     * Checks if service is already connected
     */
    private void checkConnection() {
        if(mBoundService != null && mBoundService.isConnected()) {
            Log.i(TAG, "checkConnection: service is connected, start main screen...");
            startMainScreen();
        } else {
            Log.i(TAG, "checkConnection: service is not connected, do nothing");
        }
    }

    /**
     * Transitions from Connection activity to MainActivity
     */
    private void startMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slidein, R.anim.slideout);
    }

    @OnClick(R.id.button_connect)
    public void onConnectButtonClick(View v) {
        if (!App.DEBUG) {
            final String ip = mEditTextIP.getText().toString();

            // Save the data
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(PreferenceKeys.HOST_IP, ip);
            editor.apply();
            Log.v(TAG, "IP Saved to preferences ("+ip+")");

            // send broadcast to service that it needs to try connecting
            LocalBroadcastManager.getInstance(ConnectActivity.this).sendBroadcast(new Intent(MelonPlayerService.INITIATE_CONNECTION));

            mConnectDialog.setMessage("Connecting to " + ip + " ...");
            mConnectDialog.setCancelable(false);
            mConnectDialog.show();
        } else {
            startMainScreen();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, getClass().getSimpleName() + " - onDestroy");
        if(!mBoundService.isConnected()) {
            Log.i(TAG, "onDestroy: Service is not connected, so stopService is called");
            stopService(new Intent(this, MelonPlayerService.class));
        }
        unregisterBroadcastReceiver();
        if(mConnectDialog.isShowing()) {
            mConnectDialog.dismiss();
        }
        if (mServiceConnection != null && mBound) {
            unbindService(mServiceConnection);
            Log.v(TAG, "Unbinding service");
        }
    }
}
