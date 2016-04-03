package nl.melledijkstra.musicplayerclient;

/**
 * Created by Melle Dijkstra on 2-4-2016.
 */
public final class Config {

    // Network configuration
    public static final String HOST = "192.168.1.69";
    public static final int PORT = 1010;

    // 4 seconds till socket connection timeout
    public static final int TIMEOUT = 4000;

    // Buffer size
    public static final int BUFFER_SIZE = 1024;

    // private so no instance can be made (static class)
    private Config() {}

}
