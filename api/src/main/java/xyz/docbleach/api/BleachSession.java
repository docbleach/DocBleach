package xyz.docbleach.api;

import xyz.docbleach.api.threats.Threat;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A Bleach Session handles the data a bleach needs to store: list of the threats removed, for instance
 * May be used in the future to store configuration (file's password, for instance) or callbacks
 */
public class BleachSession {
    private final Collection<Threat> threats = new ArrayList<>();

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
}