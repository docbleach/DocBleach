package xyz.docbleach.modules.ole2;

import org.apache.poi.hpsf.*;
import org.apache.poi.poifs.filesystem.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachException;
import xyz.docbleach.api.IBleach;
import xyz.docbleach.api.IBleachSession;
import xyz.docbleach.api.IBleachSession.SEVERITY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Predicate;


/**
 * Sanitizes an OLE2 file (.doc, .xls, .ppt) by copying its elements into a new OLE2 container.
 * Information to be modified (template, ...) are changed on the fly, and entries to be removed are
 * just not copied over. This way, using a simple Visitor, it is possible to add rules applied on
 * each entry.
 */
public class OLE2Bleach implements IBleach {

    private static final Logger LOGGER = LoggerFactory.getLogger(OLE2Bleach.class);
    private static final String MACRO_ENTRY = "Macros";
    private static final String VBA_ENTRY = "VBA";
    private static final String NORMAL_TEMPLATE = "Normal.dotm";


    @Override
    public boolean handlesMagic(InputStream stream) throws IOException {
        return NPOIFSFileSystem.hasPOIFSHeader(stream);
    }

    @Override
    public String getName() {
        return "OLE2 Bleach";
    }

    @Override
    public void sanitize(InputStream inputStream, OutputStream outputStream, IBleachSession session)
            throws BleachException {
        try (
                NPOIFSFileSystem fsIn = new NPOIFSFileSystem(inputStream);
                NPOIFSFileSystem fs = new NPOIFSFileSystem()
        ) {
            DirectoryEntry rootIn = fsIn.getRoot();
            DirectoryEntry root = fs.getRoot();

            LOGGER.debug("Entries before: {}", rootIn.getEntryNames());
            // Save the changes to a new file

            // Returns false if the entry should be removed
            Predicate<Entry> visitor = ((Predicate<Entry>) (e -> true))
                    .and(removeMacros(session))
                    .and(removeTemplate(session));

            rootIn.getEntries().forEachRemaining(entry -> {
                if (!visitor.test(entry)) {
                    return;
                }
                copyNodesRecursively(entry, root);
            });

            LOGGER.debug("Entries after: {}", root.getEntryNames());
            // Save the changes to a new file

            fs.writeFilesystem(outputStream);
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new BleachException(e);
        }
    }

    private void copyNodesRecursively(Entry entry, DirectoryEntry destination) {
        try {
            EntryUtils.copyNodeRecursively(entry, destination);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    Predicate<Entry> removeTemplate(IBleachSession session) {
        return entry -> {
            String entryName = entry.getName();
            if (!SummaryInformation.DEFAULT_STREAM_NAME.equals(entryName)) {
                return true;
            }

            if (!(entry instanceof DocumentEntry)) {
                return true;
            }

            DocumentEntry dsiEntry = (DocumentEntry) entry;
            sanitizeDocumentEntry(session, dsiEntry);

            return true;
        };
    }

    void sanitizeDocumentEntry(IBleachSession session, DocumentEntry dsiEntry) {
        try (DocumentInputStream dis = new DocumentInputStream(dsiEntry)) {
            PropertySet ps = new PropertySet(dis);
            SummaryInformation dsi = new SummaryInformation(ps);
            sanitizeSummaryInformation(session, dsi);
        } catch (NoPropertySetStreamException | UnexpectedPropertySetTypeException | MarkUnsupportedException | IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void sanitizeSummaryInformation(IBleachSession session,
                                            org.apache.poi.hpsf.SummaryInformation dsi) {
        String template = dsi.getTemplate();

        if (template != null) {
            LOGGER.trace("Removing the document's template (was '{}')", template);
            dsi.removeTemplate();
            SEVERITY severity = getTemplateSeverity(template);
            session.recordThreat("Doc. Template", severity);
        }
    }

    SEVERITY getTemplateSeverity(String template) {
        if (NORMAL_TEMPLATE.equalsIgnoreCase(template)) {
            return SEVERITY.MEDIUM;
        }

        if (template.startsWith("http://") ||
                template.startsWith("https://") ||
                template.startsWith("ftp://")) {
            return SEVERITY.EXTREME;
        }

        return SEVERITY.HIGH;
    }

    Predicate<Entry> removeMacros(IBleachSession session) {
        return entry -> {
            String entryName = entry.getName();
            boolean isMacros = MACRO_ENTRY.equalsIgnoreCase(entryName) ||
                    entryName.contains(VBA_ENTRY);
            // Matches _VBA_PROJECT_CUR, VBA, ... :)
            if (!isMacros) {
                return true;
            }
            LOGGER.info("Found Macros, removing them.");
            if (entry instanceof DirectoryEntry) {
                LOGGER.trace("Macros' entries: {}", ((DirectoryEntry) entry).getEntryNames());
            }
            session.recordThreat("Macros", SEVERITY.EXTREME);

            return false;
        };
    }
}