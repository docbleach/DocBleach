package xyz.docbleach.module.pdf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.action.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.Bleach;
import xyz.docbleach.api.exception.BleachException;
import xyz.docbleach.api.threat.Threat;
import xyz.docbleach.api.threat.ThreatAction;
import xyz.docbleach.api.threat.ThreatSeverity;
import xyz.docbleach.api.threat.ThreatType;
import xyz.docbleach.api.util.StreamUtils;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static xyz.docbleach.api.threat.ThreatBuilder.threat;

/**
 * PDF parsing is a bit tricky: everything may or may not be linked to additional actions, so we
 * need to treat each and every elements.
 */
public class PdfBleach implements Bleach {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfBleach.class);
    private static final byte[] PDF_MAGIC = new byte[]{37, 80, 68, 70};
    private static final String[] COMMON_PASSWORDS = new String[]{
            null, "", "test", "example", "sample", "malware", "infected", "password"
    };
    private static final MemoryUsageSetting MEMORY_USAGE_SETTING = MemoryUsageSetting.setupMixed(1024 * 100);

    @Override
    public boolean handlesMagic(InputStream stream) {
        return StreamUtils.hasHeader(stream, PDF_MAGIC);
    }

    @Override
    public String getName() {
        return "PDF Bleach";
    }

    @Override
    public void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session) throws BleachException {
        try (ScratchFile scratchFile = new ScratchFile(MEMORY_USAGE_SETTING);
             RandomAccessRead source = new RandomAccessBufferedFileInputStream(inputStream)
        ) {
            sanitize(scratchFile, source, outputStream, session);
        } catch (IOException e) {
            throw new BleachException(e);
        }
    }

    private void sanitize(ScratchFile scratchFile, RandomAccessRead source, OutputStream outputStream, BleachSession session) throws IOException, BleachException {
        final PDDocument doc = getDocument(scratchFile, source);

        final PDDocumentCatalog docCatalog = doc.getDocumentCatalog();

        sanitizeNamed(session, doc, docCatalog.getNames());
        sanitizeOpenAction(session, docCatalog);
        sanitizeDocumentActions(session, docCatalog.getActions());
        sanitizePageActions(session, docCatalog.getPages());
        sanitizeAcroFormActions(session, docCatalog.getAcroForm());
        sanitizeDocumentOutline(session, doc.getDocumentCatalog().getDocumentOutline());

        sanitizeObjects(session, doc.getDocument().getObjects());

        doc.save(outputStream);
        doc.close();
    }

    private void sanitizeDocumentOutline(BleachSession session, PDDocumentOutline documentOutline) {
        if (documentOutline == null)
            return;
        if (!documentOutline.hasChildren())
            return;
        documentOutline.children().forEach(item -> sanitizeDocumentOutlineItem(session, item));
    }

    private void sanitizeDocumentOutlineItem(BleachSession session, PDOutlineItem item) {
        if (item.getAction() == null)
            return;
        LOGGER.debug("Found&removed action on outline item (was {})", item.getAction());
        item.setAction(null);
        recordJavascriptThreat(session, "DocumentOutline Item Action", "Action");
    }

    private void sanitizeNamed(BleachSession session, PDDocument doc, PDDocumentNameDictionary names) {
        if (names == null)
            return;

        sanitizeRecursiveNameTree(names.getEmbeddedFiles(), fileSpec -> sanitizeEmbeddedFile(session, doc, fileSpec));

        sanitizeRecursiveNameTree(names.getJavaScript(), action -> sanitizeJavascript(session, doc, action));
        names.setJavascript(null);
    }

    private void sanitizeJavascript(BleachSession session, PDDocument doc, PDActionJavaScript action) {
        LOGGER.debug("Found JS Action: {}", action.getAction());
        // @TODO: find samples and check what actions could be taken. For now, we remove the named tree.
    }

    private <T extends COSObjectable> void sanitizeRecursiveNameTree(PDNameTreeNode<T> efTree, Consumer<T> callback) {
        if (efTree == null)
            return;

        Map<String, T> _names;
        try {
            _names = efTree.getNames();
        } catch (IOException e) {
            LOGGER.error("Error in sanitizeRecursiveNameTree", e);
            return;
        }

        if (_names != null) {
            _names.values().forEach(callback);
        }
        if (efTree.getKids() == null)
            return;
        for (PDNameTreeNode<T> node : efTree.getKids()) {
            sanitizeRecursiveNameTree(node, callback);
        }
    }

    private void sanitizeEmbeddedFile(BleachSession session, PDDocument doc, PDComplexFileSpecification fileSpec) {
        LOGGER.trace("Embedded file found: {}", fileSpec.getFilename());

        Function<PDEmbeddedFile, PDEmbeddedFile> sanitize = file -> sanitizeEmbeddedFile(session, doc, file);

        fileSpec.setEmbeddedFile(sanitize.apply(fileSpec.getEmbeddedFile()));
        fileSpec.setEmbeddedFileDos(sanitize.apply(fileSpec.getEmbeddedFileDos()));
        fileSpec.setEmbeddedFileMac(sanitize.apply(fileSpec.getEmbeddedFileMac()));
        fileSpec.setEmbeddedFileUnicode(sanitize.apply(fileSpec.getEmbeddedFileUnicode()));
        fileSpec.setEmbeddedFileUnix(sanitize.apply(fileSpec.getEmbeddedFileUnix()));
    }

    private PDEmbeddedFile sanitizeEmbeddedFile(BleachSession session, PDDocument doc, PDEmbeddedFile file) {
        if (file == null)
            return null;

        LOGGER.debug("Sanitizing file: Size: {}, Mime-Type: {}, ", file.getSize(), file.getSubtype());

        ByteArrayInputStream is;
        try {
            is = new ByteArrayInputStream(file.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Error during original's file read", e);
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            session.sanitize(is, os);
        } catch (BleachException e) {
            LOGGER.error("Error during the bleach process", e);
            return null;
        }

        ByteArrayInputStream fakeFile = new ByteArrayInputStream(os.toByteArray());

        PDEmbeddedFile ef;
        try {
            ef = new PDEmbeddedFile(doc, fakeFile);
            ef.setCreationDate(file.getCreationDate());
            ef.setModDate(file.getModDate());
        } catch (IOException e) {
            LOGGER.error("Error when creating the new sane file", e);
            return null;
        }

        // We copy the properties of the real embedded file
        ef.setSubtype(file.getSubtype());
        ef.setSize(os.size());
        ef.setMacCreator(file.getMacCreator());
        ef.setMacResFork(file.getMacResFork());
        ef.setMacSubtype(file.getMacSubtype());


        // We remove the real file
        file.setSize(0);
        file.setFile(null);

        try {
            // And we empty it
            file.createOutputStream().close();
        } catch (IOException e) {
            LOGGER.error("Error when trying to empty the original embedded file", e);
            // Not severe, don't abort operations.
        }
        return ef;
    }

    private void rewind(RandomAccessRead source) throws IOException {
        source.rewind((int) source.getPosition());
    }

    private PDDocument getDocument(ScratchFile scratchFile, RandomAccessRead source) throws IOException, BleachException {
        PDDocument doc;
        for (String pwd : COMMON_PASSWORDS) {
            doc = testPassword(scratchFile, source, pwd);
            if (doc != null) {
                LOGGER.debug("Password was guessed: '{}'", pwd);
                doc.protect(new StandardProtectionPolicy(pwd, pwd, doc.getCurrentAccessPermission()));
                return doc;
            }
        }

        // @TODO: fetch password from config?

        throw new BleachException("PDF is protected with an unknown password");
    }

    @SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_RETURN_FALSE", justification = "This method is an helper to check the password")
    private PDDocument testPassword(ScratchFile inFile, RandomAccessRead source, String password) throws IOException {
        PDFParser parser = new PDFParser(source, password, inFile);
        try {
            parser.parse();
            return parser.getPDDocument();
        } catch (InvalidPasswordException e) {
            LOGGER.error("An exception occured while testing a password.", e);
            return null;
        } finally {
            rewind(source);
        }
    }

    public void sanitizeObjects(BleachSession session, List<COSObject> objects) {
        LOGGER.trace("Checking all objects..."); // Most destructive operation
        for (COSObject obj : objects) {
            crawl(session, obj.getObject());
        }
    }

    public void sanitizeAcroFormActions(BleachSession session, PDAcroForm acroForm) {
        LOGGER.trace("Checking AcroForm Actions");
        if (acroForm == null) {
            LOGGER.debug("No AcroForms found");
            return;
        }

        Iterator<PDField> fields = acroForm.getFieldIterator();

        fields.forEachRemaining(field -> {
            // Sanitize annotations
            field.getWidgets().forEach(annotation -> sanitizeAnnotation(session, annotation));

            // Sanitize field actions
            PDFormFieldAdditionalActions fieldActions = field.getActions();
            if (fieldActions == null) {
                return;
            }
            sanitizeFieldAdditionalActions(session, fieldActions);
        });
    }

    public void sanitizePageActions(BleachSession session, PDPageTree pages) throws IOException {
        LOGGER.trace("Checking Pages Actions");
        for (PDPage page : pages) {
            sanitizePage(session, page);
        }
    }

    public void sanitizePageActions(BleachSession session, PDPageAdditionalActions pageActions) {
        if (pageActions.getC() != null) {
            LOGGER.debug("Found&removed action when page is closed, was ({})", pageActions.getC());
            pageActions.setC(null);
            recordJavascriptThreat(session, "Page Actions", "Action when page is closed");
        }

        if (pageActions.getO() != null) {
            LOGGER.debug("Found&removed action when page is opened, was ({})", pageActions.getO());
            pageActions.setO(null);
            recordJavascriptThreat(session, "Page Actions", "Action when page is opened");
        }
    }

    public void sanitizeOpenAction(BleachSession session, PDDocumentCatalog docCatalog) throws IOException {
        LOGGER.trace("Checking OpenAction...");
        PDDestinationOrAction openAction = docCatalog.getOpenAction();

        if (openAction == null) {
            return;
        }

        LOGGER.debug("Found a JavaScript OpenAction, removed. Was {}", openAction);
        docCatalog.setOpenAction(null);
        recordJavascriptThreat(session, "Document Catalog", "OpenAction");
    }

    public void crawl(BleachSession session, COSBase base) {
        if (base == null) {
            return;
        }

        if (base instanceof COSName ||
                base instanceof COSString ||
                base instanceof COSStream ||
                base instanceof COSNull ||
                base instanceof COSObject ||
                base instanceof COSNumber ||
                base instanceof COSBoolean) {
            return;
        }

        if (base instanceof COSDictionary) {
            COSDictionary dict = (COSDictionary) base;
            Iterator<Map.Entry<COSName, COSBase>> it = dict.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<COSName, COSBase> entry = it.next();
                if ("JS".equals(entry.getKey().getName()) || "JavaScript".equals(entry.getKey().getName())) {
                    it.remove();
                    LOGGER.debug("Found and removed Javascript code");
                    recordJavascriptThreat(session, "?", "JS Code");
                    continue;
                }

                if ("S".equals(entry.getKey().getName())) {
                    if (entry.getValue() instanceof COSName) {
                        if ("JavaScript".equals(((COSName) entry.getValue()).getName())) {
                            LOGGER.debug("Found and removed Javascript code");
                            it.remove();
                            recordJavascriptThreat(session, "?", "JS Code");
                            continue;
                        }
                    }
                }

                if ("AA".equals(entry.getKey().getName())) {
                    LOGGER.debug("Found and removed Additionnal Actions");
                    it.remove();
                    recordJavascriptThreat(session, "?", "Additional Actions");
                    continue;
                }
                crawl(session, entry.getValue());
            }
        } else if (base instanceof COSArray) {
            COSArray ar = (COSArray) base;

            for (COSBase item : ar) {
                crawl(session, item);
            }
        } else {
            LOGGER.error("Unknown COS type: {}", base);
        }
    }

    public void sanitizeDocumentActions(BleachSession session, PDDocumentCatalogAdditionalActions documentActions) {
        LOGGER.trace("Checking additional actions...");
        if (documentActions.getDP() != null) {
            LOGGER.debug("Found&removed action after printing (was {})", documentActions.getDP());
            documentActions.setDP(null);
            recordJavascriptThreat(session, "DocumentCatalogAdditionalActions", "Action after printing");
        }
        if (documentActions.getDS() != null) {
            LOGGER.debug("Found&removed action after saving (was {})", documentActions.getDS());
            documentActions.setDS(null);
            recordJavascriptThreat(session, "DocumentCatalogAdditionalActions", "Action after saving");
        }
        if (documentActions.getWC() != null) {
            LOGGER.debug("Found&removed action before closing (was {}", documentActions.getWC());
            documentActions.setWC(null);
            recordJavascriptThreat(session, "DocumentCatalogAdditionalActions", "Action before closing");
        }
        if (documentActions.getWP() != null) {
            LOGGER.debug("Found&removed action before printing (was {})", documentActions.getWP());
            documentActions.setWP(null);
            recordJavascriptThreat(session, "DocumentCatalogAdditionalActions", "Action before printing");
        }
        if (documentActions.getWS() != null) {
            LOGGER.debug("Found&removed action before saving (was {})", documentActions.getWS());
            documentActions.setWS(null);
            recordJavascriptThreat(session, "DocumentCatalogAdditionalActions", "Action before saving");
        }
    }

    public void sanitizeFieldAdditionalActions(BleachSession session, PDFormFieldAdditionalActions fieldActions) {
        if (fieldActions.getC() != null) {
            LOGGER.debug("Found&removed an action to be performed in order to recalculate the value of this field when that of another field changes.");
            fieldActions.setC(null);
            recordJavascriptThreat(session, "FormAdditionalActions", "Action on value change");
        }
        if (fieldActions.getF() != null) {
            LOGGER.debug("Found&removed an action to be performed before the field is formatted to display its current value.");
            fieldActions.setF(null);
            recordJavascriptThreat(session, "FormAdditionalActions", "Action to format the value");
        }
        if (fieldActions.getK() != null) {
            LOGGER.debug("Found&removed an action to be performed when the user types a keystroke into a text field or combo box or modifies the selection in a scrollable list box.");
            fieldActions.setK(null);
            recordJavascriptThreat(session, "FormAdditionalActions", "Action when the user types a keystoke");
        }
        if (fieldActions.getV() != null) {
            LOGGER.debug("Found&removed an action to be action to be performed when the field's value is changed.");
            fieldActions.setV(null);
            recordJavascriptThreat(session, "FormAdditionalActions", "Action when the field's value is changed");
        }
    }

    public void sanitizePage(BleachSession session, PDPage page) throws IOException {
        for (PDAnnotation annotation : page.getAnnotations()) {
            sanitizeAnnotation(session, annotation);
            sanitizePageActions(session, page.getActions());
        }
    }

    public void sanitizeLinkAnnotation(BleachSession session, PDAnnotationLink annotationLink) {
        if (annotationLink.getAction() == null) {
            return;
        }
        LOGGER.debug("Found&removed annotation link - action, was {}", annotationLink.getAction());
        recordJavascriptThreat(session, "Annotation", "External link");
        annotationLink.setAction(null);
    }

    public void sanitizeWidgetAnnotation(BleachSession session, PDAnnotationWidget annotationWidget) {
        if (annotationWidget.getAction() != null) {
            LOGGER.debug("Found&Removed action on annotation widget, was {}", annotationWidget.getAction());
            recordJavascriptThreat(session, "Annotation", "External widget");
            annotationWidget.setAction(null);
        }
        sanitizeAnnotationActions(session, annotationWidget.getActions());
    }

    public void sanitizeAnnotationActions(BleachSession session, PDAnnotationAdditionalActions annotationAdditionalActions) {
        if (annotationAdditionalActions == null) {
            return;
        }

        if (annotationAdditionalActions.getBl() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the annotation loses the input focus, was {}", annotationAdditionalActions.getBl());
            recordJavascriptThreat(session, "Annotation", "Action when annotation loses the input focus");
            annotationAdditionalActions.setBl(null);
        }
        if (annotationAdditionalActions.getD() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the mouse button is pressed inside the annotation's active area, was {}", annotationAdditionalActions.getD());
            annotationAdditionalActions.setD(null);
            recordJavascriptThreat(session, "Annotation", "Action when mouse button is pressed inside the annotation's active area");
        }
        if (annotationAdditionalActions.getE() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the cursor enters the annotation's active area, was {}", annotationAdditionalActions.getE());
            annotationAdditionalActions.setE(null);
            recordJavascriptThreat(session, "Annotation", "Action when the cursor enters the annotation's active area");
        }
        if (annotationAdditionalActions.getFo() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the annotation receives the input focus, was {}", annotationAdditionalActions.getFo());
            annotationAdditionalActions.setFo(null);
            recordJavascriptThreat(session, "Annotation", "Action when the annotation receives the input focus");
        }
        if (annotationAdditionalActions.getPC() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the page containing the annotation is closed, was {}", annotationAdditionalActions.getPC());
            annotationAdditionalActions.setPC(null);
            recordJavascriptThreat(session, "Annotation", "Action when the page containing the annotation is closed");
        }
        if (annotationAdditionalActions.getPI() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the page containing the annotation is no longer visible in the viewer application's user interface, was {}", annotationAdditionalActions.getPI());
            annotationAdditionalActions.setPI(null);
            recordJavascriptThreat(session, "Annotation", "Action when the page containing the annotation is no longer visible");
        }
        if (annotationAdditionalActions.getPO() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the page containing the annotation is opened, was {}", annotationAdditionalActions.getPO());
            annotationAdditionalActions.setPO(null);
            recordJavascriptThreat(session, "Annotation", "Action when the page containing the annotation is opened");
        }
        if (annotationAdditionalActions.getPV() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the page containing the annotation becomes visible in the viewer application's user interface, was {}", annotationAdditionalActions.getPV());
            annotationAdditionalActions.setPV(null);
            recordJavascriptThreat(session, "Annotation", "Action the page containing the annotation becomes visible");
        }
        if (annotationAdditionalActions.getU() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the mouse button is released inside the annotation's active area, was {}", annotationAdditionalActions.getU());
            annotationAdditionalActions.setU(null);
            recordJavascriptThreat(session, "Annotation", "Action when the mouse button is released inside the annotation's active area");
        }
        if (annotationAdditionalActions.getX() != null) {
            LOGGER.debug("Found&Removed action on annotation widget to be performed when the cursor exits the annotation's active area, was {}", annotationAdditionalActions.getX());
            annotationAdditionalActions.setX(null);
            recordJavascriptThreat(session, "Annotation", "Action when the cursor exits the annotation's active area");
        }
    }

    public void sanitizeAnnotation(BleachSession session, PDAnnotation annotation) {
        if (annotation instanceof PDAnnotationLink) {
            sanitizeLinkAnnotation(session, (PDAnnotationLink) annotation);
        }

        if (annotation instanceof PDAnnotationWidget) {
            sanitizeWidgetAnnotation(session, (PDAnnotationWidget) annotation);
        }
    }

    private void recordJavascriptThreat(BleachSession session, String location, String details) {
        Threat threat = threat()
                .type(ThreatType.ACTIVE_CONTENT)
                .severity(ThreatSeverity.HIGH)
                .details(details)
                .location(location)
                .action(ThreatAction.REMOVE)
                .build();

        session.recordThreat(threat);
    }
}