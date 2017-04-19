package xyz.docbleach.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

@Deprecated // Bad practice?
public class BleachSession implements IBleachSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(BleachSession.class);
    private static final Set<IBleach> bleaches = new HashSet<>();
    private static final String ERROR_CALL_FINDBLEACH = "No bleach was defined, call findBleach before sanitize";

    private InputStream inputStream;
    private OutputStream outputStream;
    private int threats = 0;
    private IBleach bleach = null;

    @SuppressWarnings("unused") // prevent initialisation without parameters
    private BleachSession() {
    }

    public BleachSession(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void findBleach() throws IOException, BleachException {
        for (IBleach tmpBleach : bleaches) {
            if (tmpBleach == null || !tmpBleach.handlesMagic(inputStream)) {
                continue;
            }

            bleach = tmpBleach;
            LOGGER.debug("Found bleach for this file type: {}", bleach.getName());

            return;
        }
        throw new BleachException("Could not find a sanitizer for your file! :(");
    }

    public void sanitize() throws IOException, BleachException {
        if (bleach == null) {
            throw new BleachException(ERROR_CALL_FINDBLEACH);
        }

        bleach.sanitize(inputStream, outputStream, this);
    }

    public void recordThreat(String name, SEVERITY severityLevel) {
        threats += 1;
    }

    public int threatCount() {
        return threats;
    }

    public void registerBleach(IBleach bleach) {
        bleaches.add(bleach);
    }
}
