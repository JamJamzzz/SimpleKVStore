package com.haochen.kvstore.eviction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomPolicy<K> implements EvictionPolicy<K>{
    private final List<K> keys = new ArrayList<>();
    private final Random random = new Random();

    @Override
    public void onPut(K key) {
        keys.add(key);
    }

    //The random eviction does not care about access patterns
    @Override
    public void onGet(K key) {
        //no-ops
    }

    @Override
    public boolean shouldEvict() {
        return !keys.isEmpty();
    }

    @Override
    public K evictKey() {
        if(keys.isEmpty()){
            return null;
        }

        int idx = random.nextInt(keys.size());

        return keys.remove(idx);
    }
}
