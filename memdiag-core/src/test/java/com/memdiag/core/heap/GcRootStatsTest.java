package com.memdiag.core.heap;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GcRootStatsTest {

    @Test
    void createEmptyStats() {
        GcRootStats stats = new GcRootStats();

        assertThat(stats.getTotalRoots()).isZero();
        for (GcRootType type : GcRootType.values()) {
            assertThat(stats.getCount(type)).isZero();
        }
    }

    @Test
    void createStatsWithCounts() {
        Map<GcRootType, Long> counts = new EnumMap<>(GcRootType.class);
        counts.put(GcRootType.JNI_LOCAL, 10L);
        counts.put(GcRootType.JNI_GLOBAL, 5L);
        counts.put(GcRootType.THREAD_STACK, 20L);

        GcRootStats stats = new GcRootStats(counts);

        assertThat(stats.getTotalRoots()).isEqualTo(35);
        assertThat(stats.getCount(GcRootType.JNI_LOCAL)).isEqualTo(10);
        assertThat(stats.getCount(GcRootType.JNI_GLOBAL)).isEqualTo(5);
        assertThat(stats.getCount(GcRootType.THREAD_STACK)).isEqualTo(20);
    }

    @Test
    void gcRootTypeEnumValues() {
        assertThat(GcRootType.values()).containsExactlyInAnyOrder(
                GcRootType.SYSTEM_CLASS,
                GcRootType.JNI_LOCAL,
                GcRootType.JNI_GLOBAL,
                GcRootType.STATIC_FIELD,
                GcRootType.INSTANCE_FIELD,
                GcRootType.THREAD_STACK,
                GcRootType.MONITOR,
                GcRootType.OTHER
        );
    }

    @Test
    void createGcRootPath() {
        ObjectId targetId = new ObjectId(123L);
        List<GcRootPath.PathNode> pathNodes = new ArrayList<>();
        pathNodes.add(new GcRootPath.PathNode(
                new ObjectId(1L),
                "java.lang.Class",
                null,
                GcRootType.SYSTEM_CLASS
        ));
        pathNodes.add(new GcRootPath.PathNode(
                new ObjectId(2L),
                "com.example.MyClass",
                "instanceField",
                GcRootType.INSTANCE_FIELD
        ));

        GcRootPath rootPath = GcRootPath.create(targetId, pathNodes);

        assertThat(rootPath.getTargetObjectId()).isEqualTo(targetId);
        assertThat(rootPath.getPathNodes()).hasSize(2);
        assertThat(rootPath.getDepth()).isEqualTo(2);
        assertThat(rootPath.getRootType()).isEqualTo(GcRootType.SYSTEM_CLASS);
    }

    @Test
    void gcRootPathGetRoot() {
        ObjectId targetId = new ObjectId(123L);
        List<GcRootPath.PathNode> pathNodes = new ArrayList<>();
        GcRootPath.PathNode rootNode = new GcRootPath.PathNode(
                new ObjectId(1L),
                "java.lang.Class",
                null,
                GcRootType.SYSTEM_CLASS
        );
        pathNodes.add(rootNode);

        GcRootPath rootPath = GcRootPath.create(targetId, pathNodes);

        assertThat(rootPath.getRoot()).isEqualTo(rootNode);
    }

    @Test
    void gcRootPathEmptyPath() {
        ObjectId targetId = new ObjectId(123L);
        GcRootPath rootPath = GcRootPath.create(targetId, new ArrayList<>());

        assertThat(rootPath.getDepth()).isZero();
        assertThat(rootPath.getRoot()).isNull();
        assertThat(rootPath.getRootType()).isEqualTo(GcRootType.OTHER);
    }

    @Test
    void objectIdCreation() {
        ObjectId id1 = new ObjectId(1L);
        ObjectId id2 = new ObjectId(1L);
        ObjectId id3 = new ObjectId(2L);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void objectIdToString() {
        ObjectId id = new ObjectId(12345L);

        assertThat(id.toString()).contains("12345");
    }

    @Test
    void objectIdGetValue() {
        ObjectId id = new ObjectId(98765L);

        assertThat(id.getId()).isEqualTo(98765L);
    }
}
