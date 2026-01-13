package com.haochen.kvstore.core;

import com.haochen.kvstore.api.KVStore;
import com.haochen.kvstore.eviction.EvictionPolicy;
import com.haochen.kvstore.exception.CapacityExceededException;
import com.haochen.kvstore.exception.KeyNotFoundException;
import com.haochen.kvstore.metrics.KVStoreMetrics;
import com.haochen.kvstore.metrics.KVStoreMetricsSnapshot;
import com.haochen.kvstore.model.KVEntry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InMemoryKVStore<K, V> implements KVStore<K, V>{

    /**
     * The class attributes: store, capacity, evictionPolicy, metrics
     */
    private final Map<K, KVEntry<K, V>> store;
    private final int capacity;

    private final EvictionPolicy<K> evictionPolicy;

    private final KVStoreMetrics metrics;

    /**
     * The constructor of this store data abstraction
     *
     * @param capacity the maximum capacity of this store
     * @param evictionPolicy the policy of the eviction
     */
    public InMemoryKVStore(int capacity, EvictionPolicy<K> evictionPolicy){
        if (capacity <= 0){
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.store = new HashMap<>();
        this.evictionPolicy = evictionPolicy;
        this.metrics = new KVStoreMetrics();
    }

    @Override
    public void put(K key, V value) {
        if(key == null){
            throw new IllegalArgumentException("The key can not be null");
        }

        long start = System.nanoTime();
        try{
            if(store.containsKey(key)){
                store.put(key, new KVEntry<>(key, value));

                KVEntry<K,V> oldEntry = store.get(key);
                if(oldEntry != null){
                    KVEntry<K, V> newEntry = oldEntry.hasTTL()
                            ? new KVEntry<>(key, value, oldEntry.getRemainingTTL())
                            : new KVEntry<>(key, value);
                }

                if(evictionPolicy != null){
                    evictionPolicy.onPut(key);
                }
                return;
            }

            //The store reached the capacity, we evicted the pair that should be evicted and then put the new pair
            if(store.size() >= this.capacity){
                if(evictionPolicy != null && evictionPolicy.shouldEvict()){
                    K evictedKey = evictionPolicy.evictKey();
                    store.remove(evictedKey);
                } else {
                    /*
                     * Instead of returning null, we throw an error which allows the caller to
                     * distinguish between a real value and the failure case
                     */
                    throw new CapacityExceededException(capacity);
                }
            }

            store.put(key, new KVEntry<>(key, value));

            if(evictionPolicy != null){
                evictionPolicy.onPut(key);
            }
        } finally {
            long latencyNanoSec = System.nanoTime() - start;
            metrics.recordPut(latencyNanoSec);
        }
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        if(key == null){
            throw new IllegalArgumentException("The key can not be null");
        }
        if(ttlMillis <= 0){
            throw new IllegalArgumentException("The Time-To-Live must not be less that or equal to 0");
        }

        long start = System.nanoTime();
        try{
            if(store.containsKey(key)){
                store.put(key, new KVEntry<>(key, value));

                if(evictionPolicy != null){
                    evictionPolicy.onPut(key);
                }
                return;
            }

            //The store reached the capacity, we evicted the pair that should be evicted and then put the new pair
            if(store.size() >= this.capacity){
                if(evictionPolicy != null && evictionPolicy.shouldEvict()){
                    K evictedKey = evictionPolicy.evictKey();
                    store.remove(evictedKey);
                } else {
                    /*
                     * Instead of returning null, we throw an error which allows the caller to
                     * distinguish between a real value and the failure case
                     */
                    throw new CapacityExceededException(capacity);
                }
            }

            store.put(key, new KVEntry<>(key, value, ttlMillis));

            if(evictionPolicy != null){
                evictionPolicy.onPut(key);
            }
        } finally {
            long latencyNanoSec = System.nanoTime() - start;
            metrics.recordPut(latencyNanoSec);
        }
    }

    @Override
    public V get(K key) {
        if(key == null){
            throw new IllegalArgumentException("The key can not be null");
        }

        long start = System.nanoTime();
        boolean hit = false;

        try {
            if(!store.containsKey(key)){
                throw new KeyNotFoundException(key);
            }

            //Set the hit to be true
            hit = true;

            KVEntry<K, V> entry = store.get(key);

            if(entry.isExpired()){
                store.remove(key);
                throw new KeyNotFoundException(key);
            }

            if(evictionPolicy != null){
                evictionPolicy.onGet(key);
            }

            return entry.getValue();
        } finally {
            long latencyNanoSec = System.nanoTime() - start;
            metrics.recordGet(hit, latencyNanoSec);
        }
    }

    @Override
    public void delete(K key) {
        if(key == null){
            throw new IllegalArgumentException("The key can not be null");
        }

        if(!store.containsKey(key)){
            throw new KeyNotFoundException(key);
        }

        store.remove(key);
    }

    @Override
    public boolean containsKey(K key) {
        if(key == null){
            throw new IllegalArgumentException("The key can not be null");
        }

        KVEntry<K, V> entry = store.get(key);
        if(entry == null){
            return false;
        }

        if(entry.isExpired()){
            store.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Removes all expired entries from the underlying store.
     *
     * This method iterates over the internal key-value store and eagerly
     * deletes entries whose TTL has expired at the time of inspection.
     * For each expired entry removed, the eviction metric is recorded.
     *
     * This cleanup is performed using an iterator to ensure safe removal
     * during traversal.
     */
    private void cleanUpExpired(){
        Iterator<Map.Entry<K, KVEntry<K, V>>> it = store.entrySet().iterator();
        while(it.hasNext()){
            if(it.next().getValue().isExpired()){
                it.remove();
            }
        }
    }

    @Override
    public int size() {
        cleanUpExpired();
        return store.size();
    }

    @Override
    public Iterator<KVEntry<K, V>> iterator() {
        cleanUpExpired();
        return store.values().iterator();
    }

    @Override
    public KVStoreMetricsSnapshot getMetrics() {
        return metrics.snapshot();
    }
}