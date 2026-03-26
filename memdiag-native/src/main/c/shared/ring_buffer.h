#ifndef RING_BUFFER_H
#define RING_BUFFER_H

#include <atomic>
#include <vector>
#include <cstddef>

template<typename T>
class RingBuffer {
public:
    enum class PushResult {
        SUCCESS,
        OVERFLOW_DROPPED
    };

    explicit RingBuffer(size_t capacity);

    PushResult push(const T& item);
    bool pop(T& item);

    size_t get_overflow_count() const;
    size_t capacity() const { return buffer_.capacity() - 1; }

private:
    std::atomic<size_t> write_pos_;
    std::atomic<size_t> read_pos_;
    std::atomic<size_t> overflow_count_;
    std::vector<T> buffer_;
};

// Template implementation - must be in header file
template<typename T>
RingBuffer<T>::RingBuffer(size_t capacity)
    : write_pos_(0), read_pos_(0), overflow_count_(0), buffer_(capacity + 1) {
}

template<typename T>
typename RingBuffer<T>::PushResult RingBuffer<T>::push(const T& item) {
    size_t current_write = write_pos_.load(std::memory_order_relaxed);
    size_t next_write = (current_write + 1) % buffer_.size();

    size_t current_read = read_pos_.load(std::memory_order_acquire);

    if (next_write == current_read) {
        overflow_count_.fetch_add(1, std::memory_order_relaxed);
        return PushResult::OVERFLOW_DROPPED;
    }

    buffer_[current_write] = item;
    write_pos_.store(next_write, std::memory_order_release);
    return PushResult::SUCCESS;
}

template<typename T>
bool RingBuffer<T>::pop(T& item) {
    size_t current_read = read_pos_.load(std::memory_order_relaxed);
    size_t current_write = write_pos_.load(std::memory_order_acquire);

    if (current_read == current_write) {
        return false;
    }

    item = buffer_[current_read];
    size_t next_read = (current_read + 1) % buffer_.size();
    read_pos_.store(next_read, std::memory_order_release);
    return true;
}

template<typename T>
size_t RingBuffer<T>::get_overflow_count() const {
    return overflow_count_.load(std::memory_order_relaxed);
}

#endif // RING_BUFFER_H
