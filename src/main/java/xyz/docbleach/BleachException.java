package xyz.docbleach;

/**
 * Wrapper for the Bleach exceptions.
 */
public class BleachException extends Exception {

  public BleachException(Throwable e) {
    super(e);
  }

  public BleachException(String message) {
    super(message);
  }
}