package kvstore.metrics;

/**
 * An immutable snapshot of KVStore runtime metrics.
 *
 * This class represents a read-only, consistent view of metrics collected
 * from a KVStore instance at a specific point in time.
 *
 * Design goals:
 * 1. Prevent any external mutation of internal metrics state
 * 2. Provide pre-computed, human-readable performance indicators
 * 3. Decouple metrics collection from metrics consumption
 *
 * This snapshot is intended for monitoring, benchmarking, and analysis,
 * not for influencing store behavior.
 */
public class KVStoreMetricsSnapshot {
    // Total number of get operations
    public final long totalGets;

    // Total number of put operations
    public final long totalPuts;

    //Number of successful get operations (cache hits)
    public final long hitCount;

    // Number of failed get operations (cache misses)
    public final long missCount;

    // Total number of evictions triggered due to capacity constraints
    public final long evictionCount;

    /*
     * Cache hit rate computed as:
     * hitCount / totalGets
     *
     * Represents as the effectiveness of the eviction policy
     */
    public final double hitRate;

    /*
     * Average latency of the get operations in nanoseconds computed as:
     * totalGetLatencyNanoSec / totalGets
     *
     * Measures the average response time for read requests
     */
    public final double avgGetLatencyNanoSec;

    /*
     * Average latency of the put operations in nanoseconds computed as:
     * totalPutLatencyNanoSec / totalGets
     *
     * Measures the average response time for write requests
     */
    public final double avgPutLatencyNanoSec;

    /**
     * Constructor for an immutable snapshot from accumulated raw metrics
     * @param totalGets total number of get operations
     * @param totalPuts total number of put operations
     * @param hitCount number of cache hits
     * @param missCount number of cache misses
     * @param evictionCount number of evictions
     * @param totalGetLatencyNanoSec cumulative latency of all get operations
     * @param totalPutLatencyNanoSec cumulative latency of all put operations
     */
    public KVStoreMetricsSnapshot(
            long totalGets,
            long totalPuts,
            long hitCount,
            long missCount,
            long evictionCount,
            long totalGetLatencyNanoSec,
            long totalPutLatencyNanoSec
    ){
        this.totalGets = totalGets;
        this.totalPuts = totalPuts;
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.evictionCount = evictionCount;

        this.hitRate = totalGets == 0 ? 0.0 : (double) hitCount / totalGets;

        this.avgGetLatencyNanoSec = totalGets == 0 ? 0.0 : (double) totalGetLatencyNanoSec / totalGets;

        this.avgPutLatencyNanoSec = totalPuts == 0 ? 0.0 : (double) totalPutLatencyNanoSec / totalPuts;
    }

    @Override
    public String toString(){
        return String.format(
                "Gets=%d, Puts=%d, HitRate=%.2f, Evictions=%d, AvgGetLatency(ns)=%.2f",
                totalGets, totalPuts, hitRate, evictionCount, avgGetLatencyNanoSec
        );
    }
}
