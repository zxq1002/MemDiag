package com.memdiag.core.nmt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NMT (Native Memory Tracking) Output Parser
 * Enhanced to handle different JVM versions and output styles.
 */
public class NmtParser {

    // Matches lines like: "- Java Heap (reserved=2097152KB, committed=2097152KB)"
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
        "([\\w\\s]+)\\(reserved=(\\d+)(\\w+),\\s*committed=(\\d+)(\\w+)\\)");

    // Matches lines like: " (malloc=1024KB #100)"
    private static final Pattern MALLOC_PATTERN = Pattern.compile(
        "malloc=(\\d+)(\\w+)\\s*#(\\d+)");

    public NmtSnapshot parse(String nmtOutput) throws IOException {
        NmtSnapshot.Builder builder = NmtSnapshot.builder();
        List<NmtMemoryUsage> usages = new ArrayList<>();

        if (nmtOutput == null || nmtOutput.isEmpty()) {
            return builder.build();
        }

        BufferedReader reader = new BufferedReader(new StringReader(nmtOutput));
        String line;
        NmtMemoryUsage.Builder currentUsage = null;

        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("Native Memory Tracking:") || trimmed.startsWith("Total:")) {
                continue;
            }

            // A category line usually starts with a "-" or is a major heading
            if (line.contains("(reserved=")) {
                Matcher categoryMatcher = CATEGORY_PATTERN.matcher(line);
                if (categoryMatcher.find()) {
                    // Save previous category before starting a new one
                    if (currentUsage != null) {
                        usages.add(currentUsage.build());
                    }

                    String categoryRaw = categoryMatcher.group(1).replace("-", "").trim();
                    long reserved = parseValueWithUnit(categoryMatcher.group(2), categoryMatcher.group(3));
                    long committed = parseValueWithUnit(categoryMatcher.group(4), categoryMatcher.group(5));

                    currentUsage = NmtMemoryUsage.builder()
                        .category(NmtCategory.fromString(categoryRaw))
                        .reserved(reserved)
                        .committed(committed);
                }
            } 
            // Look for details within the current category
            else if (currentUsage != null) {
                Matcher mallocMatcher = MALLOC_PATTERN.matcher(line);
                if (mallocMatcher.find()) {
                    long malloced = parseValueWithUnit(mallocMatcher.group(1), mallocMatcher.group(2));
                    long mallocCount = Long.parseLong(mallocMatcher.group(3));
                    // We update the current category if details are provided
                    currentUsage.malloced(malloced).mallocCount(mallocCount);
                }
            }
        }

        // Save last category
        if (currentUsage != null) {
            usages.add(currentUsage.build());
        }

        return builder.usages(usages).build();
    }

    public NmtSnapshot parseDetail(String nmtOutput) throws IOException {
        return parse(nmtOutput);
    }

    private long parseValueWithUnit(String value, String unit) {
        try {
            long val = Long.parseLong(value);
            String u = unit.toUpperCase();
            if (u.equals("KB")) return val * 1024;
            if (u.equals("MB")) return val * 1024 * 1024;
            if (u.equals("GB")) return val * 1024 * 1024 * 1024;
            return val; // B
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
