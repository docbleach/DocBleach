package xyz.docbleach.api.bleach;

import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.BleachSession;

import java.io.InputStream;
import java.io.OutputStream;

public interface Bleach {
    /**
     * Checks the magic header of the file and returns true if this bleach is able to sanitize this
     * InputStream.
     * The stream has to {@link InputStream#markSupported support mark}.
     * <p>
     * The Bleach is responsible for the error handling to prevent exceptions.
     *
     * @param stream file from wich we will read the data
     * @return true if this bleach may handle this file, false otherwise
     */
    boolean handlesMagic(InputStream stream);

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
    void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session) throws BleachException;
}