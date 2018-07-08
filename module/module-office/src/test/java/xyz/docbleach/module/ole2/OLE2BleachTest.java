package xyz.docbleach.module.ole2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachTestBase;

class OLE2BleachTest extends BleachTestBase {

  private OLE2Bleach instance;

  @BeforeEach
  void setUp() {
    instance = spy(new OLE2Bleach());
  }

  @Test
  void handlesMagic() throws IOException {
    Charset charset = Charset.defaultCharset();
    // Check that empty does not trigger an error
    InputStream invalidInputStream = new ByteArrayInputStream("".getBytes(charset));
    assertFalse(instance.handlesMagic(invalidInputStream));

    // Check that this bleach is sane
    invalidInputStream = new ByteArrayInputStream("Anything".getBytes(charset));
    assertFalse(instance.handlesMagic(invalidInputStream));
  }
}
