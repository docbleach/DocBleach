package xyz.docbleach.module.ole2;

import static xyz.docbleach.api.threat.ThreatBuilder.threat;

import java.io.IOException;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.threat.Threat;
import xyz.docbleach.api.threat.ThreatAction;
import xyz.docbleach.api.threat.ThreatSeverity;
import xyz.docbleach.api.threat.ThreatType;

public class SummaryInformationSanitiser extends EntryFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SummaryInformationSanitiser.class);
  private static final String NORMAL_TEMPLATE = "Normal.dotm";

  public SummaryInformationSanitiser(BleachSession session) {
    super(session);
  }

  @Override
  public boolean test(Entry entry) {
    String entryName = entry.getName();

    if (!SummaryInformation.DEFAULT_STREAM_NAME.equals(entryName)) {
      return true;
    }

    if (!(entry instanceof DocumentEntry)) {
      return true;
    }

    DocumentEntry dsiEntry = (DocumentEntry) entry;
    sanitizeSummaryInformation(session, dsiEntry);

    return true;
  }

  protected void sanitizeSummaryInformation(BleachSession session, DocumentEntry dsiEntry) {
    if (dsiEntry.getSize() <= 0) {
      return;
    }
    try (DocumentInputStream dis = new DocumentInputStream(dsiEntry)) {
      PropertySet ps = new PropertySet(dis);
      // Useful for debugging purposes
      // LOGGER.debug("PropertySet sections: {}", ps.getSections());
      SummaryInformation dsi = new SummaryInformation(ps);

      sanitizeSummaryInformation(session, dsi);
    } catch (NoPropertySetStreamException
        | UnexpectedPropertySetTypeException
        | IOException e) {
      LOGGER.error("An error occured while trying to sanitize the document entry", e);
    }
  }

  protected void sanitizeSummaryInformation(BleachSession session, SummaryInformation dsi) {
    sanitizeTemplate(session, dsi);
    sanitizeComments(session, dsi);
  }

  protected void sanitizeComments(BleachSession session, SummaryInformation dsi) {
    String comments = dsi.getComments();

    if (comments == null || comments.isEmpty()) {
      return;
    }

    LOGGER.trace("Removing the document's Comments (was '{}')", comments);

    dsi.removeComments();

    Threat threat =
        threat()
            .type(ThreatType.UNRECOGNIZED_CONTENT)
            .severity(ThreatSeverity.LOW)
            .action(ThreatAction.REMOVE)
            .location("Summary Information - Comment")
            .details("Comment was: '" + comments + "'")
            .build();

    session.recordThreat(threat);
  }

  protected void sanitizeTemplate(BleachSession session, SummaryInformation dsi) {
    String template = dsi.getTemplate();

    if (NORMAL_TEMPLATE.equals(template)) {
      return;
    }

    if (template == null) {
      return;
    }

    LOGGER.trace("Removing the document's template (was '{}')", template);
    dsi.removeTemplate();

    ThreatSeverity severity =
        isExternalTemplate(template) ? ThreatSeverity.HIGH : ThreatSeverity.LOW;

    Threat threat =
        threat()
            .type(ThreatType.EXTERNAL_CONTENT)
            .severity(severity)
            .action(ThreatAction.REMOVE)
            .location("Summary Information - Template")
            .details("Template was: '" + template + "'")
            .build();

    session.recordThreat(threat);
  }

  protected boolean isExternalTemplate(String template) {
    return template.startsWith("http://")
        || template.startsWith("https://")
        || template.startsWith("ftp://");
  }
}
