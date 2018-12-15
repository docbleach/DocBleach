package xyz.docbleach.module.pdf;

import static org.mockito.Mockito.mock;
import static xyz.docbleach.api.BleachTestBase.assertThreatsFound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachSession;

class PdfBleachSessionTest {

  private PdfBleachSession instance;
  private BleachSession session;

  @BeforeEach
  void setUp() {
    session = mock(BleachSession.class);
    instance = new PdfBleachSession(session);
  }

  @Test
  void recordingJavascriptThreatWorks() {
    instance.recordJavascriptThreat("", "");
    assertThreatsFound(session, 1);
  }
}
