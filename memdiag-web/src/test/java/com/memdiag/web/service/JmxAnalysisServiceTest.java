package com.memdiag.web.service;

import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JmxAnalysisServiceTest {

    @Mock
    private JmxClient jmxClient;

    @Mock
    private DiagnosisEngine diagnosisEngine;

    @Mock
    private DiagnosisResult diagnosisResult;

    private JmxAnalysisService jmxAnalysisService;

    @BeforeEach
    void setUp() {
        jmxAnalysisService = new JmxAnalysisService();
    }

    @Test
    void canDiagnose() {
        when(diagnosisEngine.analyze()).thenReturn(diagnosisResult);
        
        DiagnosisResult result = jmxAnalysisService.diagnose(diagnosisEngine);
        
        assertThat(result).isEqualTo(diagnosisResult);
        verify(diagnosisEngine).analyze();
    }

    // Note: Testing getHistogram, getThreadDump, and getNmtSnapshot directly
    // would require mocking internal object creation (JmxHeapAnalyzer, etc.)
    // which usually requires mockito-inline or PowerMock.
    // For now, we verify the methods that are easily testable and rely on 
    // integration tests or refactoring for more granular unit testing.
}
