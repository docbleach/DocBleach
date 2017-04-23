package xyz.docbleach.api.bleach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.util.CloseShieldInputStream;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class CompositeBleach implements Bleach {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeBleach.class);
    private final Collection<Bleach> bleaches = new HashSet<>();
    private final String name;

    public CompositeBleach(Bleach... bleaches) {
        Collections.addAll(this.bleaches, bleaches);

        name = buildName(bleaches);
    }

    private String buildName(Bleach[] bleaches) {
        StringBuilder myName = new StringBuilder("CompositeBleach: ");
        for (Bleach b : bleaches) {
            myName.append(b.getName()).append(" ");
        }
        return myName.toString().trim();
    }

    @Override
    public boolean handlesMagic(InputStream stream) {
        return bleaches.stream().anyMatch(bleach -> bleach.handlesMagic(stream));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session) throws BleachException {
        ByteArrayOutputStream os = null;
        CloseShieldInputStream is = new CloseShieldInputStream(inputStream);

        for (Bleach b : bleaches) {
            if (os != null) {
                is = new BufferedInputStream(new ByteArrayInputStream(os.toByteArray()));
            }

            if (!b.handlesMagic(is))
                continue;

            os = new ByteArrayOutputStream();
            b.sanitize(is, os, session);
        }

        try {
            if (os == null) {
                //no bleach is able to handle this file
                copy(is, outputStream);
            } else {
                os.writeTo(outputStream);
            }
        } catch (IOException e) {
            LOGGER.error("Could not copy streams", e);
        }
    }

    private void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[100];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }
}
