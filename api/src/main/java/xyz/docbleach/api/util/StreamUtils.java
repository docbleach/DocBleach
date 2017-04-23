package xyz.docbleach.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {
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
}