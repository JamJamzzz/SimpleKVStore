package com.haochen.kvstore.eviction;

import java.util.Deque;
import java.util.LinkedList;

public class FIFOPolicy<K> implements EvictionPolicy<K>{
    /**
     * Queue maintaining the insertion order of the keys
     * The head of the queue is the next key to be evicted
     */
    private Deque<K> queue = new LinkedList<>();

    /**
     * Record the insertion of the key
     *
     * If the key is newly inserted, it appends to the queue
     * @param key the key that need to be added to the rare of the queue
     */
    @Override
    public void onPut(K key) {
        queue.add(key);
    }

    /**
     * FIFO policy does not consider access patterns
     * Therefore, this method is no-ops
     * @param key
     */
    @Override
    public void onGet(K key) {
        //No operations
    }

    /**
     * Indicates whether an eviction can be performed
     * @return {@code true} if there is at least one key to evict
     */
    @Override
    public boolean shouldEvict() {
        return !queue.isEmpty();
    }

    /**
     * Select and remove the last key of the queue
     * @return the earliest inserted key
     */
    @Override
    public K evictKey() {
        return queue.removeFirst();
    }
}
