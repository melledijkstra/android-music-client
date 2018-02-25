package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import nl.melledijkstra.musicplayerclient.MelonPlayerService;

/**
 * Created by melle on 12-12-2016.
 * Fragment class that binds to MelonPlayerService automatically
 */

public class ServiceBoundFragment extends Fragment {

    protected MelonPlayerService boundService;
    protected boolean isBound;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        doBindService();
    }

    protected ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            boundService = ((MelonPlayerService.LocalBinder)service).getService();
            isBound = true;
            onBounded();
        }

        public void onServiceDisconnected(ComponentName className) {
            boundService = null;
            isBound = false;
            onUnbound();
        }
    };

    /**
     * Run when service is disconnected
     */
    protected void onUnbound() {}

    /**
     * Run when service is bound
     */
    protected void onBounded() {}

    void doBindService() {
        getActivity().bindService(new Intent(getActivity(),
                MelonPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (isBound) {
            // Detach our existing connection.
            getActivity().unbindService(serviceConnection);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

}
