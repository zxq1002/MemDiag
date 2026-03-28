package com.memdiag.agent.collect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A thread-safe, fixed-size ring buffer for storing allocation events.
 * Uses a lock-free design for high performance.
 */
public class AllocationRingBuffer {

    private final AtomicReferenceArray<AllocationEvent> buffer;
    private final int capacity;
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Create a new ring buffer with the specified capacity.
     */
    public AllocationRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new AtomicReferenceArray<>(capacity);
    }

    /**
     * Add an event to the buffer. If the buffer is full, the oldest event is overwritten.
     */
    public void add(AllocationEvent event) {
        int index = writeIndex.getAndUpdate(i -> (i + 1) % capacity);
        buffer.set(index, event);

        // Update count (cap at capacity)
        int currentCount;
        do {
            currentCount = count.get();
            if (currentCount >= capacity) {
                break;
            }
        } while (!count.compareAndSet(currentCount, currentCount + 1));
    }

    /**
     * Get the most recent events, up to the specified limit.
     */
    public List<AllocationEvent> getRecent(int limit) {
        List<AllocationEvent> result = new ArrayList<>();
        int currentCount = Math.min(count.get(), capacity);

        if (currentCount == 0) {
            return result;
        }

        // Start from the oldest event and read forward
        int startIndex = (writeIndex.get() - currentCount + capacity) % capacity;

        for (int i = 0; i < Math.min(limit, currentCount); i++) {
            int index = (startIndex + i) % capacity;
            AllocationEvent event = buffer.get(index);
            if (event != null) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Get all events in the buffer.
     */
    public List<AllocationEvent> getAll() {
        return getRecent(capacity);
    }

    /**
     * Get the number of events currently in the buffer.
     */
    public int size() {
        return Math.min(count.get(), capacity);
    }

    /**
     * Get the capacity of the buffer.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Check if the buffer is empty.
     */
    public boolean isEmpty() {
        return count.get() == 0;
    }

    /**
     * Check if the buffer is full.
     */
    public boolean isFull() {
        return count.get() >= capacity;
    }

    /**
     * Clear the buffer.
     */
    public void clear() {
        for (int i = 0; i < capacity; i++) {
            buffer.set(i, null);
        }
        writeIndex.set(0);
        count.set(0);
    }

    /**
     * Get events within the specified time window (in milliseconds).
     */
    public List<AllocationEvent> getEventsInWindow(long windowMs) {
        List<AllocationEvent> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        long cutoff = now - windowMs;

        for (AllocationEvent event : getAll()) {
            if (event.getTimestamp() >= cutoff) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Get the total bytes allocated in the specified time window.
     */
    public long getTotalBytesInWindow(long windowMs) {
        long total = 0;
        long cutoff = System.currentTimeMillis() - windowMs;

        for (AllocationEvent event : getAll()) {
            if (event.getTimestamp() >= cutoff) {
                total += event.getSize();
            }
        }

        return total;
    }
}
