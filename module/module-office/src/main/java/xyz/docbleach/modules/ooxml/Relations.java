package xyz.docbleach.modules.ooxml;

@SuppressWarnings("ALL")
public final class Relations {
    private static final String HYPERLINK = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink";

    // Macro
    private static final String VBA_PROJECT = "http://schemas.microsoft.com/office/2006/relationships/vbaProject";
    private static final String VBA_PROJECT_SIGNATURE = "http://schemas.microsoft.com/office/2006/relationships/vbaProjectSignature";
    private static final String WORD_VBA_DATA = "http://schemas.microsoft.com/office/2006/relationships/wordVbaData";

    // OLE Objects
    private static final String OPENXML_OLE_OBJECT = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/oleObject";
    private static final String OLE_OBJECT = "http://schemas.microsoft.com/office/2006/relationships/oleObject";
    private static final String E2_OBJECT = "http://schemas.microsoft.com/office/2006/relationships/e2Object";
    private static final String E1_OBJECT = "http://schemas.microsoft.com/office/2006/relationships/e1Object";

    // ActiveX Controls
    private static final String OPENXML_CONTROL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/control";
    private static final String OPENXML_ACTIVEX_CONTROL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/activeXControl";
    private static final String OPENXML_ACTIVEX_CONTROL_BIN = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/activeXControlBinary";
    private static final String ACTIVEX_CONTROL = "http://schemas.microsoft.com/office/2006/relationships/activeXControl";
    private static final String ACTIVEX_CONTROL_BIN = "http://schemas.microsoft.com/office/2006/relationships/activeXControlBinary";
}