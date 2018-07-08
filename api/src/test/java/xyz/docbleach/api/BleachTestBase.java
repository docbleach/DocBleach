package xyz.docbleach.api;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import xyz.docbleach.api.threat.Threat;

public abstract class BleachTestBase {

  public static void assertThreatsFound(BleachSession session, int n) {
    verify(session, times(n)).recordThreat(any(Threat.class));
  }
}
