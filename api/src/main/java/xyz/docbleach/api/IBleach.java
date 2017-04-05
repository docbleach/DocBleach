package xyz.docbleach.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IBleach {

    /**
     * Checks the magic header of the file and returns true if this bleach is able to sanitize this
     * InputStream.
     * The stream has to {@link InputStream#markSupported support mark}.
     *
     * @param stream file from wich we will read the data
     * @return true if this bleach may handle this file, false otherwise
     * @throws IOException if an error occured while opening/reading the file
     */
    boolean handlesMagic(InputStream stream) throws IOException;

    /**
     * @return this bleach's name
     */
    String getName();

    /**
     * @param inputStream  the file we want to sanitize
     * @param outputStream the sanitized file this bleach will write to
     * @param session      the bleach session that stores threats
     * @throws BleachException Any fatal error that might occur during the bleach
     */
    void sanitize(InputStream inputStream, OutputStream outputStream, IBleachSession session)
            throws BleachException;
}