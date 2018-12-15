package xyz.docbleach.module.pdf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.threat.Threat;
import xyz.docbleach.api.threat.ThreatAction;
import xyz.docbleach.api.threat.ThreatSeverity;
import xyz.docbleach.api.threat.ThreatType;

class PdfBleachSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(PdfBleachSession.class);
  private static final String[] COMMON_PASSWORDS =
      new String[]{null, "", "test", "example", "sample", "malware", "infected", "password"};
  private static final MemoryUsageSetting MEMORY_USAGE_SETTING =
      MemoryUsageSetting.setupMixed(1024 * 100);

  private final BleachSession session;
  private final COSObjectBleach cosObjectBleach;

  PdfBleachSession(BleachSession session) {
    this.session = session;
    cosObjectBleach = new COSObjectBleach(this);
  }

  void sanitize(RandomAccessRead source, OutputStream outputStream)
      throws IOException, BleachException {
    final PDDocument doc = getDocument(source);

    final PDDocumentCatalog docCatalog = doc.getDocumentCatalog();

    sanitizeNamed(doc, docCatalog.getNames());
    PDDocumentCatalogBleach catalogBleach = new PDDocumentCatalogBleach(this);
    catalogBleach.sanitize(docCatalog);
    sanitizeDocumentOutline(doc.getDocumentCatalog().getDocumentOutline());

    cosObjectBleach.sanitizeObjects(doc.getDocument().getObjects());

    doc.save(outputStream);
    doc.close();
  }

  private void sanitizeDocumentOutline(PDDocumentOutline documentOutline) {
    if (documentOutline == null) {
      return;
    }
    documentOutline.children().forEach(this::sanitizeDocumentOutlineItem);
  }

  private void sanitizeDocumentOutlineItem(PDOutlineItem item) {
    if (item.getAction() == null) {
      return;
    }
    LOGGER.debug("Found&removed action on outline item (was {})", item.getAction());
    item.setAction(null);
    recordJavascriptThreat("DocumentOutline Item Action", "Action");
  }

  private void sanitizeNamed(PDDocument doc, PDDocumentNameDictionary names) {
    if (names == null) {
      return;
    }

    new PDEmbeddedFileBleach(this, doc).sanitize(names.getEmbeddedFiles());

    if (names.getJavaScript() != null) {
      recordJavascriptThreat("Named JavaScriptAction", "Action");
      names.setJavascript(null);
    }
  }

  private PDDocument getDocument(RandomAccessRead source) throws IOException, BleachException {
    PDDocument doc;
    for (String pwd : COMMON_PASSWORDS) {
      ScratchFile scratchFile = new ScratchFile(MEMORY_USAGE_SETTING);
      doc = testPassword(scratchFile, source, pwd);
      if (doc != null) {
        LOGGER.debug("Password was guessed: '{}'", pwd);
        doc.protect(new StandardProtectionPolicy(pwd, pwd, doc.getCurrentAccessPermission()));
        return doc;
      }
      scratchFile.close();
    }

    // @TODO: fetch password from config?

    throw new BleachException("PDF is protected with an unknown password");
  }

  @SuppressFBWarnings(
      value = "EXS_EXCEPTION_SOFTENING_RETURN_FALSE",
      justification = "This method is an helper to check the password")
  private PDDocument testPassword(ScratchFile inFile, RandomAccessRead source, String password)
      throws IOException {
    PDFParser parser = new PDFParser(source, password, inFile);
    try {
      parser.parse();
      return parser.getPDDocument();
    } catch (InvalidPasswordException e) {
      LOGGER.error("The tested password is invalid");
      return null;
    } finally {
      source.rewind((int) source.getPosition());
    }
  }

  void recordJavascriptThreat(String location, String details) {
    Threat threat = Threat.builder()
        .type(ThreatType.ACTIVE_CONTENT)
        .severity(ThreatSeverity.HIGH)
        .details(details)
        .location(location)
        .action(ThreatAction.REMOVE)
        .build();

    session.recordThreat(threat);
  }

  BleachSession getSession() {
    return session;
  }
}
