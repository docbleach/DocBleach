package xyz.docbleach.module.zip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.exception.RecursionBleachException;
import xyz.docbleach.api.util.CloseShieldInputStream;
import xyz.docbleach.api.util.StreamUtils;

public class ArchiveBleach implements Bleach {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveBleach.class);
  private static final byte[] ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04};

  @Override
  public boolean handlesMagic(InputStream stream) {
    return StreamUtils.hasHeader(stream, ZIP_MAGIC);
  }

  @Override
  public String getName() {
    return "Zip Bleach";
  }

  @Override
  public void sanitize(InputStream is, OutputStream outputStream, BleachSession session) {
    try {
      System.out.println("detect(): " + ArchiveStreamFactory.detect(is));
    } catch (ArchiveException e) {
      e.printStackTrace();
    }
    ArchiveStreamFactory af = new ArchiveStreamFactory();
    try (ArchiveInputStream i = af.createArchiveInputStream(is)) {
      try (ArchiveOutputStream zipOut = af
          .createArchiveOutputStream(ArchiveStreamFactory.detect(is), outputStream)) {
        ArchiveEntry entry;
        while ((entry = i.getNextEntry()) != null) {
          if (!i.canReadEntryData(entry)) {
            // log something?
            System.out.println("Can't read " + entry.getName());
            continue;
          }

          if (entry.isDirectory()) {
            System.out.println("I'm a dir: " + entry.getName());
            continue;
          }
          System.out.println("I'm a file: " + entry.getName() + ", " + entry.getSize());
          sanitizeFile(session, i, zipOut, entry);
        }
        zipOut.finish();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sanitizeFile(
      BleachSession session, ArchiveInputStream zipIn, ArchiveOutputStream zipOut,
      ArchiveEntry entry)
      throws IOException {
    ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();

    int bytesRead;
    // @TODO: check real file size?
    byte[] tempBuffer = new byte[1024];
    while ((bytesRead = zipIn.read(tempBuffer)) != -1) {
      System.out.println("Read " + bytesRead);
      streamBuilder.write(tempBuffer, 0, bytesRead);
    }
    ByteArrayInputStream bais = new ByteArrayInputStream(streamBuilder.toByteArray());
    CloseShieldInputStream is = new CloseShieldInputStream(new BufferedInputStream(bais));

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
      session.sanitize(is, out);
    } catch (RecursionBleachException e) {
      LOGGER.error("Zip Bomb?", e);
      streamBuilder.writeTo(out);
      // Stream was untouched, o/
    } catch (BleachException e) {
      LOGGER.error("An error occured ", e);
    } finally {
      bais.close();
      is._close();
      streamBuilder.close();
    }

    // Add entry to archive
    // ZipArchiveEntry ze = new ZipArchiveEntry((ZipArchiveEntry) entry);
    zipOut.putArchiveEntry(new ArchiveEntry() {
      @Override
      public String getName() {
        return entry.getName();
      }

      @Override
      public long getSize() {
        return out.size();
      }

      @Override
      public boolean isDirectory() {
        return false;
      }

      @Override
      public Date getLastModifiedDate() {
        return entry.getLastModifiedDate();
      }
    });
    out.writeTo(zipOut);
    zipOut.closeArchiveEntry();

    out.close();
  }

  // Copies everything except size & CRC-32
  private ZipEntry cloneEntry(ZipEntry entry) {
    ZipEntry newEntry = new ZipEntry(entry.getName());

    newEntry.setTime(entry.getTime());
    if (entry.getCreationTime() != null) {
      newEntry.setCreationTime(entry.getCreationTime());
    }
    if (entry.getLastModifiedTime() != null) {
      newEntry.setLastModifiedTime(entry.getLastModifiedTime());
    }
    if (entry.getLastAccessTime() != null) {
      newEntry.setLastAccessTime(entry.getLastAccessTime());
    }
    newEntry.setComment(entry.getComment());
    newEntry.setExtra(entry.getExtra());

    return newEntry;
  }
}
