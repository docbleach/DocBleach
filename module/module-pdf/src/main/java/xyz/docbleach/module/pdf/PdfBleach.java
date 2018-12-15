package xyz.docbleach.module.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessRead;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.util.StreamUtils;

/**
 * PDF parsing is a bit tricky: everything may or may not be linked to additional actions, so we
 * need to treat each and every elements.
 */
public class PdfBleach implements Bleach {

  private static final byte[] PDF_MAGIC = new byte[]{37, 80, 68, 70};

  @Override
  public boolean handlesMagic(InputStream stream) {
    return StreamUtils.hasHeader(stream, PDF_MAGIC);
  }

  @Override
  public String getName() {
    return "PDF Bleach";
  }

  @Override
  public void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session)
      throws BleachException {
    try (RandomAccessRead source = new RandomAccessBufferedFileInputStream(inputStream)) {
      new PdfBleachSession(session).sanitize(source, outputStream);
    } catch (IOException e) {
      throw new BleachException(e);
    }
  }
}