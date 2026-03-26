package com.memdiag.core.nmt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NMT (Native Memory Tracking) 输出解析器
 * 解析 jcmd <pid> VM.native_memory summary 的输出
 */
public class NmtParser {

    // 匹配内存分类行，例如: "Java Heap (reserved=2097152KB, committed=2097152KB)"
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
        "^\\s*([^\\(]+)\\s*\\(reserved=(\\d+)KB,\\s*committed=(\\d+)KB\\)");

    // 匹配 malloc 信息，例如: "  (malloc=1024KB #100)"
    private static final Pattern MALLOC_PATTERN = Pattern.compile(
        "^\\s*\\(malloc=(\\d+)KB\\s*#(\\d+)\\)");

    // 匹配总计行
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "^\\s*Total:.*reserved=(\\d+)KB,\\s*committed=(\\d+)KB");

    public NmtSnapshot parse(String nmtOutput) throws IOException {
        NmtSnapshot.Builder builder = NmtSnapshot.builder();
        List<NmtMemoryUsage> usages = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new StringReader(nmtOutput));
        String line;
        NmtMemoryUsage.Builder currentUsage = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 检查是否是分类行
            Matcher categoryMatcher = CATEGORY_PATTERN.matcher(line);
            if (categoryMatcher.find()) {
                // 保存之前的分类
                if (currentUsage != null) {
                    usages.add(currentUsage.build());
                }

                String categoryName = categoryMatcher.group(1).trim();
                long reserved = parseKilobytes(categoryMatcher.group(2));
                long committed = parseKilobytes(categoryMatcher.group(3));

                currentUsage = NmtMemoryUsage.builder()
                    .category(NmtCategory.fromString(categoryName))
                    .reserved(reserved)
                    .committed(committed);
                continue;
            }

            // 检查是否是 malloc 行
            Matcher mallocMatcher = MALLOC_PATTERN.matcher(line);
            if (mallocMatcher.find() && currentUsage != null) {
                long malloced = parseKilobytes(mallocMatcher.group(1));
                long mallocCount = Long.parseLong(mallocMatcher.group(2));
                currentUsage.malloced(malloced).mallocCount(mallocCount);
                continue;
            }

            // 检查是否是总计行
            Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
            if (totalMatcher.find()) {
                // 总计信息已经通过各分类求和获得，这里不单独处理
                continue;
            }
        }

        // 保存最后一个分类
        if (currentUsage != null) {
            usages.add(currentUsage.build());
        }

        return builder.usages(usages).build();
    }

    /**
     * 从虚拟内存详细输出解析
     * 格式: jcmd <pid> VM.native_memory detail
     */
    public NmtSnapshot parseDetail(String nmtOutput) throws IOException {
        // 详细输出解析与 summary 类似，但包含更多细节
        // 目前使用与 summary 相同的解析逻辑
        return parse(nmtOutput);
    }

    /**
     * 将 KB 转换为字节
     */
    private long parseKilobytes(String kbString) {
        try {
            long kb = Long.parseLong(kbString);
            return kb * 1024;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
