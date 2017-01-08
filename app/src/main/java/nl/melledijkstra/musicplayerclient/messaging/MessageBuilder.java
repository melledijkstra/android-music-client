package nl.melledijkstra.musicplayerclient.messaging;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import nl.melledijkstra.musicplayerclient.Utils;

/**
 * Created by melle on 4-12-2016.
 * <p>This class generates a Message for the communication between the App and the server</p>
 */

public class MessageBuilder {

    private static final String TAG = MessageBuilder.class.getSimpleName();
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

    public MessageBuilder playSong(long songid) {
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

    public MessageBuilder changeVol(int volume) {
        volume = Utils.Constrain(volume, 0, 100);
        try {
            getmplayer().put("cmd", "changevol");
            getmplayer().put("vol", volume);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    private JSONObject getmplayer() {
        if (mplayer == null) {
            mplayer = new JSONObject();
        }
        return mplayer;
    }

    private JSONObject getyoutube_dl() {
        if (youtube_dl == null) {
            youtube_dl = new JSONObject();
        }
        return youtube_dl;
    }

    public JSONObject build() {
        try {
            // Put the logical parts into the root message JSONObject
            // They are only added if they have been used (initialized)
            if (mplayer != null && mplayer.length() > 0) message.put("mplayer", mplayer);
            if (youtube_dl != null && youtube_dl.length() > 0)
                message.put("youtube_dl", youtube_dl);
            return message;
        } catch (JSONException e) {
            Log.d(TAG, "MessageBuilder - Could not create JSONObject");
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

    public MessageBuilder pause() {
        try {
            getmplayer().put("cmd", "pause");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder changePos(int position) {
        try {
            getmplayer().put("cmd", "changepos");
            getmplayer().put("pos", position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder download(String url, long id) {
        try {
            getyoutube_dl().put("cmd", "download");
            getyoutube_dl().put("url", url);
            getyoutube_dl().put("albumid", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder previous() {
        try {
            getmplayer().put("cmd", "prev");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder next() {
        try {
            getmplayer().put("cmd", "next");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder stop() {
        try {
            getmplayer().put("cmd", "stop");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder deleteSong(long id) {
        try {
            getmplayer().put("cmd", "deletesong");
            getmplayer().put("songid", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder renameSong(long id, String newName) {
        try {
            getmplayer().put("cmd", "renamesong");
            getmplayer().put("songid", id);
            getmplayer().put("newname", newName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder moveSong(long id, long albumid) {
        try {
            getmplayer().put("cmd", "movesong");
            getmplayer().put("songid", id);
            getmplayer().put("albumid", albumid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MessageBuilder playNext(long id) {
        try {
            getmplayer().put("cmd", "playnext");
            getmplayer().put("songid", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
