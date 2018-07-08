package xyz.docbleach.module.ooxml;

@SuppressWarnings("ALL")
public final class Relations {

  public static final String HYPERLINK =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink";
  // Macro
  public static final String VBA_PROJECT =
      "http://schemas.microsoft.com/office/2006/relationships/vbaProject";
  public static final String VBA_PROJECT_SIGNATURE =
      "http://schemas.microsoft.com/office/2006/relationships/vbaProjectSignature";
  public static final String WORD_VBA_DATA =
      "http://schemas.microsoft.com/office/2006/relationships/wordVbaData";
  // OLE Objects
  public static final String OPENXML_OLE_OBJECT =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/oleObject";
  public static final String OLE_OBJECT =
      "http://schemas.microsoft.com/office/2006/relationships/oleObject";
  public static final String E2_OBJECT =
      "http://schemas.microsoft.com/office/2006/relationships/e2Object";
  public static final String E1_OBJECT =
      "http://schemas.microsoft.com/office/2006/relationships/e1Object";
  // ActiveX Controls
  public static final String OPENXML_CONTROL =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/control";
  public static final String OPENXML_ACTIVEX_CONTROL =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/activeXControl";
  public static final String OPENXML_ACTIVEX_CONTROL_BIN =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/activeXControlBinary";
  public static final String ACTIVEX_CONTROL =
      "http://schemas.microsoft.com/office/2006/relationships/activeXControl";
  public static final String ACTIVEX_CONTROL_BIN =
      "http://schemas.microsoft.com/office/2006/relationships/activeXControlBinary";

  private Relations() {
    throw new IllegalAccessError("Utility class");
  }
}
