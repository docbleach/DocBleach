package xyz.docbleach;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.bleach.IBleach;
import xyz.docbleach.bleach.OLE2Bleach;
import xyz.docbleach.bleach.OOXMLBleach;
import xyz.docbleach.bleach.PdfBleach;
import xyz.docbleach.bleach.RTFBleach;

public class BleachSession implements IBleachSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(BleachSession.class);
  private static final Set<Class<? extends IBleach>> bleaches = new HashSet<>();
  private static final String ERROR_CALL_FINDBLEACH = "No bleach was defined, call findBleach before sanitize";

  static {
    bleaches.add(OLE2Bleach.class);
    bleaches.add(RTFBleach.class);
    bleaches.add(PdfBleach.class);
    bleaches.add(OOXMLBleach.class);
  }

  private InputStream inputStream;
  private OutputStream outputStream;
  private boolean batchMode;
  private int threats = 0;
  private IBleach bleach = null;

  @SuppressWarnings("unused") // prevent initialisation without parameters
  private BleachSession() {
  }

  public BleachSession(InputStream inputStream, OutputStream outputStream, boolean batchMode) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.batchMode = batchMode;
  }

  public void findBleach() throws IOException, BleachException {
    if (LOGGER.isDebugEnabled()) {
      byte[] firstBytes = IOUtils.peekFirst8Bytes(inputStream);
      LOGGER.debug("First 8 bytes: {}", Arrays.toString(firstBytes));
    }

    for (Class<? extends IBleach> bleachClass : bleaches) {
      IBleach tmpBleach = makeBleach(bleachClass);

      if (tmpBleach == null || !tmpBleach.handlesMagic(inputStream)) {
        continue;
      }

      bleach = tmpBleach;
      LOGGER.debug("Found bleach for this file type: {}", bleach.getName());

      return;
    }
    throw new BleachException("Could not find a sanitizer for your file! :(");
  }

  public void sanitize() throws IOException, BleachException {
    if (bleach == null) {
      throw new BleachException(ERROR_CALL_FINDBLEACH);
    }

    bleach.sanitize(inputStream, outputStream, this);
  }

  private IBleach makeBleach(Class<? extends IBleach> bleachClass) {
    IBleach tmpBleach = null;
    try {
      tmpBleach = bleachClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.error("Bleach instantiation threw an error", e);
      } else {
        LOGGER.error(e.getMessage());
      }
    }
    return tmpBleach;
  }

  public void recordThreat(String name, SEVERITY severityLevel) {
    threats += 1;
  }

  public boolean isBatchMode() {
    return batchMode;
  }

  public int threatCount() {
    return threats;
  }
}
