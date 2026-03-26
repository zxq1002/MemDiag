#include "allocation_tracker.h"
#include <chrono>

AllocationTracker::AllocationTracker(size_t buffer_capacity)
    : tracking_(false), total_allocated_(0), total_freed_(0) {
    event_buffer_ = new RingBuffer<AllocationEvent>(buffer_capacity);
}

AllocationTracker::~AllocationTracker() {
    delete event_buffer_;
}

void AllocationTracker::recordAllocation(uintptr_t address, uint64_t size, uint64_t thread_id) {
    if (!tracking_.load(std::memory_order_relaxed)) {
        return;
    }

    AllocationEvent event;
    event.type = AllocationEvent::Type::ALLOCATE;
    event.address = address;
    event.size = size;
    event.old_address = 0;
    event.old_size = 0;
    event.thread_id = thread_id;
    event.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();

    event_buffer_->push(event);
    total_allocated_.fetch_add(size, std::memory_order_relaxed);

    std::lock_guard<std::mutex> lock(allocation_map_mutex_);
    AllocationSite site;
    site.address = address;
    site.size = size;
    site.thread_id = thread_id;
    site.allocation_time = event.timestamp;
    site.is_live = true;
    live_allocations_[address] = site;
}

void AllocationTracker::recordFree(uintptr_t address, uint64_t thread_id) {
    if (!tracking_.load(std::memory_order_relaxed)) {
        return;
    }

    AllocationEvent event;
    event.type = AllocationEvent::Type::FREE;
    event.address = address;
    event.size = 0;
    event.old_address = 0;
    event.old_size = 0;
    event.thread_id = thread_id;
    event.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();

    event_buffer_->push(event);

    std::lock_guard<std::mutex> lock(allocation_map_mutex_);
    auto it = live_allocations_.find(address);
    if (it != live_allocations_.end()) {
        total_freed_.fetch_add(it->second.size, std::memory_order_relaxed);
        it->second.is_live = false;
        live_allocations_.erase(it);
    }
}

void AllocationTracker::recordReallocate(uintptr_t old_address, uintptr_t new_address,
                                         uint64_t old_size, uint64_t new_size, uint64_t thread_id) {
    if (!tracking_.load(std::memory_order_relaxed)) {
        return;
    }

    AllocationEvent event;
    event.type = AllocationEvent::Type::REALLOCATE;
    event.address = new_address;
    event.size = new_size;
    event.old_address = old_address;
    event.old_size = old_size;
    event.thread_id = thread_id;
    event.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();

    event_buffer_->push(event);

    std::lock_guard<std::mutex> lock(allocation_map_mutex_);

    // Remove old allocation
    auto old_it = live_allocations_.find(old_address);
    if (old_it != live_allocations_.end()) {
        total_freed_.fetch_add(old_it->second.size, std::memory_order_relaxed);
        live_allocations_.erase(old_it);
    }

    // Add new allocation
    total_allocated_.fetch_add(new_size, std::memory_order_relaxed);
    AllocationSite site;
    site.address = new_address;
    site.size = new_size;
    site.thread_id = thread_id;
    site.allocation_time = event.timestamp;
    site.is_live = true;
    live_allocations_[new_address] = site;
}

bool AllocationTracker::popEvent(AllocationEvent& event) {
    return event_buffer_->pop(event);
}

size_t AllocationTracker::getOverflowCount() const {
    return event_buffer_->get_overflow_count();
}

void AllocationTracker::startTracking() {
    tracking_.store(true, std::memory_order_release);
}

void AllocationTracker::stopTracking() {
    tracking_.store(false, std::memory_order_release);
}

bool AllocationTracker::isTracking() const {
    return tracking_.load(std::memory_order_acquire);
}

std::vector<AllocationSite> AllocationTracker::getLiveAllocations() const {
    std::lock_guard<std::mutex> lock(allocation_map_mutex_);
    std::vector<AllocationSite> result;
    result.reserve(live_allocations_.size());
    for (const auto& pair : live_allocations_) {
        result.push_back(pair.second);
    }
    return result;
}

std::unordered_map<uintptr_t, AllocationSite> AllocationTracker::getLiveAllocationMap() const {
    std::lock_guard<std::mutex> lock(allocation_map_mutex_);
    return live_allocations_;
}

size_t AllocationTracker::getTotalAllocated() const {
    return total_allocated_.load(std::memory_order_relaxed);
}

size_t AllocationTracker::getTotalFreed() const {
    return total_freed_.load(std::memory_order_relaxed);
}

size_t AllocationTracker::getLiveBytes() const {
    std::lock_guard<std::mutex> lock(allocation_map_mutex_);
    size_t total = 0;
    for (const auto& pair : live_allocations_) {
        total += pair.second.size;
    }
    return total;
}
