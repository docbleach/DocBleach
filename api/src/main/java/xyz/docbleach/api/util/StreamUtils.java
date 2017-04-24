package xyz.docbleach.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

public class StreamUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamUtils.class);

    private StreamUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[100];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    public static boolean hasHeader(InputStream stream, byte[] header) {
        byte[] fileMagic = new byte[header.length];
        int length;

        stream.mark(header.length);

        try {
            length = stream.read(fileMagic);

            if (stream instanceof PushbackInputStream) {
                PushbackInputStream pin = (PushbackInputStream) stream;
                pin.unread(fileMagic, 0, length);
            } else {
                stream.reset();
            }
        } catch (IOException e) {
            LOGGER.warn("An exception occured", e);
            return false;
        }

        return length == header.length && Arrays.equals(fileMagic, header);
    }
}