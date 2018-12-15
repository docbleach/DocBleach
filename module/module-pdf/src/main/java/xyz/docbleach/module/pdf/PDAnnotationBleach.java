package xyz.docbleach.module.pdf;

import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PDAnnotationBleach {

  private static final Logger LOGGER = LoggerFactory.getLogger(PDAnnotationBleach.class);
  private final PdfBleachSession pdfBleachSession;

  PDAnnotationBleach(PdfBleachSession pdfBleachSession) {
    this.pdfBleachSession = pdfBleachSession;
  }

  void sanitizeLinkAnnotation(PDAnnotationLink annotationLink) {
    if (annotationLink.getAction() == null) {
      return;
    }
    LOGGER.debug("Found&removed annotation link - action, was {}", annotationLink.getAction());
    pdfBleachSession.recordJavascriptThreat("Annotation", "External link");
    annotationLink.setAction(null);
  }

  void sanitizeWidgetAnnotation(PDAnnotationWidget annotationWidget) {
    if (annotationWidget.getAction() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget, was {}", annotationWidget.getAction());
      pdfBleachSession.recordJavascriptThreat("Annotation", "External widget");
      annotationWidget.setAction(null);
    }
    sanitizeAnnotationActions(annotationWidget.getActions());
  }

  void sanitizeAnnotationActions(PDAnnotationAdditionalActions annotationAdditionalActions) {
    if (annotationAdditionalActions == null) {
      return;
    }

    if (annotationAdditionalActions.getBl() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the annotation loses the input focus, was {}",
          annotationAdditionalActions.getBl());
      pdfBleachSession
          .recordJavascriptThreat("Annotation", "Action when annotation loses the input focus");
      annotationAdditionalActions.setBl(null);
    }
    if (annotationAdditionalActions.getD() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the mouse button is pressed inside the annotation's active area, was {}",
          annotationAdditionalActions.getD());
      annotationAdditionalActions.setD(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation",
          "Action when mouse button is pressed inside the annotation's active area");
    }
    if (annotationAdditionalActions.getE() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the cursor enters the annotation's active area, was {}",
          annotationAdditionalActions.getE());
      annotationAdditionalActions.setE(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation", "Action when the cursor enters the annotation's active area");
    }
    if (annotationAdditionalActions.getFo() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the annotation receives the input focus, was {}",
          annotationAdditionalActions.getFo());
      annotationAdditionalActions.setFo(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation", "Action when the annotation receives the input focus");
    }
    if (annotationAdditionalActions.getPC() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the page containing the annotation is closed, was {}",
          annotationAdditionalActions.getPC());
      annotationAdditionalActions.setPC(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation", "Action when the page containing the annotation is closed");
    }
    if (annotationAdditionalActions.getPI() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the page containing the annotation is no longer visible in the viewer application's user interface, was {}",
          annotationAdditionalActions.getPI());
      annotationAdditionalActions.setPI(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation",
          "Action when the page containing the annotation is no longer visible");
    }
    if (annotationAdditionalActions.getPO() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the page containing the annotation is opened, was {}",
          annotationAdditionalActions.getPO());
      annotationAdditionalActions.setPO(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation", "Action when the page containing the annotation is opened");
    }
    if (annotationAdditionalActions.getPV() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the page containing the annotation becomes visible in the viewer application's user interface, was {}",
          annotationAdditionalActions.getPV());
      annotationAdditionalActions.setPV(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation", "Action the page containing the annotation becomes visible");
    }
    if (annotationAdditionalActions.getU() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the mouse button is released inside the annotation's active area, was {}",
          annotationAdditionalActions.getU());
      annotationAdditionalActions.setU(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation",
          "Action when the mouse button is released inside the annotation's active area");
    }
    if (annotationAdditionalActions.getX() != null) {
      LOGGER.debug(
          "Found&Removed action on annotation widget to be performed when the cursor exits the annotation's active area, was {}",
          annotationAdditionalActions.getX());
      annotationAdditionalActions.setX(null);
      pdfBleachSession.recordJavascriptThreat(
          "Annotation", "Action when the cursor exits the annotation's active area");
    }
  }

  void sanitizeAnnotation(PDAnnotation annotation) {
    if (annotation instanceof PDAnnotationLink) {
      sanitizeLinkAnnotation((PDAnnotationLink) annotation);
    }

    if (annotation instanceof PDAnnotationWidget) {
      sanitizeWidgetAnnotation((PDAnnotationWidget) annotation);
    }
  }
}
