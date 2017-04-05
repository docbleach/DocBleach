package xyz.docbleach.api;

/**
 * Wrapper for the Bleach exceptions.
 */
@Deprecated // Bad practice?
public class BleachException extends Exception {

    public BleachException(Throwable e) {
        super(e);
    }

    public BleachException(String message) {
        super(message);
    }
}