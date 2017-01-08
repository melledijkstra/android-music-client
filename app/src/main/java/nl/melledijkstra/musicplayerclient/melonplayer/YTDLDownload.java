package nl.melledijkstra.musicplayerclient.melonplayer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by melle on 19-12-2016.
 */

public class YTDLDownload implements Hydratable {

    public enum States {
        DOWNLOADING,
        FINISHED,
        PROCESSING,
    }

    private String name;
    private String filename;
    private String speed;
    private String timeElapsed;
    private String remainingTime;
    private String percentDownloaded;
    private String totalBytes;
    private States state;

    public YTDLDownload(JSONObject data) {
        this.Hydrate(data);
    }

    @Override
    public void Hydrate(JSONObject obj) {
        try {
            if(obj.has("name")) name = obj.getString("name");
            if(obj.has("filename")) filename = obj.getString("filename");
            if(obj.has("state")) {
                switch (obj.getString("state")) {
                    case "downloading":
                        state = States.DOWNLOADING;
                        break;
                    case "finished":
                        state = States.FINISHED;
                        break;
                    case "processing":
                        state = States.PROCESSING;
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
