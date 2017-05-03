package xyz.docbleach.module.ole2;

import org.apache.poi.poifs.filesystem.Entry;
import xyz.docbleach.api.BleachSession;

import java.util.function.Predicate;

/**
 * Defines a content filter for the OLE2 Bleach.
 * The EntryFilter is responsible for filtering unwanted entries.
 * Returning "true" to the test means the entry should be kept.
 * "false" leads to removal of the entry.
 * The EntryFilter may also transform the entry into something else,
 * for instance the "SummaryInformationSanitiser" removes comments,
 * but the Entry is reused.
 */
abstract class EntryFilter implements Predicate<Entry> {
    final BleachSession session;

    EntryFilter(BleachSession session) {
        super();
        this.session = session;
    }
}