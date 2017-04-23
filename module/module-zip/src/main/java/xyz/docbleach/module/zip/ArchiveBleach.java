package xyz.docbleach.module.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.exception.RecursionBleachException;
import xyz.docbleach.api.util.CloseShieldInputStream;

import java.io.*;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveBleach implements Bleach {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveBleach.class);
    private static final byte[] MAGIC_HEADER = new byte[]{0x50, 0x4B, 0x03, 0x04};

    @Override
    public boolean handlesMagic(InputStream stream) {
        byte[] header = new byte[4];
        stream.mark(4);
        int length;

        try {
            length = stream.read(header);
            stream.reset();
        } catch (IOException e) {
            LOGGER.warn("An exception occured", e);
            return false;
        }

        return length == 4 && Arrays.equals(header, MAGIC_HEADER);
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
        byte[] tempBuffer = new byte[(int) entry.getSize()];
        while ((bytesRead = zipIn.read(tempBuffer)) != -1) {
            streamBuilder.write(tempBuffer, 0, bytesRead);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ByteArrayInputStream bais = new ByteArrayInputStream(streamBuilder.toByteArray());
        CloseShieldInputStream is = new CloseShieldInputStream(new BufferedInputStream(bais));

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

        ZipEntry newEntry = new ZipEntry(entry);
        newEntry.setCompressedSize(-1);
        newEntry.setSize(out.size());
        newEntry.setComment(newEntry.getComment() + " - DocBleach");

        zipOut.putNextEntry(newEntry);
        out.writeTo(zipOut);
        out.close();
    }
}