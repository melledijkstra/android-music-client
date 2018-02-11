package nl.melledijkstra.musicplayerclient.melonplayer;

import nl.melledijkstra.musicplayerclient.grpc.Song;

/**
 * <p>SongModel Model class that has all information about a specific song</p>
 * <p>Created by Melle Dijkstra on 14-4-2016</p>
 */
public class SongModel implements Protoble<Song> {

    private long ID;
    private String title;
    private long duration;

    public SongModel(Song exchangeData) {
        this.Hydrate(exchangeData);
    }

    public SongModel(long ID, String title, long duration) {
        this.ID = ID;
        this.title = title;
        this.duration = duration;
    }

    @Override
    public void Hydrate(Song data) {
        ID = data.getId();
        title = data.getTitle();
        duration = data.getDuration();
    }

    public String getTitle() {
        return title;
    }

    public long getID() {
        return ID;
    }

    @Override
    public String toString() {
        return title;
    }

    public long getDuration() {
        return duration;
    }
}
