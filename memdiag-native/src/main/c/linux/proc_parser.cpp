#include "proc_parser.h"
#include <fstream>
#include <sstream>
#include <regex>
#include <cstdlib>

std::vector<MemoryRegion> parse_smaps(const std::string& pid) {
    std::vector<MemoryRegion> regions;
    std::string path = "/proc/" + pid + "/smaps";
    std::ifstream file(path);

    if (!file.is_open()) {
        return regions;
    }

    std::string line;
    MemoryRegion current_region;
    bool in_region = false;
    uint64_t current_size = 0;
    uint64_t current_rss = 0;

    std::regex header_regex(R"(([0-9a-fA-F]+)-([0-9a-fA-F]+)\s+([rwxps-]+)\s+([0-9a-fA-F]+)\s+([0-9a-fA-F]+:[0-9a-fA-F]+)\s+(\d+)\s*(.*)?)");
    std::regex size_regex(R"(Size:\s+(\d+)\s+kB)");
    std::regex rss_regex(R"(Rss:\s+(\d+)\s+kB)");

    while (std::getline(file, line)) {
        std::smatch match;

        if (std::regex_match(line, match, header_regex)) {
            if (in_region) {
                current_region.size = current_size * 1024;
                current_region.resident_size = current_rss * 1024;
                regions.push_back(current_region);
            }

            current_region.start_address = std::stoull(match[1].str(), nullptr, 16);
            current_region.end_address = std::stoull(match[2].str(), nullptr, 16);
            current_region.permissions = match[3].str();
            current_region.mapping_file = match.size() > 7 ? match[7].str() : "";
            current_region.region_type = "";
            current_size = 0;
            current_rss = 0;
            in_region = true;
            continue;
        }

        if (in_region) {
            if (std::regex_match(line, match, size_regex)) {
                current_size = std::stoull(match[1].str());
            } else if (std::regex_match(line, match, rss_regex)) {
                current_rss = std::stoull(match[1].str());
            }
        }
    }

    if (in_region) {
        current_region.size = current_size * 1024;
        current_region.resident_size = current_rss * 1024;
        regions.push_back(current_region);
    }

    return regions;
}

std::vector<LibraryMapping> parse_maps(const std::string& pid) {
    std::vector<LibraryMapping> mappings;
    std::string path = "/proc/" + pid + "/maps";
    std::ifstream file(path);

    if (!file.is_open()) {
        return mappings;
    }

    std::string line;
    std::regex regex(R"(([0-9a-fA-F]+)-([0-9a-fA-F]+)\s+([rwxps-]+)\s+([0-9a-fA-F]+)\s+([0-9a-fA-F]+:[0-9a-fA-F]+)\s+(\d+)\s*(.*)?)");

    while (std::getline(file, line)) {
        std::smatch match;
        if (std::regex_match(line, match, regex)) {
            LibraryMapping mapping;
            mapping.start_address = std::stoull(match[1].str(), nullptr, 16);
            mapping.end_address = std::stoull(match[2].str(), nullptr, 16);
            mapping.permissions = match[3].str();
            mapping.offset = std::stoull(match[4].str(), nullptr, 16);
            mapping.device = match[5].str();
            mapping.inode = std::stoull(match[6].str());
            mapping.pathname = match.size() > 7 ? match[7].str() : "";
            mappings.push_back(mapping);
        }
    }

    return mappings;
}
