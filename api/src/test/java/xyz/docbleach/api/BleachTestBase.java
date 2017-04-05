package xyz.docbleach.api;

import xyz.docbleach.api.IBleachSession.SEVERITY;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class BleachTestBase {
    protected void assertThreatsFound(BleachSession session, int n) {
        verify(session, times(n)).recordThreat(anyString(), any(SEVERITY.class));
    }

    protected void assertThreatsFound(BleachSession session, int n, SEVERITY severity) {
        verify(session, times(n)).recordThreat(anyString(), eq(severity));
    }
}