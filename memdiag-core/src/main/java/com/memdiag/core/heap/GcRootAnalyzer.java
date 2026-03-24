package com.memdiag.core.heap;

import java.util.List;

public interface GcRootAnalyzer {
    /**
     * 查找指向指定对象的所有 GC Root 引用链
     *
     * @param objectId 目标对象 ID
     * @param maxDepth 最大搜索深度
     * @param maxPaths 最多返回的路径数量
     * @return GC Root 引用链列表
     */
    List<GcRootPath> findGcRoots(ObjectId objectId, int maxDepth, int maxPaths);

    /**
     * 获取所有 GC Root 对象统计
     *
     * @return 按类型统计的 GC Root 数量
     */
    GcRootStats getGcRootStats();
}
