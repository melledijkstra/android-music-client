package nl.melledijkstra.musicplayerclient;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * <p>Melon Music Player Message (MPMM)</p>
 * <p>Created by Melle Dijkstra on 22-4-2016</p>
 * TODO: CREATE DOCUMENTATION!!!
 */
public class MMPM implements Serializable {

    public String cmd;
    public ArrayList<String> songList;

    public MMPM() {

    }

    @Override
    public String toString() {
        return "MMPM '"+toJson()+"'";
    }

    public String toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",cmd);
            obj.put("songlist",songList);
        } catch (JSONException e) {
            Log.v(App.TAG,"Something went wrong with JSON: "+e.getMessage());
            e.printStackTrace();
        }
        return obj.toString();
    }

    public void setCommand(String cmd) {
        this.cmd = cmd;
    }

    /**
     * Processes a raw string to a MMPM message
     * @param inputMsg The string to process
     * @return A MMPM message
     */
    public static MMPM processRawMessage(String inputMsg) throws JSONException {
        MMPM returnMsg = new MMPM();
        // root json object
        JSONObject obj = new JSONObject(inputMsg);
        // get the songlist
        JSONArray songlist = new JSONArray();
        if(obj.has("songlist")) {
            songlist = obj.getJSONArray("songlist");
        }
        for(int i = 0;i < songlist.length();i++) {
            returnMsg.songList.add(songlist.getString(i));
        }
        if(returnMsg.songList != null) {
            Log.v(App.TAG,"songs: "+returnMsg.songList.size());
        }
        return returnMsg;
    }
}
