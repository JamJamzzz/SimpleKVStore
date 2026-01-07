package kvstore.api;

import kvstore.metrics.KVStoreMetricsSnapshot;
import kvstore.model.KVEntry;

import java.util.Iterator;

/**The put method, insert or update a key value pair
 * @param <K> The type of the key
 * @param <V> The type of the value
 *
 */
public interface KVStore<K, V> extends Iterable<KVEntry<K, V>>{
    /**
     * Insert or update a pair
     * @param key the key of the pair that need to be inserted or updated
     * @param value the value of the pair
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    void put(K key, V value);

    /**
     * Insert or update a pair with a Time To Live in milliseconds
     * @param key the key of the pair that need to be inserted or updated
     * @param value the value of the pair
     * @param ttlMillis the time-to-live duration in milliseconds
     * @throws IllegalArgumentException if {@code key} is {@code null} or {@code ttlMillis} is less that or euqals to 0
     */
    void put(K key, V value, long ttlMillis);

    /**
     * Retrieve the value associated with the key
     * @param key the key of the pair that need to be looked up
     * @return the value with the key or {@code null} if the key does not exit
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    V get(K key);

    /**
     * Remove a key value pair
     * @param key the key need to be removed
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    void delete(K key);

    /**
     * Check whether the store contains the key
     * @param key the key to be checked
     * @return {@code true} if the key exits or {@code false} otherwise
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    boolean containsKey(K key);

    /**
     * Return the number of key value pairs currently stored.
     * @return the total number of entries in the store
     */
    int size();

    /**
     * Return the iterator over the entries in this store
     * @return an iterator over {@link KVEntry} instances in the store
     */
    @Override
    Iterator<KVEntry<K, V>> iterator();

    /**
     * Return a snapshot of the current runtime metrics
     * @return a snapshot of the KVStore metrics
     */
    KVStoreMetricsSnapshot getMetrics();
}
