package com.memdiag.core.nativeapi;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class AllocationPairDetectorTest {

    @Test
    void simpleAllocationAndFree() {
        AllocationPairDetector detector = new AllocationPairDetector();

        AllocationEvent alloc = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(1024)
            .timestamp(Instant.now())
            .build();

        AllocationEvent free = AllocationEvent.builder()
            .type(AllocationEvent.Type.FREE)
            .address(0x1000)
            .timestamp(Instant.now())
            .build();

        detector.processEvent(alloc);
        detector.processEvent(free);

        AllocationTrace trace = detector.buildTrace();

        assertThat(trace.getEvents()).hasSize(2);
        assertThat(trace.getLiveAllocationCount()).isEqualTo(0);
        assertThat(trace.getLiveBytes()).isEqualTo(0);
    }

    @Test
    void detectsUnpairedAllocation() {
        AllocationPairDetector detector = new AllocationPairDetector();

        AllocationEvent alloc = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(1024)
            .timestamp(Instant.now())
            .build();

        detector.processEvent(alloc);
        AllocationTrace trace = detector.buildTrace();

        assertThat(trace.getUnpairedAllocations()).hasSize(1);
        assertThat(trace.getLiveAllocationCount()).isEqualTo(1);
        assertThat(trace.hasLeaks()).isTrue();
    }

    @Test
    void detectsUnpairedFree() {
        AllocationPairDetector detector = new AllocationPairDetector();

        AllocationEvent free = AllocationEvent.builder()
            .type(AllocationEvent.Type.FREE)
            .address(0x1000)
            .timestamp(Instant.now())
            .build();

        detector.processEvent(free);
        AllocationTrace trace = detector.buildTrace();

        assertThat(trace.getUnpairedFrees()).hasSize(1);
    }

    @Test
    void reallocateHandling() {
        AllocationPairDetector detector = new AllocationPairDetector();

        AllocationEvent alloc = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(1024)
            .timestamp(Instant.now())
            .build();

        AllocationEvent realloc = AllocationEvent.builder()
            .type(AllocationEvent.Type.REALLOCATE)
            .oldAddress(0x1000)
            .oldSize(1024)
            .address(0x2000)
            .size(2048)
            .timestamp(Instant.now())
            .build();

        detector.processEvent(alloc);
        detector.processEvent(realloc);

        AllocationTrace trace = detector.buildTrace();

        assertThat(trace.getLiveAllocationCount()).isEqualTo(1);
        assertThat(trace.getLiveBytes()).isEqualTo(2048);
    }

    @Test
    void tracksAllocationSites() {
        AllocationPairDetector detector = new AllocationPairDetector();

        NativeStackFrame frame = NativeStackFrame.builder()
            .functionName("test_alloc")
            .libraryName("libtest.so")
            .build();

        AllocationEvent alloc1 = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(1024)
            .stackTrace(Arrays.asList(frame))
            .timestamp(Instant.now())
            .build();

        AllocationEvent alloc2 = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x2000)
            .size(2048)
            .stackTrace(Arrays.asList(frame))
            .timestamp(Instant.now())
            .build();

        detector.processEvent(alloc1);
        detector.processEvent(alloc2);

        List<AllocationSite> sites = detector.getTopAllocationSites(10);

        assertThat(sites).isNotEmpty();
        AllocationSite site = sites.get(0);
        assertThat(site.getAllocationCount()).isEqualTo(2);
        assertThat(site.getTotalBytesAllocated()).isEqualTo(3072);
    }

    @Test
    void calculatesLiveRatio() {
        AllocationPairDetector detector = new AllocationPairDetector();

        NativeStackFrame frame = NativeStackFrame.builder()
            .functionName("test_func")
            .build();

        AllocationEvent alloc1 = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(100)
            .stackTrace(Arrays.asList(frame))
            .timestamp(Instant.now())
            .build();

        AllocationEvent alloc2 = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x2000)
            .size(100)
            .stackTrace(Arrays.asList(frame))
            .timestamp(Instant.now())
            .build();

        AllocationEvent free = AllocationEvent.builder()
            .type(AllocationEvent.Type.FREE)
            .address(0x1000)
            .stackTrace(Arrays.asList(frame))
            .timestamp(Instant.now())
            .build();

        detector.processEvent(alloc1);
        detector.processEvent(alloc2);
        detector.processEvent(free);

        List<AllocationSite> sites = detector.getTopAllocationSites(10);
        AllocationSite site = sites.get(0);

        assertThat(site.getLiveRatio()).isEqualTo(0.5);
    }

    @Test
    void resetClearsState() {
        AllocationPairDetector detector = new AllocationPairDetector();

        detector.processEvent(AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(1024)
            .timestamp(Instant.now())
            .build());

        detector.reset();

        AllocationTrace trace = detector.buildTrace();
        assertThat(trace.getEvents()).isEmpty();
        assertThat(trace.getLiveAllocationCount()).isEqualTo(0);
    }

    @Test
    void nativeStackFrameToString() {
        NativeStackFrame frame = NativeStackFrame.builder()
            .functionName("malloc")
            .libraryName("libc.so.6")
            .sourceFile("malloc.c")
            .lineNumber(123)
            .instructionAddress(0x7f1234)
            .build();

        String str = frame.toString();

        assertThat(str).contains("malloc");
        assertThat(str).contains("libc.so.6");
        assertThat(str).contains("malloc.c");
        assertThat(str).contains("123");
    }

    @Test
    void allocationEventToString() {
        AllocationEvent alloc = AllocationEvent.builder()
            .type(AllocationEvent.Type.ALLOCATE)
            .address(0x1000)
            .size(4096)
            .build();

        AllocationEvent free = AllocationEvent.builder()
            .type(AllocationEvent.Type.FREE)
            .address(0x1000)
            .build();

        assertThat(alloc.toString()).contains("ALLOCATE");
        assertThat(alloc.toString()).contains("4");
        assertThat(free.toString()).contains("FREE");
    }
}
