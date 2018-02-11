package nl.melledijkstra.musicplayerclient.melonplayer;

import nl.melledijkstra.musicplayerclient.grpc.MediaDownload;

/**
 * <p>Created by melle on 19-12-2016.</p>
 * Represents a media download at the server
 */

public class YTDLDownload implements Protoble<MediaDownload> {

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

    public YTDLDownload(MediaDownload data) {
        this.Hydrate(data);
    }

    @Override
    public void Hydrate(MediaDownload obj) {
        // TODO: implement this
        throw new UnsupportedOperationException("Not implemented");
    }

}
