package nl.melledijkstra.musicplayerclient.melonplayer;

import nl.melledijkstra.musicplayerclient.grpc.MediaDownload;

/**
 * <p>Created by melle on 19-12-2016.</p>
 * Represents a media download at the server
 */
public class YTDLDownload implements Protoble<MediaDownload> {

    /**
     * The states a download can be in
     */
    public enum States {
        DOWNLOADING,
        FINISHED,
        PROCESSING,
    }

    /**
     * The name of the download
     */
    private String name;

    /**
     * The filename of the download
     */
    private String filename;

    /**
     * The speed at which it is downloading
     */
    private String speed;

    /**
     * The elapsed time of the download
     */
    private String timeElapsed;

    /**
     * The estimated time it takes to download the media
     */
    private String remainingTime;

    /**
     * Percentage of downloaded content
     */
    private String percentDownloaded;

    /**
     * Total bytes downloaded
     */
    private String totalBytes;

    /**
     * The current state of the download
     */
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
