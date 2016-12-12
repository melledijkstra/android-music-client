package nl.melledijkstra.musicplayerclient.messaging;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.App;

/**
 * Created by melle on 4-12-2016.
 * <p>With this class you can generate a Message for the communication between the App and the server</p>
 */

public class MessageBuilder {

    private JSONObject message;
    private JSONObject mplayer;
    private JSONObject youtube_dl;

    public MessageBuilder() {
        message = new JSONObject();
    }

    public MessageBuilder albumList() {
        try {
            getmplayer().put("cmd", "albumlist");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder playSong(int songid) {
        try {
            getmplayer().put("cmd", "play");
            getmplayer().put("songid", songid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder songList(long albumid) {
        try {
            getmplayer().put("cmd", "songlist");
            getmplayer().put("albumid", albumid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder volumeUp() {
        try {
            getmplayer().put("cmd", "changevol");
            getmplayer().put("vol", "up");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder volumeDown() {
        try {
            getmplayer().put("cmd", "changevol");
            getmplayer().put("vol", "down");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    private JSONObject getmplayer() {
        if(mplayer == null) {
            mplayer = new JSONObject();
        }
        return mplayer;
    }

    private JSONObject getyoutube_dl() {
        if(youtube_dl == null) {
            youtube_dl = new JSONObject();
        }
        return youtube_dl;
    }

    public JSONObject build() {
        try {
            // Put the logical parts into the root message JSONObject
            // They are only added if they have been used (initialized)
            if(mplayer != null) message.put("mplayer", mplayer);
            if(youtube_dl != null) message.put("youtube_dl", youtube_dl);
            return message;
        } catch (JSONException e) {
            Log.d(App.TAG, "MessageBuilder - Could not create JSONObject");
        }
        return null;
    }

    public MessageBuilder status() {
        try {
            getmplayer().put("cmd", "status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
