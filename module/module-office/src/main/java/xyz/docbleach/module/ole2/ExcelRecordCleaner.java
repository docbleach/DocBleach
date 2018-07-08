package xyz.docbleach.module.ole2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelRecordCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelRecordCleaner.class);
  /**
   * ObProj's record SID. From Microsoft's documentation: > The existence of the ObProj record
   * specifies that there is a VBA project in the file. > This project is located in the VBA storage
   * stream.
   */
  private static final short OB_PROJ_SID = 0xD3;

  protected static void cleanupAndSaveExcel97(NPOIFSFileSystem fs, OutputStream outputStream)
      throws IOException {
    Workbook wb = WorkbookFactory.create(fs);

    if (wb instanceof HSSFWorkbook) {
      HSSFWorkbook hwb = (HSSFWorkbook) wb;
      InternalWorkbook internal = hwb.getInternalWorkbook();
      if (internal != null) {
        LOGGER.trace("# of Records: {}", internal.getNumRecords());
        removeObProjRecord(internal.getRecords());
        LOGGER.trace("# of Records: {}", internal.getNumRecords());
      }
    }

    wb.write(outputStream);
  }

  protected static void removeObProjRecord(Collection<Record> records) {
    new HashSet<>(records)
        .forEach(
            record -> {
              if (!isObProj(record)) {
                return;
              }
              records.remove(record);
              LOGGER.debug("Found and removed ObProj record: {}", record);
            });
  }

  protected static boolean isObProj(Record record) {
    return record.getSid() == OB_PROJ_SID;
  }
}
