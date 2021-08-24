package com.vadim;

public interface Cache<K, V> {
    V get(K key);
}
