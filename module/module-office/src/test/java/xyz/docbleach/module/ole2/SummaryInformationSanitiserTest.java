package xyz.docbleach.module.ole2;

import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static xyz.docbleach.api.BleachTestBase.assertThreatsFound;

class SummaryInformationSanitiserTest {
    private SummaryInformationSanitiser instance;
    private BleachSession session;

    @BeforeEach
    void setUp() {
        session = mock(BleachSession.class);
        instance = spy(new SummaryInformationSanitiser(session));
    }

    @Test
    void test1() {
        // Test an invalid stream, should be ignored
        Entry entry = mock(Entry.class);
        doReturn("\005RandomString").when(entry).getName();
        assertTrue(instance.test(entry));
        verify(instance, never()).sanitizeSummaryInformation(eq(session), (DocumentEntry) any());

        // Test a valid stream name, but wrong type (should be ignored)
        reset(entry);
        doReturn(SummaryInformation.DEFAULT_STREAM_NAME).when(entry).getName();
        assertTrue(instance.test(entry));
        verify(instance, never()).sanitizeSummaryInformation(eq(session), (DocumentEntry) any());

        reset(instance, entry);

        // Test a valid SummaryInformation name
        DocumentEntry docEntry = mock(DocumentEntry.class);

        doReturn(SummaryInformation.DEFAULT_STREAM_NAME).when(docEntry).getName();
        doNothing().when(instance).sanitizeSummaryInformation(session, docEntry);
        assertTrue(instance.test(docEntry));
        verify(instance, atLeastOnce()).sanitizeSummaryInformation(session, docEntry);
    }

    @Test
    void sanitizeSummaryInformation() {
    }

    @Test
    void sanitizeSummaryInformation1() {
    }

    @Test
    void sanitizeComments() {
        SummaryInformation si = new SummaryInformation();

        // When no comment is set, no error/threat is thrown
        instance.sanitizeComments(session, si);
        assertThreatsFound(session, 0);

        // When a comment is set, it should be removed
        si.setComments("Hello!");
        instance.sanitizeComments(session, si);
        assertNull(si.getComments());
        assertThreatsFound(session, 1);
    }

    @Test
    void sanitizeTemplate() {
    }

    @Test
    void isExternalTemplate() {
        assertFalse(instance.isExternalTemplate("Normal.dotm"), "The base template is not external");

        assertFalse(instance.isExternalTemplate("my-template.dotm"), "Unknown template");
        assertFalse(instance.isExternalTemplate("hxxp://my-template.dotm"), "Unknown template");

        assertTrue(instance.isExternalTemplate("https://google.com"), "Detects links");
        assertTrue(instance.isExternalTemplate("http://google.com"), "Detects links");
        assertTrue(instance.isExternalTemplate("ftp://google.com"), "Detects links");
    }
}