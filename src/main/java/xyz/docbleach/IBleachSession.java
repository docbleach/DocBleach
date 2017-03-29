package xyz.docbleach;

/**
 * A Bleach Session manages the environment for a given situation : {bleach, runtime}. It is aware
 * of the presence of a prompt, if one is available for bleaches to fetch information from the user,
 * and the bleaches call it back to store useful data, only threats for now.
 */
public interface IBleachSession {

  /**
   * When running in batch mode, no interaction is expected between the bleach and the user. This
   * way, if the bleach needs an information it is either able to prompt the user (interactive), or
   * guess the best action to do (failure is an acceptable behavior).
   *
   * @return false if a prompt ({@link System#console()}) is available
   */
  boolean isBatchMode();

  /**
   * The BleachSession is able to record threats encountered by the bleach.
   *
   * @param name an arbitrary name given to the threat by the bleach
   * @param severityLevel A severity level, to be considered as arbitrary because it can't be judged
   * (the bleach isn't aware of the end user environment: Does he use Linux or Windows? Has he
   * disabled macros?)
   */
  void recordThreat(String name, SEVERITY severityLevel);

  enum SEVERITY {
    LOW(1),
    MEDIUM(3),
    HIGH(5),
    EXTREME(10);

    private final int points;

    SEVERITY(int points) {
      this.points = points;
    }

    /**
     * @return an arbitrary score tied to the severity, to compare threats
     */
    public int getPoints() {
      return points;
    }
  }
}
