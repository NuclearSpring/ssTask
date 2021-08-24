package com.vadim;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class CacheImplTest {

    public static final int KEYS = 1000000;
    public static final int THREADS = 8;

    @Test
    public void testKeyNull() {
        Cache<Integer, Integer> cache = new CacheImpl<>(k -> k);
        Assert.assertEquals((Integer)1, cache.get(1));
        Assert.assertThrows(NullPointerException.class, () -> cache.get(null));
    }

    @Test
    public void testValueNull(){
        Cache<Integer, Integer> cache = new CacheImpl<>(k -> null);
        Assert.assertThrows(NullPointerException.class, () -> cache.get(1));
    }

    @Test
    public void testCreation() {
        AtomicInteger calls = new AtomicInteger(0);
        Cache<Integer, Integer> cache = new CacheImpl<>(k -> {calls.incrementAndGet();return k;});
        Assert.assertEquals((Integer)1, cache.get(1));
        Assert.assertEquals(1L, calls.get());
        Assert.assertEquals((Integer)1, cache.get(1));
        Assert.assertEquals(1L, calls.get());
    }

    @Test
    public void testThreading() throws InterruptedException {
        Cache<Integer, Long> cache = new CacheImpl<>(k -> Thread.currentThread().getId());
        Thread[] threads = new Thread[THREADS];
        CountDownLatch latch = new CountDownLatch(THREADS);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Long[] values = new Long[KEYS];
                for (int j = 0; j < KEYS; j++) {
                    values[j] = cache.get(j);
                }
                for (int j = 0; j < KEYS; j++) {
                    assertEquals("Value mismatch for key " + j, values[j], cache.get(j));
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }
}