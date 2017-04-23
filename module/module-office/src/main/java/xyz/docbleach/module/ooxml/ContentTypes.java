package xyz.docbleach.module.ooxml;

@SuppressWarnings("ALL")
public final class ContentTypes {
    // Main Parts (PowerPoint)
    public static final String MAIN_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml";
    public static final String MAIN_PPTM = "application/vnd.ms-powerpoint.presentation.macroEnabled.main+xml";
    public static final String MAIN_POTX = "application/vnd.openxmlformats-officedocument.presentationml.template.main+xml";
    public static final String MAIN_POTM = "application/vnd.ms-powerpoint.template.macroEnabled.main+xml";
    public static final String MAIN_PPSX = "application/vnd.openxmlformats-officedocument.presentationml.slideshow.main+xml";
    public static final String MAIN_PPSM = "application/vnd.ms-powerpoint.slideshow.macroEnabled.main+xml";
    public static final String MAIN_PPAM = "application/vnd.ms-powerpoint.addin.macroEnabled.main+xml";
    // Main Parts (Word)
    public static final String MAIN_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    public static final String MAIN_DOCM = "application/vnd.ms-word.document.macroEnabled.main+xml";
    public static final String MAIN_DOTX = "application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml";
    public static final String MAIN_DOTM = "application/vnd.ms-word.template.macroEnabledTemplate.main+xml";
    // Main Parts (Excel)
    public static final String MAIN_XLSB = "application/vnd.ms-excel.sheet.binary.macroEnabled.main";
    public static final String MAIN_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    public static final String MAIN_XLSM = "application/vnd.ms-excel.sheet.macroEnabled.main+xml";
    public static final String MAIN_XLTX = "application/vnd.openxmlformats-officedocument.spreadsheetml.template.main+xml";
    public static final String MAIN_XLTM = "application/vnd.ms-excel.template.macroEnabled.main+xml";
    public static final String MAIN_XLAM = "application/vnd.ms-excel.addin.macroEnabled.main+xml";
    // VBA related
    public static final String VBA_DATA = "application/vnd.ms-word.vbaData+xml";
    public static final String VBA_PROJECT = "application/vnd.ms-office.vbaProject";
    public static final String VBA_PROJECT_SIGNATURE = "application/vnd.ms-office.vbaProjectSignature";
    // PostScript
    public static final String POSTSCRIPT = "application/postscript";
    // OLE Objects
    public static final String OLE_OBJECT = "application/vnd.openxmlformats-officedocument.oleObject";
    public static final String PACKAGE = "application/vnd.openxmlformats-officedocument.package";
    // ActiveX Controls
    public static final String ACTIVEX = "application/vnd.ms-office.activeX";
    public static final String OPENXML_ACTIVEX = "application/vnd.openxmlformats-officedocument.activeX";
    public static final String OPENXML_ACTIVEX_XML = "application/vnd.openxmlformats-officedocument.activeX+xml";

    private ContentTypes() {
        throw new IllegalAccessError("Utility class");
    }
}