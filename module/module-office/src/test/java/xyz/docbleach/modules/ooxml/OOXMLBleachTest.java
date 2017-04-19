package xyz.docbleach.modules.ooxml;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachTestBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OOXMLBleachTest extends BleachTestBase {
    // Source: https://msdn.microsoft.com/fr-fr/library/aa338205(v=office.12).aspx
    private static final List<String> DYNAMIC_TYPES = Arrays.asList(
            "application/vnd.ms.powerpoint.template.macroEnabled.12",
            "application/vnd.ms-excel.macroEnabled.12",
            "application/vnd.ms-excel.macroEnabledTemplate.12",
            "application/vnd.ms-office.activeX+xml",
            "application/vnd.ms-office.vbaProject",
            "application/vnd.ms-powerpoint.macroEnabled.12",
            "application/vnd.ms-powerpoint.show.macroEnabled.12",
            "application/vnd.ms-word.document.macroEnabled.12",
            "application/vnd.ms-word.document.macroEnabled.main+xml",
            "application/vnd.ms-word.template.macroEnabled.12",
            "application/vnd.ms-word.template.macroEnabled.main+xml",
            "fake/content_type"
    );

    private static final List<String> NOT_DYNAMIC_TYPES = Arrays.asList(
            "application/x-font",
            "application/vnd.ms-excel.12",
            "application/vnd.ms-excel.addin.12",
            "application/xml",
            "application/vnd.ms-excel.binary.12",
            "application/vnd.ms-excel.binary.12",
            "application/vnd.ms-excel.template.12",
            "application/vnd.ms-metro.core-properties+xml",
            "application/vnd.ms-metro.relationships+xml",
            "application/vnd.ms-office.chart",
            "application/vnd.ms-powerpoint.",
            "application/vnd.ms-powerpoint.main.12+xml",
            "application/vnd.ms-powerpoint.presentation.12",
            "application/vnd.ms-powerpoint.show.12",
            "application/vnd.ms-powerpoint.template.12",
            "application/vnd.ms-word.document.12",
            "application/vnd.ms-word.styles+xml",
            "application/vnd.ms-word.document.main+xml",
            "application/vnd.ms-word.fontTable+xml",
            "application/vnd.ms-word.listDefs+xml",
            "application/vnd.ms-word.settings+xml",
            "application/vnd.ms-word.subDoc+xml",
            "application/vnd.ms-word.template.12",
            "application/vnd.ms-word.template.main+xml",
            "audio/aiff",
            "audio/basic",
            "audio/midi",
            "audio/mp3",
            "audio/mpegurl",
            "audio/wav",
            "audio/x-ms-wax",
            "audio/x-ms-wma",
            "image/bmp",
            "image/gif",
            "image/jpeg",
            "image/png",
            "image/tiff",
            "image/xbm",
            "image/x-icon",
            "video/avi",
            "video/mpeg",
            "video/mpg",
            "video/x-ivf",
            "video/x-ms-asf",
            "video/x-ms-asf-plugin",
            "video/x-ms-wm",
            "video/x-ms-wmv",
            "video/x-ms-wmx",
            "video/x-ms-wvx"
    );
    private OOXMLBleach instance;

    @Test
    @Disabled
    void isForbiddenType() throws InvalidFormatException {
        ContentType ct;

        // Block PostScript
        ct = new ContentType("application/postscript");
        assertTrue(instance.isForbiddenType(ct));
    }

    @Test
    @Disabled
    void noFalsePositiveForbiddenType() throws InvalidFormatException {
        ContentType ct;

        for (String contentType : NOT_DYNAMIC_TYPES) {
            ct = new ContentType(contentType);
            assertFalse(instance.isForbiddenType(ct), contentType + " should not be a forbidden type");
        }
    }

    @Test
    @Disabled
    void remapsMacroEnabledDocumentType() throws InvalidFormatException {
        // Not implemented for now. :(
        ContentType ct;

        for (String contentType : DYNAMIC_TYPES) {
            ct = new ContentType(contentType);

            assertTrue(instance.isForbiddenType(ct), contentType + " should be a forbidden type");
        }
    }

    @BeforeEach
    void setUp() {
        instance = new OOXMLBleach();
    }

    @Test
    void handlesMagic() throws IOException {
        Charset cs = Charset.defaultCharset();

        // Check that empty does not trigger an error
        InputStream invalidInputStream2 = new ByteArrayInputStream("".getBytes(cs));
        assertFalse(instance.handlesMagic(invalidInputStream2));

        // Check that this bleach is sane
        InputStream invalidInputStream3 = new ByteArrayInputStream("Anything".getBytes(cs));
        assertFalse(instance.handlesMagic(invalidInputStream3));
    }

    @Test
    @Disabled
    void ignoresZipFile() throws IOException {
        // Not tested anymore, as opening the file as an archive means reading
        // it as a whole, consuming a bunch of resources.
        Charset cs = Charset.defaultCharset();

        final InputStream invalidInputStream = new ByteArrayInputStream("PK\u0003\u0004".getBytes(cs));
        assertThrows(NotOfficeXmlFileException.class, () -> instance.handlesMagic(invalidInputStream));
    }
}