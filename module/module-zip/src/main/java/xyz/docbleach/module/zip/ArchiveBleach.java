package xyz.docbleach.module.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.exception.RecursionBleachException;
import xyz.docbleach.api.util.CloseShieldInputStream;
import xyz.docbleach.api.util.StreamUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
    public void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session) throws BleachException {
        ZipInputStream zipIn = new ZipInputStream(inputStream);
        ZipOutputStream zipOut = new ZipOutputStream(outputStream);

        try {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                LOGGER.trace("Entry: {} - Size: (original: {}, compressed: {})",
                        entry.getName(),
                        entry.getSize(),
                        entry.getCompressedSize());

                if (entry.isDirectory()) {
                    ZipEntry newEntry = new ZipEntry(entry);
                    zipOut.putNextEntry(newEntry);
                } else {
                    sanitizeFile(session, zipIn, zipOut, entry);
                }

                zipOut.closeEntry();
            }

            zipOut.finish();
        } catch (IOException e) {
            LOGGER.error("Error in ArchiveBleach", e);
        }
    }

    private void sanitizeFile(BleachSession session, ZipInputStream zipIn, ZipOutputStream zipOut, ZipEntry entry) throws IOException, BleachException {
        ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();

        int bytesRead;
        // @TODO: check real file size?
        byte[] tempBuffer = new byte[1024];
        while ((bytesRead = zipIn.read(tempBuffer)) != -1) {
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

        ZipEntry newEntry = cloneEntry(entry);
        newEntry.setCompressedSize(-1);
        newEntry.setSize(out.size());
        newEntry.setComment(newEntry.getComment() + " - DocBleach");

        zipOut.putNextEntry(newEntry);
        out.writeTo(zipOut);
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