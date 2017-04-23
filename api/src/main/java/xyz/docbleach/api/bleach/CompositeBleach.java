package xyz.docbleach.api.bleach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.util.CloseShieldInputStream;
import xyz.docbleach.api.util.StreamUtils;

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
            if (os != null && is == null) {
                // We check if "is" is null to prevent useless object creation
                ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());
                is = new CloseShieldInputStream(new BufferedInputStream(bais, bais.available()));

                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (!b.handlesMagic(is))
                continue;

            LOGGER.trace("Using bleach: {}", b.getName());
            os = new ByteArrayOutputStream();
            b.sanitize(is, os, session);
            try {
                is.close();
                is = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (os == null) {
                //no bleach is able to handle this file
                StreamUtils.copy(is, outputStream);
            } else {
                os.writeTo(outputStream);
            }
        } catch (IOException e) {
            LOGGER.error("Could not copy streams", e);
        }
    }
}