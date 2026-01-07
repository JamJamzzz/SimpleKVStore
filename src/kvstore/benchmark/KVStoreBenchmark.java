package kvstore.benchmark;

import kvstore.api.KVStore;
import kvstore.core.InMemoryKVStore;
import kvstore.eviction.FIFOPolicy;
import kvstore.eviction.LRUPolicy;
import kvstore.eviction.RandomPolicy;
import kvstore.exception.KeyNotFoundException;
import kvstore.metrics.KVStoreMetricsSnapshot;

import java.util.Random;

public class KVStoreBenchmark {
    private static void runRandomAccessBenchmark(
            String policyName,
            KVStore<Integer, Integer> store,
            int capacity,
            int ops
    ){
        Random rand = new Random(42);

        // Warm up, fill in the cache
        for(int i = 0; i < capacity; i++){
            store.put(i, i);
        }

        for(int i = 0; i < ops; i++){
            try {
                //Making some misses on purpose
                int key = rand.nextInt(capacity * 2);
                store.get(key);
            } catch (KeyNotFoundException ignored) {

            }

        }

        KVStoreMetricsSnapshot metrics = store.getMetrics();
        System.out.println("*** Random Access Benchmark (" + policyName + ") ***");
        System.out.println(metrics);
    }

    private static void runSequentialAccessBenchmark(
            String policyName,
            KVStore<Integer, Integer> store,
            int capacity,
            int ops
    ){
        //Warm up
        for(int i = 0; i < capacity; i++){
            store.put(i, i);
        }

        for(int i = 0; i < ops; i++){
            try{
                int key = i % capacity;
                store.get(key);
            } catch (KeyNotFoundException ignored) {
                // The misses are expected
            }

        }

        KVStoreMetricsSnapshot metrics = store.getMetrics();
        System.out.println("*** Sequential Access Benchmark (" + policyName + ") ***");
        System.out.println(metrics);
    }

    private static void runRandomAccessBenchmarkWithTTL(
            String policyName,
            KVStore<Integer, Integer> store,
            int capacity,
            int ops,
            long ttlMillis
    ){
        Random rand = new Random(42);
        for(int i = 0; i < capacity; i++){
            if(i % 2 == 0){
                store.put(i, i, ttlMillis);
            } else {
                store.put(i,i);
            }
        }

        for(int i = 0; i < ops; i++){
            try{
                int key = rand.nextInt(capacity * 2);
                store.get(key);
            } catch (KeyNotFoundException ignored) {}
        }

        KVStoreMetricsSnapshot metrics = store.getMetrics();
        System.out.println("*** Random Access Benchmark with TTL (" + policyName + ") ***");
        System.out.println("TTL = " + ttlMillis + " ms");
        System.out.println(metrics);
    }

    public static void main(String[] args){
        int capacity = 100;
        int operations = 100_000;

        runSequentialAccessBenchmark(
                "LRU",
                new InMemoryKVStore<>(capacity, new LRUPolicy<>()),
                capacity,
                operations
        );
        runRandomAccessBenchmark(
                "LRU",
                new InMemoryKVStore<>(capacity, new LRUPolicy<>()),
                capacity,
                operations
        );
        runRandomAccessBenchmarkWithTTL(
                "LRU",
                new InMemoryKVStore<>(capacity, new LRUPolicy<>()),
                capacity,
                operations,
                50   // short TTL
        );
        runSequentialAccessBenchmark(
                "FIFO",
                new InMemoryKVStore<>(capacity, new FIFOPolicy<>()),
                capacity,
                operations
        );
        runRandomAccessBenchmark(
                "FIFO",
                new InMemoryKVStore<>(capacity, new FIFOPolicy<>()),
                capacity,
                operations
        );
        runSequentialAccessBenchmark(
                "Random",
                new InMemoryKVStore<>(capacity, new RandomPolicy<>()),
                capacity,
                operations
        );
        runRandomAccessBenchmark(
                "Random",
                new InMemoryKVStore<>(capacity, new RandomPolicy<>()),
                capacity,
                operations
        );

        int[] capacities = {50, 100, 150};

        for(int cap : capacities){
            runRandomAccessBenchmark(
                    "LRU",
                    new InMemoryKVStore<>(cap, new LRUPolicy<>()),
                    cap,
                    operations
            );

            runRandomAccessBenchmark(
                    "FIFO",
                    new InMemoryKVStore<>(cap, new FIFOPolicy<>()),
                    cap,
                    operations
            );

            runRandomAccessBenchmark(
                    "Random",
                    new InMemoryKVStore<>(cap, new RandomPolicy<>()),
                    cap,
                    operations
            );
        }
    }
}