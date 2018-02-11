package nl.melledijkstra.musicplayerclient.melonplayer;

import com.google.protobuf.GeneratedMessageLite;

/**
 * <p>Created by melle on 19-5-2016.</p>
 */
public interface Protoble<T> {

    /**
     * This method makes sure the object implementing this interface is able to hydrate itself with
     * information from the given source
     * @param obj The protobuf object
     */
    void Hydrate(T obj);

}
