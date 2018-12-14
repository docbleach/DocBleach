package xyz.docbleach.api.threat;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Threat {

  public static Builder builder() {
    return new AutoValue_Threat.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder type(ThreatType value);

    public abstract Builder severity(ThreatSeverity value);

    public abstract Builder action(ThreatAction value);

    public abstract Builder location(String value);

    public abstract Builder details(String value);

    public abstract Threat build();
  }

  public abstract ThreatType type();

  public abstract ThreatSeverity severity();

  public abstract ThreatAction action();

  public abstract String location();

  public abstract String details();
}