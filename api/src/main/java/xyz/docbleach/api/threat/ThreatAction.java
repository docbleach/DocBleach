package xyz.docbleach.api.threat;

/**
 * Represents the action taken by the bleach
 */
public enum ThreatAction {
    /**
     * No actions had to be taken, for instance if the threat is innocuous.
     */
    NOTHING,

    /**
     * The potential threat has been replaced by an innocuous content.
     * ie: HTML file replaced by a screenshot of the page
     */
    DISARM,

    /**
     * No actions were taken because of the configuration
     */
    IGNORE,

    /**
     * The threat has been removed.
     */
    REMOVE;
}