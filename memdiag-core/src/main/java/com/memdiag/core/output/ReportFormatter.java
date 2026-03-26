package com.memdiag.core.output;

import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;

/**
 * 报告格式化器接口
 */
public interface ReportFormatter {

    /**
     * 格式化完整报告
     */
    String format(HeapHistogram histogram, ThreadDump threadDump, DiagnosisResult diagnosis);

    /**
     * 获取格式名称
     */
    String getFormatName();
}
