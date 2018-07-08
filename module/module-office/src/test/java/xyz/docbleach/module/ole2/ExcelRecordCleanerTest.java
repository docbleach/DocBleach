package xyz.docbleach.module.ole2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.UnknownRecord;
import org.junit.jupiter.api.Test;

class ExcelRecordCleanerTest {

  @Test
  void removeObProjRecord() {
    Record valid1 = new UnknownRecord(0x01, new byte[]{});
    Record obProj1 = new UnknownRecord(0xD3, new byte[]{});
    Record valid2 = new UnknownRecord(0x02, new byte[]{});
    Collection<Record> records = new HashSet<>();
    records.add(valid1);
    records.add(obProj1);
    records.add(valid2);

    ExcelRecordCleaner.removeObProjRecord(records);

    assertTrue(records.contains(valid1), "A valid record is not removed");
    assertTrue(records.contains(valid2), "A valid record is not removed");
    assertFalse(records.contains(obProj1), "The ObProj record is removed");
  }
}
