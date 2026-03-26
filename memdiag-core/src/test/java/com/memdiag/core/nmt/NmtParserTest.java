package com.memdiag.core.nmt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class NmtParserTest {

    @Test
    void parseSimpleNmtOutput() throws IOException {
        String nmtOutput = "Native Memory Tracking:\n" +
                "\n" +
                "Total: reserved=1048576KB, committed=524288KB\n" +
                "-                 Java Heap (reserved=524288KB, committed=524288KB)\n" +
                "-                     Class (reserved=102400KB, committed=51200KB)\n" +
                "-                    Thread (reserved=20480KB, committed=10240KB)\n" +
                "-                      Code (reserved=51200KB, committed=25600KB)\n" +
                "-                        GC (reserved=25600KB, committed=12800KB)\n" +
                "-                  Compiler (reserved=10240KB, committed=5120KB)\n" +
                "-                  Internal (reserved=5120KB, committed=2560KB)\n";

        NmtParser parser = new NmtParser();
        NmtSnapshot snapshot = parser.parse(nmtOutput);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getTotalReserved()).isGreaterThan(0);
        assertThat(snapshot.getTotalCommitted()).isGreaterThan(0);
    }

    @Test
    void parseCategories() throws IOException {
        String nmtOutput = "Native Memory Tracking:\n" +
                "\n" +
                "Total: reserved=1048576KB, committed=524288KB\n" +
                "-                 Java Heap (reserved=524288KB, committed=524288KB)\n" +
                "-                     Class (reserved=102400KB, committed=51200KB)\n";

        NmtParser parser = new NmtParser();
        NmtSnapshot snapshot = parser.parse(nmtOutput);

        assertThat(snapshot.getUsages()).hasSizeGreaterThan(0);
    }

    @Test
    void parseEmptyOutputReturnsEmptySnapshot() throws IOException {
        String nmtOutput = "";

        NmtParser parser = new NmtParser();
        NmtSnapshot snapshot = parser.parse(nmtOutput);

        assertThat(snapshot.getTotalReserved()).isZero();
        assertThat(snapshot.getTotalCommitted()).isZero();
        assertThat(snapshot.getUsages()).isEmpty();
    }

    @Test
    void parseInvalidFormatGracefully() throws IOException {
        String nmtOutput = "This is not valid NMT output\n" +
                "It has no proper format\n";

        NmtParser parser = new NmtParser();
        NmtSnapshot snapshot = parser.parse(nmtOutput);

        assertThat(snapshot).isNotNull();
    }

    @Test
    void nmtCategoryEnumValues() {
        assertThat(NmtCategory.values()).contains(
                NmtCategory.JAVA_HEAP,
                NmtCategory.CLASS,
                NmtCategory.THREAD,
                NmtCategory.CODE,
                NmtCategory.GC,
                NmtCategory.COMPILER,
                NmtCategory.INTERNAL,
                NmtCategory.UNKNOWN
        );
    }

    @Test
    void nmtCategoryDisplayName() {
        assertThat(NmtCategory.JAVA_HEAP.getDisplayName()).isEqualTo("Java Heap");
        assertThat(NmtCategory.CLASS.getDisplayName()).isEqualTo("Class");
        assertThat(NmtCategory.THREAD.getDisplayName()).isEqualTo("Thread");
    }

    @Test
    void nmtCategoryFromString() {
        assertThat(NmtCategory.fromString("Java Heap")).isEqualTo(NmtCategory.JAVA_HEAP);
        assertThat(NmtCategory.fromString("Class")).isEqualTo(NmtCategory.CLASS);
        assertThat(NmtCategory.fromString("Unknown")).isEqualTo(NmtCategory.UNKNOWN);
    }

    @Test
    void nmtMemoryUsageBuilder() {
        NmtMemoryUsage usage = NmtMemoryUsage.builder()
                .category(NmtCategory.JAVA_HEAP)
                .reserved(1024L * 1024 * 1024)
                .committed(512L * 1024 * 1024)
                .malloced(10L * 1024 * 1024)
                .mallocCount(1000)
                .build();

        assertThat(usage.getCategory()).isEqualTo(NmtCategory.JAVA_HEAP);
        assertThat(usage.getReserved()).isEqualTo(1024L * 1024 * 1024);
        assertThat(usage.getCommitted()).isEqualTo(512L * 1024 * 1024);
        assertThat(usage.getMalloced()).isEqualTo(10L * 1024 * 1024);
        assertThat(usage.getMallocCount()).isEqualTo(1000);
    }

    @Test
    void nmtSnapshotBuilder() {
        NmtSnapshot snapshot = NmtSnapshot.builder()
                .timestamp(Instant.now())
                .build();

        assertThat(snapshot.getTimestamp()).isNotNull();
        assertThat(snapshot.getUsages()).isEmpty();
    }
}
