package xyz.docbleach.bleach;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import xyz.docbleach.BleachSession;
import xyz.docbleach.IBleachSession.SEVERITY;

abstract class BleachTestBase {

  void assertThreatsFound(BleachSession session, int n) {
    verify(session, times(n)).recordThreat(anyString(), any(SEVERITY.class));
  }

  void assertThreatsFound(BleachSession session, int n, SEVERITY severity) {
    verify(session, times(n)).recordThreat(anyString(), eq(severity));
  }
}