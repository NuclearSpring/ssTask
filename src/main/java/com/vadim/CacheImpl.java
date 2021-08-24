package com.vadim;

import com.sun.istack.internal.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Neither keys nor values can't be null. Keys because null is a bad choice of a special value, and value because null will cause factory method to be called repeatedly
 * For proper lock-free implementation see ConcurrentHashMap.computeIfAbsent(). This one is using striped lock.
 * @param <K> key type
 * @param <V> value type
 */
public class CacheImpl<K, V> implements Cache<K, V> {
    static class Bucket<K, V> {
        Map<K, V> map;
        ReadWriteLock mutex;

        public Bucket() {
            map = new HashMap<>();
            mutex = new ReentrantReadWriteLock();
        }
    }

    int capacity = 32; // why not
    Bucket<K, V>[] buckets;
    Function<K, V> factory;

    public CacheImpl(Function<K, V> factory) {
        buckets = new Bucket[capacity];
        for (int i = 0; i < capacity; i++) {
            buckets[i] = new Bucket<>();
        }
        this.factory = factory;
    }

    @Override
    public V get(K key) {
        int hash = hash(key);
        Bucket<K, V> bucket = buckets[hash];
        bucket.mutex.readLock().lock();
        V value = bucket.map.get(key);
        bucket.mutex.readLock().unlock();
        if (value == null){
            Lock writeLock = bucket.mutex.writeLock();
            writeLock.lock();
            try {
                value = bucket.map.get(key);
                if (value == null) {
                    value = factory.apply(key);
                    if (value == null)
                        throw new NullPointerException("Factory method returned null for key " + key);
                    bucket.map.put(key, value);
                }
            } finally {
                writeLock.unlock();
            }
        }
        return value;
    }

    private int hash(K key) {
        int h;
        int hash = ((h = key.hashCode()) ^ (h >>> 16)) & (capacity - 1);
        return hash;
    }
}
