#ifndef ALLOCATION_TRACKER_H
#define ALLOCATION_TRACKER_H

#include "ring_buffer.h"
#include <cstdint>
#include <atomic>
#include <mutex>
#include <unordered_map>

struct AllocationEvent {
    enum class Type {
        ALLOCATE,
        FREE,
        REALLOCATE
    };

    Type type;
    uintptr_t address;
    uint64_t size;
    uint64_t old_address;
    uint64_t old_size;
    uint64_t thread_id;
    uint64_t timestamp;
};

struct AllocationSite {
    uintptr_t address;
    uint64_t size;
    uint64_t thread_id;
    uint64_t allocation_time;
    bool is_live;
};

class AllocationTracker {
public:
    explicit AllocationTracker(size_t buffer_capacity = 100000);
    ~AllocationTracker();

    void recordAllocation(uintptr_t address, uint64_t size, uint64_t thread_id);
    void recordFree(uintptr_t address, uint64_t thread_id);
    void recordReallocate(uintptr_t old_address, uintptr_t new_address,
                         uint64_t old_size, uint64_t new_size, uint64_t thread_id);

    bool popEvent(AllocationEvent& event);
    size_t getOverflowCount() const;

    void startTracking();
    void stopTracking();
    bool isTracking() const;

    std::vector<AllocationSite> getLiveAllocations() const;
    std::unordered_map<uintptr_t, AllocationSite> getLiveAllocationMap() const;

    size_t getTotalAllocated() const;
    size_t getTotalFreed() const;
    size_t getLiveBytes() const;

private:
    RingBuffer<AllocationEvent>* event_buffer_;
    std::atomic<bool> tracking_;
    std::atomic<uint64_t> total_allocated_;
    std::atomic<uint64_t> total_freed_;

    mutable std::mutex allocation_map_mutex_;
    std::unordered_map<uintptr_t, AllocationSite> live_allocations_;
};

#endif // ALLOCATION_TRACKER_H
