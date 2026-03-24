package com.memdiag.core.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JmxClientTest {

    @Test
    void canGetMemoryMXBean() {
        JmxClient client = JmxClient.attachToCurrentJvm();
        assertThat(client.getHeapMemoryUsage()).isNotNull();
        assertThat(client.getHeapMemoryUsage().getUsed()).isGreaterThanOrEqualTo(0);
    }
}
