package com.memdiag.core.nativeapi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllocationTrace {
    private final Instant startTime;
    private final Instant endTime;
    private final List<AllocationEvent> events;
    private final Map<Long, AllocationRecord> liveAllocations;
    private final List<UnpairedAllocation> unpairedAllocations;
    private final List<UnpairedFree> unpairedFrees;

    private AllocationTrace(Builder builder) {
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.events = Collections.unmodifiableList(new ArrayList<>(builder.events));
        this.liveAllocations = Collections.unmodifiableMap(new HashMap<>(builder.liveAllocations));
        this.unpairedAllocations = Collections.unmodifiableList(new ArrayList<>(builder.unpairedAllocations));
        this.unpairedFrees = Collections.unmodifiableList(new ArrayList<>(builder.unpairedFrees));
    }

    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public List<AllocationEvent> getEvents() { return events; }
    public Map<Long, AllocationRecord> getLiveAllocations() { return liveAllocations; }
    public List<UnpairedAllocation> getUnpairedAllocations() { return unpairedAllocations; }
    public List<UnpairedFree> getUnpairedFrees() { return unpairedFrees; }

    public long getTotalAllocatedBytes() {
        return events.stream()
            .filter(e -> e.getType() == AllocationEvent.Type.ALLOCATE)
            .mapToLong(AllocationEvent::getSize)
            .sum();
    }

    public long getTotalFreedBytes() {
        return events.stream()
            .filter(e -> e.getType() == AllocationEvent.Type.FREE)
            .count();
    }

    public long getLiveBytes() {
        return liveAllocations.values().stream()
            .mapToLong(AllocationRecord::getSize)
            .sum();
    }

    public int getLiveAllocationCount() {
        return liveAllocations.size();
    }

    public boolean hasLeaks() {
        return !unpairedAllocations.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class AllocationRecord {
        private final long address;
        private final long size;
        private final Instant allocationTime;
        private final List<NativeStackFrame> stackTrace;
        private final long threadId;

        public AllocationRecord(long address, long size, Instant allocationTime,
                               List<NativeStackFrame> stackTrace, long threadId) {
            this.address = address;
            this.size = size;
            this.allocationTime = allocationTime;
            this.stackTrace = stackTrace;
            this.threadId = threadId;
        }

        public long getAddress() { return address; }
        public long getSize() { return size; }
        public Instant getAllocationTime() { return allocationTime; }
        public List<NativeStackFrame> getStackTrace() { return stackTrace; }
        public long getThreadId() { return threadId; }
    }

    public static class UnpairedAllocation {
        private final AllocationEvent allocationEvent;
        private final String reason;

        public UnpairedAllocation(AllocationEvent allocationEvent, String reason) {
            this.allocationEvent = allocationEvent;
            this.reason = reason;
        }

        public AllocationEvent getAllocationEvent() { return allocationEvent; }
        public String getReason() { return reason; }
    }

    public static class UnpairedFree {
        private final AllocationEvent freeEvent;
        private final String reason;

        public UnpairedFree(AllocationEvent freeEvent, String reason) {
            this.freeEvent = freeEvent;
            this.reason = reason;
        }

        public AllocationEvent getFreeEvent() { return freeEvent; }
        public String getReason() { return reason; }
    }

    public static class Builder {
        private Instant startTime;
        private Instant endTime;
        private List<AllocationEvent> events = new ArrayList<>();
        private Map<Long, AllocationRecord> liveAllocations = new HashMap<>();
        private List<UnpairedAllocation> unpairedAllocations = new ArrayList<>();
        private List<UnpairedFree> unpairedFrees = new ArrayList<>();

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder events(List<AllocationEvent> events) {
            this.events = new ArrayList<>(events);
            return this;
        }

        public Builder addEvent(AllocationEvent event) {
            this.events.add(event);
            return this;
        }

        public Builder liveAllocations(Map<Long, AllocationRecord> liveAllocations) {
            this.liveAllocations = new HashMap<>(liveAllocations);
            return this;
        }

        public Builder addLiveAllocation(long address, AllocationRecord record) {
            this.liveAllocations.put(address, record);
            return this;
        }

        public Builder unpairedAllocations(List<UnpairedAllocation> unpairedAllocations) {
            this.unpairedAllocations = new ArrayList<>(unpairedAllocations);
            return this;
        }

        public Builder addUnpairedAllocation(UnpairedAllocation unpaired) {
            this.unpairedAllocations.add(unpaired);
            return this;
        }

        public Builder unpairedFrees(List<UnpairedFree> unpairedFrees) {
            this.unpairedFrees = new ArrayList<>(unpairedFrees);
            return this;
        }

        public Builder addUnpairedFree(UnpairedFree unpaired) {
            this.unpairedFrees.add(unpaired);
            return this;
        }

        public AllocationTrace build() {
            return new AllocationTrace(this);
        }
    }
}
