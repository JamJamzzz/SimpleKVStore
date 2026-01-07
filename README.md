# SimpleKVStore

## Overview
SimpleKVStore is a single-node, in-memory key-value store designed as a
modular and extensible backend component.

It provides basic key-based operations while enforcing capacity constraints
and explicit failure handling through exceptions.

## Project Structure

- `api` – Public KVStore interface
- `core` – In-memory store implementation
- `eviction` – Eviction policy abstractions and implementations
- `metrics` – Runtime metrics and snapshots
- `benchmark` – Workload-driven performance evaluation
- `exception` – Explicit failure signaling
- `test` – Interface-oriented unit tests

## Design

### System State

The KV store maintains the following core state:

- A `Map<K, KVEntry<K, V>>` that stores all key-value entries currently held in
  memory. This map represents the storage state of the system.

- A fixed `capacity` that defines the maximum number of entries the store is
  allowed to hold at any time. This constraint is enforced on every mutating
  operation.

- An optional `EvictionPolicy<K>` that encapsulates eviction-related state and
  logic. When configured, the eviction policy is responsible for tracking access
  patterns and selecting a key to evict once the store reach the capacity.

The KV store itself does not maintain any eviction-specific metadata. All
eviction-related state is fully delegated to the eviction policy, ensuring a
clear separation of concerns between storage management and eviction strategy.

### Invariants
The system enforces the following invariants all the time:
- `store.size <= capacity`
- Each key is unique
- All the public methods preserve the invariants

### Data Model (KVEntry)

The store represents each key-value pair as a `KVEntry` object
instead of storing raw values directly in a `Map<K, V>`.

A `KVEntry` encapsulates a key and its corresponding value as a single logical
unit and serves as the internal data model of the system. The in-memory state
is therefore maintained as:

- `Map<K, KVEntry<K, V>>`

rather than a simple `Map<K, V>`.

This design choice is intentional and provides the following benefits:

- **Separation of concerns**  
  The core storage logic operates on entries, while eviction policies and
  future extensions can reason about entries without affecting the public API.

- **Extensibility**  
  Additional metadata (e.g. access information, TTL, or persistence-related
  data) can be added to `KVEntry` without requiring changes to the store
  interface or external callers.

- **Improved readability and maintainability**  
  Explicitly modeling a storage entry makes the intent of the system clearer
  than manipulating raw map values directly.

While a simple `Map<K, V>` would be sufficient for a minimal implementation,
introducing `KVEntry` is a deliberate design decision to keep the internal
structure closer to real-world backend systems and to avoid future refactoring
as the system evolves.

### Time-To-Live (TTL) and Expiration

The KVStore supports optional time-based expiration (TTL) for individual
entries.

Each `KVEntry` may be associated with an expiration timestamp, after which the
entry is considered invalid and behaves as if it does not exist in the store.

TTL-based expiration is **orthogonal** to capacity-based eviction:
- TTL determines whether an entry is still valid.
- Eviction policies determine which valid entry should be removed when the
  store reached capacity.

Expired entries are removed using a **lazy expiration strategy**. That is,
entries are only checked and removed upon access or prior to capacity-based
eviction. This design avoids background cleanup threads while ensuring that
expired entries do not participate in eviction decisions.

When a key expires:
- `get(key)` throws a `KeyNotFoundException`
- `containsKey(key)` returns `false`
- Expiration does **not** count as an eviction in metrics

Updating an existing key via `put` resets its TTL, consistent with common cache
semantics.

## Failure Handing

### Key Not Found

When a key does not exist in the store, the system throws a
`KeyNotFoundException`.

This applies to:
- `get(key)`
- `delete(key)`

Returning a `null` might be ambiguous, a `null` could either represent a valid stored value or a missing key

By throwing an exception, the system can allow the caller to distinguish between `a successful lookup` or `an invalid access`

### Capacity Exceeded

When the store reaches its capacity limit and eviction is not possible,
a `CapacityExceededException` is thrown.

When the eviction policy is configured, the store will make the eviction decision due to the policy before inserting a new entry

This separation ensures the store logic and the eviction logic remain decoupled.

### Why Exceptions Instead of Return Codes

Using exceptions provides several advantages:
- Failure paths are explicit and can not be ignored.
- Normal control flow remains clean and readable.
- Callers can handle different failures.

## Eviction Policy

The KVStore delegates eviction decisions to an `EvictionPolicy<K>` abstraction.

This design separates storage concerns from eviction logic, allowing different
policies to be plugged in without modifying the core store implementation.

Currently implemented eviction policies include:

- **LRU (Least Recently Used)**  
  Evicts the least recently accessed key. This policy adapts to access
  patterns and performs well under workloads with strong temporal locality.

- **LFU (Least Frequently Used)**  
  Evicts the key with the lowest access frequency.  
  Frequencies are updated on every access, and ties are broken by insertion
  order to ensure deterministic eviction behavior.

- **FIFO (First-In First-Out)**  
  Evicts the earliest inserted key, independent of access frequency or recency.
  This policy provides predictable eviction behavior and serves as a baseline
  for comparison against locality-aware strategies.

- **Random**  
  Evicts a randomly selected key from the current working set.
  This policy does not exploit any access locality and is primarily included
  as a baseline for benchmarking and comparative evaluation of more
  sophisticated eviction strategies.

### LFU Implementation Notes

The LFU eviction policy is implemented with an O(1) access and update path.

- Each key is associated with an access frequency stored in a frequency map.
- Keys are grouped into frequency buckets (`freq -> LinkedHashSet<key>`),
  allowing constant-time promotion on access.
- A `minFreq` pointer is maintained to efficiently identify eviction
  candidates.
- When multiple keys share the same frequency, eviction is resolved
  deterministically based on insertion order within the frequency bucket.

This design avoids heap-based implementations and ensures predictable
performance under high access rates.


## Metrics and Observability

The KVStore exposes runtime metrics to provide visibility into system behavior
under different workloads.

Metrics are collected passively and do not affect the functional behavior of
the store. They are intended for performance analysis and system evaluation.

The following metrics are tracked:

- Total number of `get` operations
- Total number of `put` operations
- Cache hit count and miss count for `get`
- Cache eviction count
- Cumulative and average latency for `get` and `put` operations

A snapshot-based design is used to ensure that metrics can be safely inspected
without mutating internal state.

Eviction metrics only reflect removals caused by capacity pressure.
TTL-based expiration is tracked separately and does not contribute to eviction
counts. This distinction ensures eviction metrics accurately represent cache
pressure rather than natural expiration.


## Benchmark and Evaluation

To validate the correctness and performance characteristics of the store,
a simple benchmark framework is included.

Two access patterns are evaluated:

### Sequential Access
- Working set fits entirely in cache
- Achieves near-100% cache hit rate
- Minimal average access latency

### Random Access
- Working set is twice the cache capacity
- Cache hit rate converges to approximately 50%
- The average access latency is significantly higher due to frequent cache misses

These results align with theoretical cache behavior and validate both the
eviction policies and the metrics implementation.

In particular, locality-aware policies such as LRU consistently outperform
baseline strategies like FIFO under random access workloads, while exhibiting
similar performance under sequential access patterns.

### TTL Impact on Cache Behavior

To evaluate the impact of TTL-based expiration, an additional benchmark was
conducted under random access workloads with a subset of entries configured
with short TTLs.

Under this configuration:
- The cache hit rate decreases as expired entries naturally result in misses.
- The eviction count remains zero, confirming that expiration does not trigger
  capacity-based eviction.
- Average get latency increases slightly due to expiration checks and additional
  cache misses.

These results demonstrate that TTL reduces eviction pressure by allowing stale
entries to expire naturally, at the cost of a lower hit rate under random access
patterns. The observed overhead from lazy expiration is minimal and does not
significantly impact overall system performance.

Under random access workloads, LFU exhibits similar or slightly improved hit
rates compared to LRU when access frequency is skewed, at the cost of higher
metadata maintenance overhead.

## Design Tradeoffs

### Exceptions vs Return Values
The store uses exceptions to signal failure conditions such as missing keys or
capacity exhaustion. This avoids ambiguity and ensures failure paths are explicit.

### Metrics via Exception Paths
Cache misses are represented as exceptions in the `get` path. While this
introduces additional latency, it keeps the API explicit and simplifies
correctness reasoning.

### In-Memory Single-Threaded Design
The current implementation is single-threaded and not thread-safe by design.

## Testing Strategy

### Testing Goals
Tests are designed to ensure that:
- Core operations behave correctly under normal conditions.
- Failure paths are explicit.
- System invariants are preserved across all operations.

### Normal Operation Tests
Basic Operations are verified through tests that cover:
- Inserting and retrieving key-value pairs.
- Updating existing keys.
- Deleting keys and validating whether they should be removed or not.

### Failure Path Tests
Failure scenarios are explicitly tested to ensure correct error signaling:
- Accessing a non-existent key results in a `KeyNotFoundException`.
- Deleting a non-existent key results in a `KeyNotFoundException`.
- Exceeding capacity without an eviction policy results in a
  `CapacityExceededException`.

### Interface Oriented Testing
Tests are written against the `KVStore` interface rather than concrete
implementations. This allows the same test suite to be reused for different storage or
eviction policy implementations without modification.

## Future Work

Possible extensions include:
- Thread-safe implementation
- Additional eviction policies (ARC(Adaptive Replacement Cache) CLOCK / CLOCK-pro)
- Persistence or write-ahead logging
- Concurrent metrics collection


## Summary

SimpleKVStore is a modular, in-memory key-value store that emphasizes explicit
failure handling, eviction policy abstraction, and observability through
metrics evaluation.