package kvstore.model;

/**
 * Using an abstract data type KVEntry to store the entry of the store
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public class KVEntry<K, V> {
    private final K key;
    private V value;
    private final long expireAt;

    //Default constructor, never expired
    public KVEntry(K key, V value){
        this.key = key;
        this.value = value;
        this.expireAt = Long.MAX_VALUE;
    }

    //With TTl
    public KVEntry(K  key, V value, long ttlMillis){
        this.key = key;
        this.value = value;
        this.expireAt = ttlMillis;
    }

    public K getKey() {
        return key;
    }

    public V getValue(){
        return value;
    }

    public void setValue(V value){
        this.value = value;
    }

    public boolean isExpired(){
        return System.currentTimeMillis() > expireAt;
    }
}
