package xyz.docbleach.api.threat;

import xyz.docbleach.api.BleachSession;

public class ThreatBuilder {

  private ThreatSeverity severity;
  private ThreatType type;
  private ThreatAction action;
  private String location, details;

  private ThreatBuilder() {
  }

  public static ThreatBuilder threat() {
    return new ThreatBuilder();
  }

  public Threat build() {
    return new Threat(type, severity, location, details, action);
  }

  public void record(BleachSession session) {
    session.recordThreat(build());
  }

  public ThreatBuilder severity(ThreatSeverity severity) {
    this.severity = severity;
    return this;
  }

  public ThreatBuilder type(ThreatType type) {
    this.type = type;
    return this;
  }

  public ThreatBuilder action(ThreatAction action) {
    this.action = action;
    return this;
  }

  public ThreatBuilder location(String location) {
    this.location = location;
    return this;
  }

  public ThreatBuilder details(String details) {
    this.details = details;
    return this;
  }
}
