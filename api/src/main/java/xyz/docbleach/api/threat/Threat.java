package xyz.docbleach.api.threat;

import java.io.Serializable;

public class Threat implements Serializable {

  private final ThreatType type;
  private final ThreatSeverity severity;
  private final ThreatAction action;
  private final String location;
  private final String details;

  public Threat(
      ThreatType type,
      ThreatSeverity severity,
      String location,
      String details,
      ThreatAction action) {
    this.type = type;
    this.severity = severity;
    this.action = action;
    this.location = location;
    this.details = details;
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

  @Override
  public String toString() {
    return "Threat{"
        + "type="
        + type
        + ", severity="
        + severity
        + ", action="
        + action
        + ", location='"
        + location
        + '\''
        + ", details='"
        + details
        + '\''
        + '}';
  }
}
