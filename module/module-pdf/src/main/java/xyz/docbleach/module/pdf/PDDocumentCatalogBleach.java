package xyz.docbleach.module.pdf;

import java.io.IOException;
import java.util.Iterator;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDDocumentCatalogAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PDDocumentCatalogBleach {

  private static final Logger LOGGER = LoggerFactory.getLogger(COSObjectBleach.class);
  private final PdfBleachSession pdfBleachSession;
  private final PDAnnotationBleach annotationBleach;

  PDDocumentCatalogBleach(PdfBleachSession pdfBleachSession) {
    this.pdfBleachSession = pdfBleachSession;
    this.annotationBleach = new PDAnnotationBleach(pdfBleachSession);
  }


  private void sanitizeAcroFormActions(PDAcroForm acroForm) {
    if (acroForm == null) {
      LOGGER.debug("No AcroForms found");
      return;
    }
    LOGGER.trace("Checking AcroForm Actions");

    Iterator<PDField> fields = acroForm.getFieldIterator();

    fields.forEachRemaining(this::sanitizeField);
  }

  private void sanitizeField(PDField field) {
    // Sanitize annotations
    field.getWidgets().forEach(annotationBleach::sanitizeAnnotation);

    // Sanitize field actions
    PDFormFieldAdditionalActions fieldActions = field.getActions();
    if (fieldActions == null) {
      return;
    }
    sanitizeFieldAdditionalActions(fieldActions);
  }

  private void sanitizePage(PDPage page) throws IOException {
    for (PDAnnotation annotation : page.getAnnotations()) {
      annotationBleach.sanitizeAnnotation(annotation);
      sanitizePageActions(page.getActions());
    }
  }

  private void sanitizePageActions(PDPageTree pages) throws IOException {
    LOGGER.trace("Checking Pages Actions");
    for (PDPage page : pages) {
      sanitizePage(page);
    }
  }

  void sanitizePageActions(PDPageAdditionalActions pageActions) {
    if (pageActions.getC() != null) {
      LOGGER.debug("Found&removed action when page is closed, was ({})", pageActions.getC());
      pageActions.setC(null);
      pdfBleachSession.recordJavascriptThreat("Page Actions", "Action when page is closed");
    }

    if (pageActions.getO() != null) {
      LOGGER.debug("Found&removed action when page is opened, was ({})", pageActions.getO());
      pageActions.setO(null);
      pdfBleachSession.recordJavascriptThreat("Page Actions", "Action when page is opened");
    }
  }

  void sanitizeOpenAction(PDDocumentCatalog docCatalog)
      throws IOException {
    LOGGER.trace("Checking OpenAction...");
    PDDestinationOrAction openAction = docCatalog.getOpenAction();

    if (openAction == null) {
      return;
    }

    LOGGER.debug("Found a JavaScript OpenAction, removed. Was {}", openAction);
    docCatalog.setOpenAction(null);
    pdfBleachSession.recordJavascriptThreat("Document Catalog", "OpenAction");
  }

  void sanitizeDocumentActions(PDDocumentCatalogAdditionalActions documentActions) {
    LOGGER.trace("Checking additional actions...");
    if (documentActions.getDP() != null) {
      LOGGER.debug("Found&removed action after printing (was {})", documentActions.getDP());
      documentActions.setDP(null);
      pdfBleachSession
          .recordJavascriptThreat("DocumentCatalogAdditionalActions", "Action after printing");
    }
    if (documentActions.getDS() != null) {
      LOGGER.debug("Found&removed action after saving (was {})", documentActions.getDS());
      documentActions.setDS(null);
      pdfBleachSession
          .recordJavascriptThreat("DocumentCatalogAdditionalActions", "Action after saving");
    }
    if (documentActions.getWC() != null) {
      LOGGER.debug("Found&removed action before closing (was {}", documentActions.getWC());
      documentActions.setWC(null);
      pdfBleachSession
          .recordJavascriptThreat("DocumentCatalogAdditionalActions", "Action before closing");
    }
    if (documentActions.getWP() != null) {
      LOGGER.debug("Found&removed action before printing (was {})", documentActions.getWP());
      documentActions.setWP(null);
      pdfBleachSession
          .recordJavascriptThreat("DocumentCatalogAdditionalActions", "Action before printing");
    }
    if (documentActions.getWS() != null) {
      LOGGER.debug("Found&removed action before saving (was {})", documentActions.getWS());
      documentActions.setWS(null);
      pdfBleachSession
          .recordJavascriptThreat("DocumentCatalogAdditionalActions", "Action before saving");
    }
  }

  void sanitizeFieldAdditionalActions(PDFormFieldAdditionalActions fieldActions) {
    if (fieldActions.getC() != null) {
      LOGGER.debug(
          "Found&removed an action to be performed in order to recalculate the value of this field when that of another field changes.");
      fieldActions.setC(null);
      pdfBleachSession.recordJavascriptThreat("FormAdditionalActions", "Action on value change");
    }
    if (fieldActions.getF() != null) {
      LOGGER.debug(
          "Found&removed an action to be performed before the field is formatted to display its current value.");
      fieldActions.setF(null);
      pdfBleachSession
          .recordJavascriptThreat("FormAdditionalActions", "Action to format the value");
    }
    if (fieldActions.getK() != null) {
      LOGGER.debug(
          "Found&removed an action to be performed when the user types a keystroke into a text field or combo box or modifies the selection in a scrollable list box.");
      fieldActions.setK(null);
      pdfBleachSession
          .recordJavascriptThreat("FormAdditionalActions", "Action when the user types a keystoke");
    }
    if (fieldActions.getV() != null) {
      LOGGER.debug(
          "Found&removed an action to be action to be performed when the field's value is changed.");
      fieldActions.setV(null);
      pdfBleachSession.recordJavascriptThreat("FormAdditionalActions",
          "Action when the field's value is changed");
    }
  }

  void sanitize(PDDocumentCatalog docCatalog) throws IOException {
    sanitizeOpenAction(docCatalog);
    sanitizeDocumentActions(docCatalog.getActions());
    sanitizePageActions(docCatalog.getPages());
    sanitizeAcroFormActions(docCatalog.getAcroForm());
  }
}
