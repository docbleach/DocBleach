package xyz.docbleach.api;

import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.threat.Threat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A Bleach Session handles the data a bleach needs to store: list of the threats removed, for instance
 * May be used in the future to store configuration (file's password, for instance) or callbacks
 */
public class BleachSession {
    private final transient Bleach bleach;
    private final Collection<Threat> threats = new ArrayList<>();

    public BleachSession(Bleach bleach) {
        this.bleach = bleach;
    }

    /**
     * The BleachSession is able to record threats encountered by the bleach.
     *
     * @param threat The removed threat object, containing the threat type and more information
     */
    public void recordThreat(Threat threat) {
        if (threat == null)
            return;
        threats.add(threat);
    }

    public Collection<Threat> getThreats() {
        return threats;
    }

    public int threatCount() {
        return threats.size();
    }

    /**
     * @return The bleach used in this session
     */
    public Bleach getBleach() {
        return bleach;
    }

    public void sanitize(InputStream is, OutputStream os) throws BleachException {
        bleach.sanitize(is, os, this);
    }
}