#ifndef SYMBOL_CACHE_H
#define SYMBOL_CACHE_H

#include <string>
#include <unordered_map>
#include <mutex>
#include <vector>
#include <cstdint>

struct SymbolInfo {
    std::string function_name;
    std::string source_file;
    int line_number;
    std::string library_name;
};

class SymbolCache {
public:
    explicit SymbolCache(size_t max_entries = 10000);
    ~SymbolCache() = default;

    // Disable copy and move
    SymbolCache(const SymbolCache&) = delete;
    SymbolCache& operator=(const SymbolCache&) = delete;
    SymbolCache(SymbolCache&&) = delete;
    SymbolCache& operator=(SymbolCache&&) = delete;

    // Resolve symbol from address, using cache if available
    SymbolInfo resolve(uintptr_t address);

    // Manually add a symbol to cache
    void add(uintptr_t address, const SymbolInfo& info);

    // Check if address is in cache
    bool contains(uintptr_t address) const;

    // Clear the cache
    void clear();

    // Get cache statistics
    size_t size() const;
    size_t hit_count() const;
    size_t miss_count() const;
    double hit_rate() const;

private:
    // Internal method to resolve symbol without cache (platform-specific)
    SymbolInfo resolve_internal(uintptr_t address);

    // Extract library name from address
    std::string get_library_name(uintptr_t address);

    const size_t max_entries_;
    mutable std::mutex cache_mutex_;
    std::unordered_map<uintptr_t, SymbolInfo> addr_to_symbol_;
    std::unordered_map<std::string, std::vector<std::pair<uintptr_t, SymbolInfo>>> file_to_symbols_;

    // Statistics
    mutable size_t hit_count_ = 0;
    mutable size_t miss_count_ = 0;
};

#endif // SYMBOL_CACHE_H
