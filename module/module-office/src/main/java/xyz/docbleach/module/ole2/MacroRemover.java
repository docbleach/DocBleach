package xyz.docbleach.module.ole2;

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

public class MacroRemover extends EntryFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MacroRemover.class);
  private static final String VBA_ENTRY = "VBA";
  private static final String MACRO_ENTRY = "Macros";

  public MacroRemover(BleachSession session) {
    super(session);
  }

  @Override
  public boolean test(Entry entry) {
    String entryName = entry.getName();

    // Matches _VBA_PROJECT_CUR, VBA, ... :)
    if (!isMacro(entryName)) {
      return true;
    }

    LOGGER.info("Found Macros, removing them.");
    StringBuilder infos = new StringBuilder();
    if (entry instanceof DirectoryEntry) {
      Set<String> entryNames = ((DirectoryEntry) entry).getEntryNames();
      LOGGER.trace("Macros' entries: {}", entryNames);
      infos.append("Entries: ").append(entryNames);
    } else if (entry instanceof DocumentEntry) {
      int size = ((DocumentEntry) entry).getSize();
      infos.append("Size: ").append(size);
    }

    Threat threat = Threat.builder()
        .type(ThreatType.ACTIVE_CONTENT)
        .severity(ThreatSeverity.EXTREME)
        .action(ThreatAction.REMOVE)
        .location(entryName)
        .details(infos.toString())
        .build();

    session.recordThreat(threat);

    return false;
  }

  protected boolean isMacro(String entryName) {
    return MACRO_ENTRY.equalsIgnoreCase(entryName) || entryName.contains(VBA_ENTRY);
  }
}
