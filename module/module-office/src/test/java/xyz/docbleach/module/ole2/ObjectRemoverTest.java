package xyz.docbleach.module.ole2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.BleachTestBase;

class ObjectRemoverTest {

  private ObjectRemover instance;
  private BleachSession session;

  @BeforeEach
  void setUp() {
    session = mock(BleachSession.class);
    instance = spy(new ObjectRemover(session));
  }

  @Test
  void testRemovesMacro() {
    Entry entry = mock(Entry.class);
    doReturn("\u0001CompObj").when(entry).getName();
    assertFalse(instance.test(entry), "An object entry should not be copied over");
    BleachTestBase.assertThreatsFound(session, 1);
    reset(session);
  }

  @Test
  void testKeepsEverythingElse() {
    Entry entry = mock(Entry.class);
    doReturn(SummaryInformation.DEFAULT_STREAM_NAME).when(entry).getName();
    assertTrue(instance.test(entry), "Non-object entries should be ignored");
    BleachTestBase.assertThreatsFound(session, 0);
    reset(session);

    doReturn("RandomName").when(entry).getName();
    assertTrue(instance.test(entry), "Non-object entries should be ignored");
    BleachTestBase.assertThreatsFound(session, 0);
    reset(session);
  }

  @Test
  void recognizesObjects() {
    assertTrue(
        instance.isObject("\u0001CompObj"), "u0001CompObj is the Excel Macro directory name");

    assertFalse(instance.isObject("Nothing"), "Nothing is not an object. Is-it?");
  }
}
