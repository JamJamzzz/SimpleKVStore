package kvstore.test;

import kvstore.api.KVStore;
import kvstore.core.InMemoryKVStore;
import kvstore.eviction.FIFOPolicy;
import kvstore.eviction.LRUPolicy;

import kvstore.exception.CapacityExceededException;
import kvstore.exception.KeyNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

import kvstore.metrics.KVStoreMetricsSnapshot;
import org.junit.jupiter.api.Test;

public class KVStoreTest {

    @Test
    public void testZeroCapacityStore() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InMemoryKVStore<>(0, new LRUPolicy<>());
        });
    }

    @Test
    public void testNegativeCapacityStore() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InMemoryKVStore<>(-1, new LRUPolicy<>());
        });
    }

    @Test
    public void testGetMissingKeyThrowsException() {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        assertThrows(KeyNotFoundException.class, () -> {
            store.get("missing");
        });
    }


    @Test
    public void testSingleCapacityEviction() {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(1, new LRUPolicy<>());

        store.put("a", 1);
        store.put("b", 2);

        assertFalse(store.containsKey("a"));
        assertTrue(store.containsKey("b"));
        assertEquals(1, store.size());
    }

    @Test
    public void testDeleteMissingKeyThrowsException() {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        assertThrows(KeyNotFoundException.class, () -> {
            store.delete("missing");
        });
    }

    @Test
    public void testGetUpdatesLRUOrder() {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        store.put("a", 1);
        store.put("b", 2);

        store.get("a");      // a becomes most recently used
        store.put("c", 3);  // b should be evicted

        assertTrue(store.containsKey("a"));
        assertTrue(store.containsKey("c"));
        assertFalse(store.containsKey("b"));
    }

    @Test
    public void testIteratorReturnsAllEntries() {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(3, new LRUPolicy<>());

        store.put("a", 1);
        store.put("b", 2);
        store.put("c", 3);

        int count = 0;
        for (var entry : store) {
            count++;
        }

        assertEquals(3, count);
    }

    @Test
    public void testFIFOEvictionOrder() {
        KVStore<Integer, Integer> store =
                new InMemoryKVStore<>(2, new FIFOPolicy<>());

        store.put(1, 1);
        store.put(2, 2);
        store.put(3, 3);

        assertFalse(store.containsKey(1));
        assertTrue(store.containsKey(2));
        assertTrue(store.containsKey(3));
    }

    @Test
    public void testExpiredEntryBehavesAsAbsent() throws InterruptedException {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        store.put("a", 1, 50); // 50 ms TTL
        Thread.sleep(80);

        assertThrows(KeyNotFoundException.class, () -> store.get("a"));
        assertFalse(store.containsKey("a"));
        assertEquals(0, store.size());
    }

    @Test
    public void testTTLExpirationDoesNotCountAsEviction() throws InterruptedException {
        InMemoryKVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        store.put("a", 1, 50);
        store.put("b", 2);

        Thread.sleep(80);

        assertThrows(KeyNotFoundException.class, () -> store.get("a"));

        KVStoreMetricsSnapshot metrics = store.getMetrics();
        assertEquals(0, metrics.evictionCount,
                "TTL expiration should not be counted as eviction");
    }

    @Test
    public void testLRUDoesNotEvictExpiredEntries() throws InterruptedException {
        InMemoryKVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        store.put("a", 1, 50);   // short TTL
        store.put("b", 2);       // long / infinite TTL

        Thread.sleep(80);        // a expires

        store.put("c", 3);       // should NOT trigger eviction

        assertFalse(store.containsKey("a"));
        assertTrue(store.containsKey("b"));
        assertTrue(store.containsKey("c"));

        KVStoreMetricsSnapshot metrics = store.getMetrics();
        assertEquals(0, metrics.evictionCount,
                "Expired entries should not participate in LRU eviction");
    }

    @Test
    public void testPutResetsTTL() throws InterruptedException {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        store.put("a", 1, 100);
        Thread.sleep(50);

        // reset TTL
        store.put("a", 2, 100);
        Thread.sleep(60);

        assertEquals(2, store.get("a"));
    }

    @Test
    public void testContainsKeyWithExpiredEntry() throws InterruptedException {
        KVStore<String, Integer> store =
                new InMemoryKVStore<>(2, new LRUPolicy<>());

        store.put("a", 1, 50);
        Thread.sleep(80);

        assertFalse(store.containsKey("a"));
    }
}
