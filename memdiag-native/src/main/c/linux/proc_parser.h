#ifndef PROC_PARSER_H
#define PROC_PARSER_H

#include <string>
#include <vector>
#include <cstdint>

struct MemoryRegion {
    uintptr_t start_address;
    uintptr_t end_address;
    std::string permissions;
    uint64_t size;
    uint64_t resident_size;
    std::string mapping_file;
    std::string region_type;
};

struct LibraryMapping {
    uintptr_t start_address;
    uintptr_t end_address;
    std::string permissions;
    uint64_t offset;
    std::string device;
    uint64_t inode;
    std::string pathname;
};

std::vector<MemoryRegion> parse_smaps(const std::string& pid);
std::vector<LibraryMapping> parse_maps(const std::string& pid);

#endif // PROC_PARSER_H
