package xyz.docbleach.module.ole2;

import static xyz.docbleach.api.threat.ThreatBuilder.threat;

import java.util.Set;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.threat.Threat;
import xyz.docbleach.api.threat.ThreatAction;
import xyz.docbleach.api.threat.ThreatSeverity;
import xyz.docbleach.api.threat.ThreatType;

public class ObjectRemover extends EntryFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectRemover.class);
  private static final String COMPOUND_OBJECT_ENTRY = "\u0001CompObj";
  private static final String OBJECT_POOL_ENTRY = "ObjectPool";

  public ObjectRemover(BleachSession session) {
    super(session);
  }

  @Override
  public boolean test(Entry entry) {
    String entryName = entry.getName();

    if (!isObject(entryName)) {
      return true;
    }

    LOGGER.info("Found Compound Objects, removing them.");
    StringBuilder infos = new StringBuilder();
    if (entry instanceof DirectoryEntry) {
      Set<String> entryNames = ((DirectoryEntry) entry).getEntryNames();
      LOGGER.trace("Compound Objects' entries: {}", entryNames);
      infos.append("Entries: ").append(entryNames);
    } else if (entry instanceof DocumentEntry) {
      int size = ((DocumentEntry) entry).getSize();
      infos.append("Size: ").append(size);
    }

    Threat threat =
        threat()
            .type(ThreatType.EXTERNAL_CONTENT)
            .severity(ThreatSeverity.HIGH)
            .action(ThreatAction.REMOVE)
            .location(entryName)
            .details(infos.toString())
            .build();

    session.recordThreat(threat);

    return false;
  }

  protected boolean isObject(String entryName) {
    return OBJECT_POOL_ENTRY.equalsIgnoreCase(entryName)
        || COMPOUND_OBJECT_ENTRY.equalsIgnoreCase(entryName);
  }
}
