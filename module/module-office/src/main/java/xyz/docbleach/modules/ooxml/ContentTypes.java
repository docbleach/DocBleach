package xyz.docbleach.modules.ooxml;

@SuppressWarnings("ALL")
public final class ContentTypes {
    // Main Parts (PowerPoint)
    private static final String MAIN_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml";
    private static final String MAIN_PPTM = "application/vnd.ms-powerpoint.presentation.macroEnabled.main+xml";
    private static final String MAIN_POTX = "application/vnd.openxmlformats-officedocument.presentationml.template.main+xml";
    private static final String MAIN_POTM = "application/vnd.ms-powerpoint.template.macroEnabled.main+xml";
    private static final String MAIN_PPSX = "application/vnd.openxmlformats-officedocument.presentationml.slideshow.main+xml";
    private static final String MAIN_PPSM = "application/vnd.ms-powerpoint.slideshow.macroEnabled.main+xml";
    private static final String MAIN_PPAM = "application/vnd.ms-powerpoint.addin.macroEnabled.main+xml";

    // Main Parts (Word)
    private static final String MAIN_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    private static final String MAIN_DOCM = "application/vnd.ms-word.document.macroEnabled.main+xml";
    private static final String MAIN_DOTX = "application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml";
    private static final String MAIN_DOTM = "application/vnd.ms-word.template.macroEnabledTemplate.main+xml";

    // Main Parts (Excel)
    private static final String MAIN_XLSB = "application/vnd.ms-excel.sheet.binary.macroEnabled.main";
    private static final String MAIN_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    private static final String MAIN_XLSM = "application/vnd.ms-excel.sheet.macroEnabled.main+xml";
    private static final String MAIN_XLTX = "application/vnd.openxmlformats-officedocument.spreadsheetml.template.main+xml";
    private static final String MAIN_XLTM = "application/vnd.ms-excel.template.macroEnabled.main+xml";
    private static final String MAIN_XLAM = "application/vnd.ms-excel.addin.macroEnabled.main+xml";

    // VBA related
    private static final String VBA_DATA = "application/vnd.ms-word.vbaData+xml";
    private static final String VBA_PROJECT = "application/vnd.ms-office.vbaProject";
    private static final String VBA_PROJECT_SIGNATURE = "application/vnd.ms-office.vbaProjectSignature";

    // PostScript
    private static final String POSTSCRIPT = "application/postscript";

    // OLE Objects
    private static final String OLE_OBJECT = "application/vnd.openxmlformats-officedocument.oleObject";
    private static final String PACKAGE = "application/vnd.openxmlformats-officedocument.package";

    // ActiveX Controls
    private static final String ACTIVEX = "application/vnd.ms-office.activeX";
    private static final String OPENXML_ACTIVEX = "application/vnd.openxmlformats-officedocument.activeX";
    private static final String OPENXML_ACTIVEX_XML = "application/vnd.openxmlformats-officedocument.activeX+xml";
}