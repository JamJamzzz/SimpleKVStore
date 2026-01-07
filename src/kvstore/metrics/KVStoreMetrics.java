package kvstore.metrics;

/**
 * Collects and aggregates runtime metrics for a KVStore instance.
 *
 * This class is responsible for recording low-level operational statistics
 * (counts and latencies) during the execution of the store, but does not
 * participate in any logic such as get/put or eviction decisions.
 *
 * Design principles:
 * 1. Metrics collection is separated from core storage logic
 * 2. Metrics are updated incrementally during runtime
 * 3. Internal state is mutable, but exposed externally only via immutable snapshots
 */
public class KVStoreMetrics {
    /**
     * Global Class Attributes
     */
    //Total number of the get operations been called
    private long totalGets;

    //Total number of the put operations been called
    private long totalPuts;

    //Total time of the successful get operations (cache hits)
    private long hitCount;

    //Total time of the failed get operations (cache misses)
    private long missCount;

    //Total number of the evictions
    private long evictionCount;

    //Cumulative latency of all get operations in nano secs
    private long totalGetLatencyNanoSec;

    //Cumulative latency of all put operations in nano secs
    private long totalPutLatencyNanoSec;

    /* ---------- Record Methods ---------- */

    /**
     * Record a get operations
     * @param hit true if the get resulted in a cache hit, false otherwise
     * @param latencyNanoSec Time in nanoseconds to complete the get operations
     */
    public void recordGet(boolean hit, long latencyNanoSec){
        totalGets++;
        if(hit){
            hitCount++;
        } else {
            missCount++;
        }

        totalGetLatencyNanoSec += latencyNanoSec;
    }

    /**
     * Records a put operations
     * @param latencyNanoSec Time in nanoseconds to complete the put operations
     */
    public void recordPut(long latencyNanoSec){
        totalPuts++;
        totalPutLatencyNanoSec += latencyNanoSec;
    }

    /**
     * Records an eviction event
     *
     * This method should be invoked exactly once for each entry evicted from the store
     */
    public void recordEviction(){
        evictionCount++;
    }

    /* ---------- SnapShot ---------- */

    /**
     * Returns an immutable snapshot of the current metrics state
     *
     * The snapshot provides a read-only view of all collected metrics and computed indicators
     */
    public KVStoreMetricsSnapshot snapshot() {
        return new KVStoreMetricsSnapshot(
          totalGets,
          totalPuts,
          hitCount,
          missCount,
          evictionCount,
          totalGetLatencyNanoSec,
          totalPutLatencyNanoSec
        );
    }
}
