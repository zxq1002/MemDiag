package com.memdiag.core.nativeapi;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllocationPairDetector {

    private static final long LEAK_THRESHOLD_BYTES = 1024 * 1024; // 1MB
    private static final Duration LONG_LIVED_THRESHOLD = Duration.ofMinutes(5);

    private final Map<Long, AllocationTrace.AllocationRecord> pendingAllocations = new HashMap<>();
    private final Map<Long, String> addressToSiteSignature = new HashMap<>();
    private final List<AllocationTrace.UnpairedAllocation> unpairedAllocations = new ArrayList<>();
    private final List<AllocationTrace.UnpairedFree> unpairedFrees = new ArrayList<>();
    private final List<AllocationEvent> processedEvents = new ArrayList<>();
    private final Map<String, SiteStats> siteStats = new HashMap<>();

    private static class SiteStats {
        List<NativeStackFrame> stackTrace;
        long allocationCount = 0;
        long totalBytesAllocated = 0;
        long bytesStillLive = 0;
        long freeCount = 0;
        long bytesFreed = 0;

        SiteStats(List<NativeStackFrame> stackTrace) {
            this.stackTrace = stackTrace;
        }

        AllocationSite toSite() {
            return AllocationSite.builder()
                .stackTrace(stackTrace)
                .allocationCount(allocationCount)
                .totalBytesAllocated(totalBytesAllocated)
                .bytesStillLive(bytesStillLive)
                .freeCount(freeCount)
                .bytesFreed(bytesFreed)
                .build();
        }
    }

    public AllocationPairDetector() {
    }

    public void processEvent(AllocationEvent event) {
        processedEvents.add(event);

        switch (event.getType()) {
            case ALLOCATE:
                handleAllocation(event);
                break;
            case FREE:
                handleFree(event);
                break;
            case REALLOCATE:
                handleReallocate(event);
                break;
        }
    }

    private void handleAllocation(AllocationEvent event) {
        long address = event.getAddress();
        String signature = getSiteSignature(event);

        if (pendingAllocations.containsKey(address)) {
            AllocationTrace.AllocationRecord existing = pendingAllocations.remove(address);
            String existingSignature = addressToSiteSignature.remove(address);
            unpairedAllocations.add(new AllocationTrace.UnpairedAllocation(
                AllocationEvent.builder()
                    .type(AllocationEvent.Type.ALLOCATE)
                    .address(existing.getAddress())
                    .size(existing.getSize())
                    .timestamp(existing.getAllocationTime())
                    .stackTrace(existing.getStackTrace())
                    .threadId(existing.getThreadId())
                    .build(),
                "Address reused without free"
            ));
            // Adjust site stats for the leaked allocation
            updateSiteStats(existingSignature, false, existing.getSize());
        }

        AllocationTrace.AllocationRecord record = new AllocationTrace.AllocationRecord(
            address,
            event.getSize(),
            event.getTimestamp(),
            event.getStackTrace(),
            event.getThreadId()
        );
        pendingAllocations.put(address, record);
        addressToSiteSignature.put(address, signature);

        updateSiteStats(signature, true, event.getSize());
    }

    private void handleFree(AllocationEvent event) {
        long address = event.getAddress();

        AllocationTrace.AllocationRecord record = pendingAllocations.remove(address);
        String signature = addressToSiteSignature.remove(address);

        if (record == null || signature == null) {
            unpairedFrees.add(new AllocationTrace.UnpairedFree(
                event,
                "Free of unknown address"
            ));
            return;
        }

        updateSiteStats(signature, false, record.getSize());
    }

    private void handleReallocate(AllocationEvent event) {
        long oldAddress = event.getOldAddress();
        long newAddress = event.getAddress();
        String newSignature = getSiteSignature(event);

        AllocationTrace.AllocationRecord oldRecord = pendingAllocations.remove(oldAddress);
        String oldSignature = addressToSiteSignature.remove(oldAddress);

        if (oldRecord == null || oldSignature == null) {
            unpairedFrees.add(new AllocationTrace.UnpairedFree(
                event,
                "Realloc of unknown old address"
            ));
        } else {
            updateSiteStats(oldSignature, false, oldRecord.getSize());
        }

        AllocationTrace.AllocationRecord newRecord = new AllocationTrace.AllocationRecord(
            newAddress,
            event.getSize(),
            event.getTimestamp(),
            event.getStackTrace(),
            event.getThreadId()
        );
        pendingAllocations.put(newAddress, newRecord);
        addressToSiteSignature.put(newAddress, newSignature);

        updateSiteStats(newSignature, true, event.getSize());
    }

    private void updateSiteStats(String signature, boolean isAlloc, long size) {
        SiteStats stats = siteStats.computeIfAbsent(
            signature,
            k -> new SiteStats(null)
        );

        if (isAlloc) {
            stats.allocationCount++;
            stats.totalBytesAllocated += size;
            stats.bytesStillLive += size;
        } else {
            stats.freeCount++;
            stats.bytesFreed += size;
            stats.bytesStillLive = Math.max(0, stats.bytesStillLive - size);
        }
    }

    private String getSiteSignature(AllocationEvent event) {
        if (event.getStackTrace() == null || event.getStackTrace().isEmpty()) {
            return "unknown";
        }
        StringBuilder sb = new StringBuilder();
        for (NativeStackFrame frame : event.getStackTrace()) {
            if (frame.getFunctionName() != null) {
                sb.append(frame.getFunctionName());
            } else {
                sb.append(String.format("0x%016x", frame.getInstructionAddress()));
            }
            sb.append("|");
        }
        return sb.toString();
    }

    public AllocationTrace buildTrace() {
        Instant startTime = processedEvents.isEmpty() ? Instant.now() :
            processedEvents.get(0).getTimestamp();
        Instant endTime = processedEvents.isEmpty() ? Instant.now() :
            processedEvents.get(processedEvents.size() - 1).getTimestamp();

        List<AllocationTrace.UnpairedAllocation> finalUnpaired = new ArrayList<>(unpairedAllocations);
        for (Map.Entry<Long, AllocationTrace.AllocationRecord> entry : pendingAllocations.entrySet()) {
            AllocationTrace.AllocationRecord record = entry.getValue();
            finalUnpaired.add(new AllocationTrace.UnpairedAllocation(
                AllocationEvent.builder()
                    .type(AllocationEvent.Type.ALLOCATE)
                    .address(record.getAddress())
                    .size(record.getSize())
                    .timestamp(record.getAllocationTime())
                    .stackTrace(record.getStackTrace())
                    .threadId(record.getThreadId())
                    .build(),
                "Still live at trace end"
            ));
        }

        AllocationTrace.Builder builder = AllocationTrace.builder()
            .startTime(startTime)
            .endTime(endTime)
            .events(processedEvents)
            .liveAllocations(pendingAllocations)
            .unpairedAllocations(finalUnpaired)
            .unpairedFrees(unpairedFrees);

        return builder.build();
    }

    public List<AllocationSite> getTopAllocationSites(int limit) {
        return siteStats.values().stream()
            .map(SiteStats::toSite)
            .sorted(Comparator.comparingLong(AllocationSite::getBytesStillLive).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<LeakCandidate> findLeakCandidates() {
        List<LeakCandidate> candidates = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<Long, AllocationTrace.AllocationRecord> entry : pendingAllocations.entrySet()) {
            AllocationTrace.AllocationRecord record = entry.getValue();

            if (record.getSize() >= LEAK_THRESHOLD_BYTES) {
                Duration age = Duration.between(record.getAllocationTime(), now);
                candidates.add(new LeakCandidate(record, age));
            }
        }

        candidates.sort(Comparator.comparingLong(c -> -c.record.getSize()));
        return candidates;
    }

    public List<LeakCandidate> findLongLivedAllocations() {
        List<LeakCandidate> candidates = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<Long, AllocationTrace.AllocationRecord> entry : pendingAllocations.entrySet()) {
            AllocationTrace.AllocationRecord record = entry.getValue();
            Duration age = Duration.between(record.getAllocationTime(), now);

            if (age.compareTo(LONG_LIVED_THRESHOLD) >= 0) {
                candidates.add(new LeakCandidate(record, age));
            }
        }

        candidates.sort(Comparator.comparing(c -> -c.age.toMillis()));
        return candidates;
    }

    public static class LeakCandidate {
        private final AllocationTrace.AllocationRecord record;
        private final Duration age;

        public LeakCandidate(AllocationTrace.AllocationRecord record, Duration age) {
            this.record = record;
            this.age = age;
        }

        public AllocationTrace.AllocationRecord getRecord() { return record; }
        public Duration getAge() { return age; }

        public String getAgeDescription() {
            if (age.toHours() > 0) {
                return age.toHours() + " hours";
            } else if (age.toMinutes() > 0) {
                return age.toMinutes() + " minutes";
            } else {
                return age.getSeconds() + " seconds";
            }
        }
    }

    public void reset() {
        pendingAllocations.clear();
        addressToSiteSignature.clear();
        unpairedAllocations.clear();
        unpairedFrees.clear();
        processedEvents.clear();
        siteStats.clear();
    }
}
