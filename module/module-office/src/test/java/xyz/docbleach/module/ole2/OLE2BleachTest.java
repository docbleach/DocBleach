package xyz.docbleach.module.ole2;

import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.BleachTestBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OLE2BleachTest extends BleachTestBase {
    private static final String SUMMARY_INFORMATION_ENTRY_NAME = "\005SummaryInformation";
    private OLE2Bleach instance;
    private BleachSession session;

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

    @BeforeEach
    void setUp() {
        instance = spy(new OLE2Bleach());
        session = mock(BleachSession.class);
    }


    @Test
    void removeTemplate() {
        Predicate<Entry> predicate = instance.removeTemplate(session);

        // Test an invalid stream, should be ignored
        Entry entry = mock(Entry.class);
        doReturn("\005RandomString").when(entry).getName();
        assertTrue(predicate.test(entry));
        verify(instance, never()).sanitizeDocumentEntry(eq(session), any());

        // Test a valid stream name, but wrong type (should be ignored)
        reset(entry);
        doReturn(SUMMARY_INFORMATION_ENTRY_NAME).when(entry).getName();
        assertTrue(predicate.test(entry));
        verify(instance, never()).sanitizeDocumentEntry(eq(session), any());

        reset(instance, entry);

        // Test a valid SummaryInformation name
        DocumentEntry docEntry = mock(DocumentEntry.class);

        when(docEntry.getName()).thenReturn(SUMMARY_INFORMATION_ENTRY_NAME);
        doNothing().when(instance).sanitizeDocumentEntry(session, docEntry);
        assertTrue(predicate.test(docEntry));
        verify(instance, atLeastOnce()).sanitizeDocumentEntry(session, docEntry);
    }

    @Test
    void getTemplateSeverity() {
        assertEquals(instance.isExternalTemplate("Normal.dotm"), false, "The base template is not external");

        assertEquals(instance.isExternalTemplate("my-template.dotm"), false, "Unknown template");
        assertEquals(instance.isExternalTemplate("hxxp://my-template.dotm"), false, "Unknown template");

        assertEquals(instance.isExternalTemplate("https://google.com"), true, "Detects links");
        assertEquals(instance.isExternalTemplate("http://google.com"), true, "Detects links");
        assertEquals(instance.isExternalTemplate("ftp://google.com"), true, "Detects links");

    }

    @Test
    void removeMacros() {
        Predicate<Entry> predicate = instance.removeMacros(session);

        Entry entry = mock(Entry.class);
        doReturn("_VBA_PROJECT_CUR").when(entry).getName();
        assertFalse(predicate.test(entry));
        assertThreatsFound(session, 1);
        reset(session);

        doReturn(SUMMARY_INFORMATION_ENTRY_NAME).when(entry).getName();
        assertTrue(predicate.test(entry));
        assertThreatsFound(session, 0);
        reset(session);

        doReturn("RandomName").when(entry).getName();
        assertTrue(predicate.test(entry));
        assertThreatsFound(session, 0);
        reset(session);
    }


    @Test
    void handlesMagic() throws IOException {
        Charset charset = Charset.defaultCharset();
        // Check that empty does not trigger an error
        InputStream invalidInputStream = new ByteArrayInputStream("".getBytes(charset));
        assertFalse(instance.handlesMagic(invalidInputStream));

        // Check that this bleach is sane
        invalidInputStream = new ByteArrayInputStream("Anything".getBytes(charset));
        assertFalse(instance.handlesMagic(invalidInputStream));
    }
}