package xyz.docbleach.api;

import xyz.docbleach.api.threats.Threat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class BleachTestBase {
    protected void assertThreatsFound(BleachSession session, int n) {
        verify(session, times(n)).recordThreat(any(Threat.class));
    }
}