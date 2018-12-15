package xyz.docbleach.module.pdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfBleachTest {

  private PdfBleach instance;

  @BeforeEach
  void setUp() {
    instance = new PdfBleach();
  }

  @Test
  void handlesMagic() {
    Charset charset = Charset.defaultCharset();
    InputStream validInputStream = new ByteArrayInputStream("%PDF1.5".getBytes(charset));
    assertTrue(instance.handlesMagic(validInputStream));

    // Check that empty (shorter than %PDF1.-length) does not trigger an error
    InputStream invalidInputStream = new ByteArrayInputStream("".getBytes(charset));
    assertFalse(instance.handlesMagic(invalidInputStream));

    // Check that this bleach is sane
    invalidInputStream = new ByteArrayInputStream("Anything".getBytes(charset));
    assertFalse(instance.handlesMagic(invalidInputStream));
  }
}
