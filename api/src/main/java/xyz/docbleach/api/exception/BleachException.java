package xyz.docbleach.api.exception;

/**
 * Wrapper for the Bleach exceptions. Represents a fatal error that prevents the Bleach from
 * working, aborting its operations. Any non fatal error should be handled by the bleach.
 */
public class BleachException extends Exception {

  public BleachException(Throwable e) {
    super(e);
  }

  public BleachException(String message) {
    super(message);
  }
}
