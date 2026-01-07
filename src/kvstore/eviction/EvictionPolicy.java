package kvstore.eviction;

/**
 * EvictionPolicy interface defines a pluggable policy strategy for the store
 *
 * This interface can track key access patterns and decided with key should be evicted
 * When the store reach the capacity
 *
 * The store does not know how this interface is implemented
 * It only interacts with this interface through the callbacks
 * @param <K>
 */
public interface EvictionPolicy <K>{
    /**
     *Callback invoked when a key is inserted or updated
     * @param key the key that was put into the store
     */
    void onPut(K key);

    /**
     * Callback invoked when a key is accessed
     * @param key the key that was accessed
     */
    void onGet(K key);

    /**
     * Indicates whether the eviction should be called
     * @return {@code true} if a key should be evicted, {@code false} otherwise
     */
    boolean shouldEvict();

    /**
     * Select the key and returns the key to be evicted from the store
     *
     * This method is only expected to be called when
     * {@link #shouldEvict()} return {@code true}
     * @return the key that should be evicted from the store
     */
    K evictKey();
}
