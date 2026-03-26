#include "symbol_cache.h"

#ifdef __linux__
#include <dlfcn.h>
#include <link.h>
#include <unistd.h>
#include <fstream>
#include <sstream>
#endif

SymbolCache::SymbolCache(size_t max_entries)
    : max_entries_(max_entries) {
}

SymbolInfo SymbolCache::resolve(uintptr_t address) {
    std::lock_guard<std::mutex> lock(cache_mutex_);

    // Check cache first
    auto it = addr_to_symbol_.find(address);
    if (it != addr_to_symbol_.end()) {
        hit_count_++;
        return it->second;
    }

    miss_count_++;

    // Cache miss, resolve the symbol
    SymbolInfo info = resolve_internal(address);

    // Add to cache if not full
    if (addr_to_symbol_.size() < max_entries_) {
        addr_to_symbol_[address] = info;
    }

    return info;
}

void SymbolCache::add(uintptr_t address, const SymbolInfo& info) {
    std::lock_guard<std::mutex> lock(cache_mutex_);

    if (addr_to_symbol_.size() >= max_entries_) {
        // Cache is full, clear oldest entries (simple strategy: clear half)
        size_t to_remove = addr_to_symbol_.size() / 2;
        auto it = addr_to_symbol_.begin();
        for (size_t i = 0; i < to_remove && it != addr_to_symbol_.end(); ++i, ++it) {
            addr_to_symbol_.erase(it);
        }
    }

    addr_to_symbol_[address] = info;
}

bool SymbolCache::contains(uintptr_t address) const {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    return addr_to_symbol_.find(address) != addr_to_symbol_.end();
}

void SymbolCache::clear() {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    addr_to_symbol_.clear();
    file_to_symbols_.clear();
    hit_count_ = 0;
    miss_count_ = 0;
}

size_t SymbolCache::size() const {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    return addr_to_symbol_.size();
}

size_t SymbolCache::hit_count() const {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    return hit_count_;
}

size_t SymbolCache::miss_count() const {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    return miss_count_;
}

double SymbolCache::hit_rate() const {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    size_t total = hit_count_ + miss_count_;
    if (total == 0) {
        return 0.0;
    }
    return static_cast<double>(hit_count_) / total;
}

SymbolInfo SymbolCache::resolve_internal(uintptr_t address) {
    SymbolInfo info;
    info.function_name = "";
    info.source_file = "";
    info.line_number = 0;
    info.library_name = get_library_name(address);

#ifdef __linux__
    Dl_info dli;
    if (dladdr(reinterpret_cast<void*>(address), &dli) != 0) {
        if (dli.dli_sname != nullptr) {
            info.function_name = dli.dli_sname;
        }
        if (dli.dli_fname != nullptr) {
            info.library_name = dli.dli_fname;
            // Extract just the filename without path
            size_t last_slash = info.library_name.rfind('/');
            if (last_slash != std::string::npos) {
                info.library_name = info.library_name.substr(last_slash + 1);
            }
        }
    }
#endif

    // If we couldn't get the function name, use the address as hex
    if (info.function_name.empty()) {
        char addr_str[32];
        snprintf(addr_str, sizeof(addr_str), "0x%016lx", static_cast<unsigned long>(address));
        info.function_name = addr_str;
    }

    return info;
}

std::string SymbolCache::get_library_name(uintptr_t address) {
#ifdef __linux__
    // Try to get library name from /proc/self/maps
    std::ifstream maps_file("/proc/self/maps");
    if (!maps_file.is_open()) {
        return "unknown";
    }

    std::string line;
    while (std::getline(maps_file, line)) {
        std::istringstream iss(line);
        uintptr_t start, end;
        char dash;
        if (iss >> std::hex >> start >> dash >> std::hex >> end) {
            if (address >= start && address < end) {
                // The path is at the end of the line
                size_t path_pos = line.find_last_of(' ');
                if (path_pos != std::string::npos && path_pos + 1 < line.size()) {
                    std::string path = line.substr(path_pos + 1);
                    if (!path.empty() && path[0] != '[') {
                        // Extract just the filename
                        size_t last_slash = path.rfind('/');
                        if (last_slash != std::string::npos) {
                            return path.substr(last_slash + 1);
                        }
                        return path;
                    }
                }
                break;
            }
        }
    }
#endif

    return "unknown";
}
