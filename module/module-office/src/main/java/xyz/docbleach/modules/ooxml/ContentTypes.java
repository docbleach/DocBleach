package xyz.docbleach.modules.ooxml;

@SuppressWarnings("ALL")
public final class ContentTypes {
    // Main Parts (PowerPoint)
    public final static String MAIN_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml";
    public final static String MAIN_PPTM = "application/vnd.ms-powerpoint.presentation.macroEnabled.main+xml";
    public final static String MAIN_POTX = "application/vnd.openxmlformats-officedocument.presentationml.template.main+xml";
    public final static String MAIN_POTM = "application/vnd.ms-powerpoint.template.macroEnabled.main+xml";
    public final static String MAIN_PPSX = "application/vnd.openxmlformats-officedocument.presentationml.slideshow.main+xml";
    public final static String MAIN_PPSM = "application/vnd.ms-powerpoint.slideshow.macroEnabled.main+xml";
    public final static String MAIN_PPAM = "application/vnd.ms-powerpoint.addin.macroEnabled.main+xml";

    // Main Parts (Word)
    public final static String MAIN_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    public final static String MAIN_DOCM = "application/vnd.ms-word.document.macroEnabled.main+xml";
    public final static String MAIN_DOTX = "application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml";
    public final static String MAIN_DOTM = "application/vnd.ms-word.template.macroEnabledTemplate.main+xml";

    // Main Parts (Excel)
    public final static String MAIN_XLSB = "application/vnd.ms-excel.sheet.binary.macroEnabled.main";
    public final static String MAIN_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    public final static String MAIN_XLSM = "application/vnd.ms-excel.sheet.macroEnabled.main+xml";
    public final static String MAIN_XLTX = "application/vnd.openxmlformats-officedocument.spreadsheetml.template.main+xml";
    public final static String MAIN_XLTM = "application/vnd.ms-excel.template.macroEnabled.main+xml";
    public final static String MAIN_XLAM = "application/vnd.ms-excel.addin.macroEnabled.main+xml";

    // VBA related
    public final static String VBA_DATA = "application/vnd.ms-word.vbaData+xml";
    public final static String VBA_PROJECT = "application/vnd.ms-office.vbaProject";
    public final static String VBA_PROJECT_SIGNATURE = "application/vnd.ms-office.vbaProjectSignature";

    // PostScript
    public final static String POSTSCRIPT = "application/postscript";

    // OLE Objects
    public final static String OLE_OBJECT = "application/vnd.openxmlformats-officedocument.oleObject";
    public final static String PACKAGE = "application/vnd.openxmlformats-officedocument.package";

    // ActiveX Controls
    public final static String ACTIVEX = "application/vnd.ms-office.activeX";
    public final static String OPENXML_ACTIVEX = "application/vnd.openxmlformats-officedocument.activeX";
    public final static String OPENXML_ACTIVEX_XML = "application/vnd.openxmlformats-officedocument.activeX+xml";
}