package com.haochen.kvstore.eviction;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class LFUPolicy<K> implements EvictionPolicy<K>{
    // Mapping the key to the frequency
    Map<K, Integer> freq;

    /*Mapping the frequency to the key of that frequency
      Using the LinkedHashSet instead of HashSet to make sure when the frequencies of keys are equal
      We evicted the LRU one
     */
    Map<Integer, LinkedHashSet<K>> freqBuckets;

    //The Minimum frequency
    int minFreq;

    public LFUPolicy(){
        freq = new HashMap<>();
        freqBuckets = new HashMap<>();
        minFreq = 0;
    }

    @Override
    public void onPut(K key) {
        //The new key starts with frequency 1
        freq.put(key, 1);
        freqBuckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    @Override
    public void onGet(K key) {
        Integer oldFreq = freq.get(key);
        if(oldFreq == null){
            return;
        }

        LinkedHashSet<K> oldBucket = freqBuckets.get(oldFreq);
        oldBucket.remove(key);

        //Clean up the redundant buckets if the buckets are empty
        if(oldBucket.isEmpty()){
            freqBuckets.remove(oldFreq);
            //update the minFreq if the oldFreq is the minimum one
            if(minFreq == oldFreq){
                minFreq++;
            }
        }

        int newFreq = oldFreq + 1;
        freq.put(key, newFreq);
        freqBuckets.computeIfAbsent(newFreq, f -> new LinkedHashSet<>()).add(key);
    }

    @Override
    public boolean shouldEvict() {
        return !freq.isEmpty();
    }

    @Override
    public K evictKey() {
        LinkedHashSet<K> bucket = freqBuckets.get(minFreq);
        if(bucket == null || bucket.isEmpty()){
            return null;
        }

        //Get the LFU one, if the frequencies of some keys are equal, get the LRU one
        K evictedKey = bucket.iterator().next();
        bucket.remove(evictedKey);

        /*We don't update the minFreq here because after the eviction, a new entry will be put
          And the onPut method will be called
         */
        if(bucket.isEmpty()){
            freqBuckets.remove(minFreq);
        }
        freq.remove(evictedKey);

        return evictedKey;
    }
}
