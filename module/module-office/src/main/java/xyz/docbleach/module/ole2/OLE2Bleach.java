package xyz.docbleach.module.ole2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Predicate;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;

/**
 * Sanitizes an OLE2 file (.doc, .xls, .ppt) by copying its elements into a new OLE2 container.
 * Information to be modified (template, ...) are changed on the fly, and entries to be removed are
 * just not copied over. This way, using a simple Visitor, it is possible to add rules applied on
 * each entry.
 */
public class OLE2Bleach implements Bleach {

  private static final Logger LOGGER = LoggerFactory.getLogger(OLE2Bleach.class);

  @Override
  public boolean handlesMagic(InputStream stream) {
    try {
      return stream.available() > 4 && FileMagic.valueOf(stream) == FileMagic.OLE2;
    } catch (Exception e) {
      LOGGER.warn("An exception occured", e);
      return false;
    }
  }

  @Override
  public String getName() {
    return "OLE2 Bleach";
  }

  @Override
  public void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session)
      throws BleachException {
    try (POIFSFileSystem fsIn = new POIFSFileSystem(inputStream);
        POIFSFileSystem fs = new POIFSFileSystem()) {
      // @TODO: Filter based on Storage Class ID - see issue #23
      sanitize(session, fsIn, fs);

      if (ClassID.EXCEL97.equals(fs.getRoot().getStorageClsid())) {
        ExcelRecordCleaner.cleanupAndSaveExcel97(fs, outputStream);
      } else {
        fs.writeFilesystem(outputStream);
      }
    } catch (IOException | IndexOutOfBoundsException e) {
      throw new BleachException(e);
    }
  }

  protected void sanitize(BleachSession session, POIFSFileSystem fsIn, POIFSFileSystem fs) {
    DirectoryEntry rootIn = fsIn.getRoot();
    DirectoryEntry rootOut = fs.getRoot();

    sanitize(session, rootIn, rootOut);
  }

  protected void sanitize(BleachSession session, DirectoryEntry rootIn, DirectoryEntry rootOut) {
    LOGGER.debug("Entries before: {}", rootIn.getEntryNames());
    // Save the changes to a new file

    // Returns false if the entry should be removed
    Predicate<Entry> visitor =
        ((Predicate<Entry>) (e -> true))
            .and(new MacroRemover(session))
            .and(new ObjectRemover(session))
            .and(new SummaryInformationSanitiser(session));

    LOGGER.debug("Root ClassID: {}", rootIn.getStorageClsid());
    // https://blogs.msdn.microsoft.com/heaths/2006/02/27/identifying-windows-installer-file-types/
    rootOut.setStorageClsid(rootIn.getStorageClsid());

    rootIn
        .getEntries()
        .forEachRemaining(
            entry -> {
              if (!visitor.test(entry)) {
                return;
              }
              copyNodesRecursively(session, entry, rootOut);
            });

    LOGGER.debug("Entries after: {}", rootOut.getEntryNames());
    // Save the changes to a new file
  }

  protected void copyNodesRecursively(BleachSession session, Entry entry, DirectoryEntry target) {
    LOGGER.trace("copyNodesRecursively: {}, parent: {}", entry.getName(), entry.getParent());
    try {
      if (!entry.isDirectoryEntry()) {
        DocumentEntry dentry = (DocumentEntry) entry;
        DocumentInputStream dstream = new DocumentInputStream(dentry);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
          session.sanitize(dstream, os);
        } catch (BleachException e) {
          LOGGER.error("An error occured", e);
          return;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());

        target.createDocument(dentry.getName(), bais);
        dstream.close();
        return;
      }

      DirectoryEntry dirEntry = (DirectoryEntry) entry;
      DirectoryEntry newTarget = target.createDirectory(entry.getName());
      newTarget.setStorageClsid(dirEntry.getStorageClsid());

      sanitize(session, dirEntry, newTarget);
    } catch (IOException e) {
      LOGGER.error("An error occured while trying to recursively copy nodes", e);
    }
  }
}
