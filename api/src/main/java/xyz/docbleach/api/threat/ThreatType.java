package xyz.docbleach.api.threat;

/**
 * Used to group threats together
 */
public enum ThreatType {
    /**
     * Macros, JavaScript, ActiveX, AcroForms
     */
    ACTIVE_CONTENT,

    /**
     * Word Template linking to an external "thing", ...
     */
    EXTERNAL_CONTENT,

    /**
     * OLE Objects, ...
     */
    BINARY_CONTENT,

    /**
     * OLE Objects, strange image in a document ...
     */
    UNRECOGNIZED_CONTENT
}