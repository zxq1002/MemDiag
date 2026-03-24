package com.memdiag.core.nativeapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcFileSystemNativeAnalyzer implements NativeMemoryAnalyzer {

    private static final Pattern MAPS_PATTERN = Pattern.compile(
        "([0-9a-fA-F]+)-([0-9a-fA-F]+)\\s+([rwxps-]+)\\s+([0-9a-fA-F]+)\\s+([0-9a-fA-F]+:[0-9a-fA-F]+)\\s+(\\d+)\\s*(.*)?"
    );

    private static final Pattern SMAPS_HEADER_PATTERN = Pattern.compile(
        "([0-9a-fA-F]+)-([0-9a-fA-F]+)\\s+([rwxps-]+)\\s+([0-9a-fA-F]+)\\s+([0-9a-fA-F]+:[0-9a-fA-F]+)\\s+(\\d+)\\s*(.*)?"
    );

    private static final Pattern SMAPS_SIZE_PATTERN = Pattern.compile(
        "Size:\\s+(\\d+)\\s+kB"
    );

    private static final Pattern SMAPS_RSS_PATTERN = Pattern.compile(
        "Rss:\\s+(\\d+)\\s+kB"
    );

    private final String pid;

    public ProcFileSystemNativeAnalyzer(String pid) {
        this.pid = pid;
    }

    public ProcFileSystemNativeAnalyzer() {
        this(String.valueOf(ProcessHandle.current().pid()));
    }

    @Override
    public boolean isAvailable() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) {
            return false;
        }
        File procDir = new File("/proc");
        return procDir.exists() && procDir.isDirectory();
    }

    @Override
    public String getPlatform() {
        return "Linux";
    }

    @Override
    public boolean requiresAgent() {
        return false;
    }

    @Override
    public boolean isAgentAttached() {
        return false;
    }

    @Override
    public boolean attachAgent() {
        return false;
    }

    @Override
    public boolean detachAgent() {
        return false;
    }

    @Override
    public NativeMemorySummary getSummary() {
        List<MemoryRegion> regions = getMemoryRegions();
        long totalResident = 0;
        long totalVirtual = 0;

        for (MemoryRegion region : regions) {
            totalResident += region.getResidentSize();
            totalVirtual += region.getSize();
        }

        return NativeMemorySummary.builder()
            .totalResident(totalResident)
            .totalVirtual(totalVirtual)
            .directByteBufferSize(0)
            .jniAllocatedSize(0)
            .threadStackSize(0)
            .codeCacheSize(0)
            .build();
    }

    @Override
    public List<MemoryRegion> getMemoryRegions() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        List<MemoryRegion> regions = new ArrayList<>();
        File smapsFile = new File("/proc/" + pid + "/smaps");

        if (!smapsFile.exists()) {
            // Fall back to maps file
            return getMemoryRegionsFromMaps();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(smapsFile))) {
            String line;
            MemoryRegion.Builder currentBuilder = null;
            long currentSize = 0;
            long currentRss = 0;

            while ((line = reader.readLine()) != null) {
                Matcher headerMatcher = SMAPS_HEADER_PATTERN.matcher(line);
                if (headerMatcher.matches()) {
                    if (currentBuilder != null) {
                        regions.add(currentBuilder
                            .size(currentSize * 1024)
                            .residentSize(currentRss * 1024)
                            .build());
                    }

                    long startAddress = Long.parseLong(headerMatcher.group(1), 16);
                    long endAddress = Long.parseLong(headerMatcher.group(2), 16);
                    String permissions = headerMatcher.group(3);
                    String pathname = headerMatcher.group(7);

                    currentBuilder = MemoryRegion.builder()
                        .startAddress(startAddress)
                        .endAddress(endAddress)
                        .permissions(permissions)
                        .mappingFile(pathname != null ? pathname : "");
                    currentSize = 0;
                    currentRss = 0;
                    continue;
                }

                if (currentBuilder != null) {
                    Matcher sizeMatcher = SMAPS_SIZE_PATTERN.matcher(line);
                    if (sizeMatcher.matches()) {
                        currentSize = Long.parseLong(sizeMatcher.group(1));
                        continue;
                    }

                    Matcher rssMatcher = SMAPS_RSS_PATTERN.matcher(line);
                    if (rssMatcher.matches()) {
                        currentRss = Long.parseLong(rssMatcher.group(1));
                    }
                }
            }

            if (currentBuilder != null) {
                regions.add(currentBuilder
                    .size(currentSize * 1024)
                    .residentSize(currentRss * 1024)
                    .build());
            }

        } catch (Exception e) {
            return Collections.emptyList();
        }

        return regions;
    }

    private List<MemoryRegion> getMemoryRegionsFromMaps() {
        List<MemoryRegion> regions = new ArrayList<>();
        File mapsFile = new File("/proc/" + pid + "/maps");

        if (!mapsFile.exists()) {
            return Collections.emptyList();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(mapsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = MAPS_PATTERN.matcher(line);
                if (matcher.matches()) {
                    long startAddress = Long.parseLong(matcher.group(1), 16);
                    long endAddress = Long.parseLong(matcher.group(2), 16);
                    String permissions = matcher.group(3);
                    String pathname = matcher.group(7);

                    regions.add(MemoryRegion.builder()
                        .startAddress(startAddress)
                        .endAddress(endAddress)
                        .size(endAddress - startAddress)
                        .residentSize(0)
                        .permissions(permissions)
                        .mappingFile(pathname != null ? pathname : "")
                        .build());
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }

        return regions;
    }

    @Override
    public List<LibraryMapping> getLibraryMappings() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        List<LibraryMapping> mappings = new ArrayList<>();
        File mapsFile = new File("/proc/" + pid + "/maps");

        if (!mapsFile.exists()) {
            return Collections.emptyList();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(mapsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = MAPS_PATTERN.matcher(line);
                if (matcher.matches()) {
                    long startAddress = Long.parseLong(matcher.group(1), 16);
                    long endAddress = Long.parseLong(matcher.group(2), 16);
                    String permissions = matcher.group(3);
                    long offset = Long.parseLong(matcher.group(4), 16);
                    String device = matcher.group(5);
                    long inode = Long.parseLong(matcher.group(6));
                    String pathname = matcher.group(7);

                    mappings.add(LibraryMapping.builder()
                        .startAddress(startAddress)
                        .endAddress(endAddress)
                        .permissions(permissions)
                        .offset(offset)
                        .device(device)
                        .inode(inode)
                        .pathname(pathname != null ? pathname : "")
                        .build());
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }

        return mappings;
    }

    @Override
    public NativeDiagnosis analyzeNativeLeaks() {
        NativeDiagnosis.Builder builder = NativeDiagnosis.builder();

        if (!isAvailable()) {
            builder.addWarning("Proc filesystem not available - not a Linux system or /proc not mounted");
            builder.addRecommendation("Run on a Linux system with /proc filesystem mounted");
            return builder.build();
        }

        List<MemoryRegion> regions = getMemoryRegions();
        if (regions.isEmpty()) {
            builder.addWarning("No memory regions found");
            return builder.build();
        }

        long totalRss = regions.stream().mapToLong(MemoryRegion::getResidentSize).sum();
        long totalSize = regions.stream().mapToLong(MemoryRegion::getSize).sum();

        builder.addFinding(String.format("Total virtual memory: %,d bytes (%.2f MB)",
            totalSize, totalSize / (1024.0 * 1024.0)));
        builder.addFinding(String.format("Total resident memory: %,d bytes (%.2f MB)",
            totalRss, totalRss / (1024.0 * 1024.0)));
        builder.addFinding(String.format("Number of memory regions: %d", regions.size()));

        return builder.build();
    }
}
