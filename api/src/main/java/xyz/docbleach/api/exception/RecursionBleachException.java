package xyz.docbleach.api.exception;

/**
 * Exception thrown when a recursion is prevented, to protect from Zip Bombs for instance.
 */
public class RecursionBleachException extends BleachException {

    /**
     * @param depth The depth of the recursion
     */
    public RecursionBleachException(int depth) {
        super("Recursion exploit? There are already " + depth + " sanitation tasks.");
    }
}
