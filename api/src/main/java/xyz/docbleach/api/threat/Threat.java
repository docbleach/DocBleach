package xyz.docbleach.api.threat;

import java.io.Serializable;

public class Threat implements Serializable {
    private final ThreatType type;
    private final ThreatSeverity severity;
    private final String location;
    private final String details;
    private final ThreatAction action;

    public Threat(ThreatType type, ThreatSeverity severity, String location, String details, ThreatAction action) {
        this.type = type;
        this.severity = severity;
        this.location = location;
        this.details = details;
        this.action = action;
    }

    public ThreatType getType() {
        return type;
    }

    public ThreatSeverity getSeverity() {
        return severity;
    }

    public String getLocation() {
        return location;
    }

    public String getDetails() {
        return details;
    }

    public ThreatAction getAction() {
        return action;
    }
}