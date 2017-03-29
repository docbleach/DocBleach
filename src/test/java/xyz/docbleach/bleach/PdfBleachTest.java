package xyz.docbleach.bleach;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDDocumentCatalogAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.BleachSession;

class PdfBleachTest extends BleachTestBase {

  private PdfBleach instance;
  private BleachSession session;

  @BeforeEach
  void setUp() {
    instance = new PdfBleach();
    session = mock(BleachSession.class);
  }

  @Test
  void handlesMagic() throws IOException {
    Charset charset = Charset.defaultCharset();
    InputStream validInputStream = new ByteArrayInputStream("%PDF1.5".getBytes(charset));
    assertTrue(instance.handlesMagic(validInputStream));

    // Check that empty (shorter than %PDF1.-length) does not trigger an error
    InputStream invalidInputStream = new ByteArrayInputStream("".getBytes(charset));
    assertFalse(instance.handlesMagic(invalidInputStream));

    // Check that this bleach is sane
    invalidInputStream = new ByteArrayInputStream("Anything".getBytes(charset));
    assertFalse(instance.handlesMagic(invalidInputStream));
  }

  @Test
  void sanitizeAcroFormActions() {
    PDFormFieldAdditionalActions fieldActions = new PDFormFieldAdditionalActions();
    fieldActions.setC(new PDActionJavaScript());
    fieldActions.setF(new PDActionJavaScript());
    fieldActions.setK(new PDActionJavaScript());
    fieldActions.setV(new PDActionJavaScript());

    instance.sanitizeFieldAdditionalActions(session, fieldActions);
    assertThreatsFound(session, 4);

    assertNull(fieldActions.getC());
    assertNull(fieldActions.getF());
    assertNull(fieldActions.getK());
    assertNull(fieldActions.getV());

    reset(session);

    // Simple check to make sure this method is idempotent
    instance.sanitizeFieldAdditionalActions(session, fieldActions);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizePageActions() {
    PDPageAdditionalActions actions = new PDPageAdditionalActions();
    actions.setC(new PDActionJavaScript());
    actions.setO(new PDActionJavaScript());

    instance.sanitizePageActions(session, actions);
    assertThreatsFound(session, 2);

    assertNull(actions.getC());
    assertNull(actions.getO());

    reset(session);

    instance.sanitizePageActions(session, actions);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAdditionalActions() {
    PDDocumentCatalogAdditionalActions documentCatalogAdditionalActions = new PDDocumentCatalogAdditionalActions();
    instance.sanitizeDocumentActions(session, documentCatalogAdditionalActions);
    documentCatalogAdditionalActions.setDP(new PDActionJavaScript());
    documentCatalogAdditionalActions.setDS(new PDActionJavaScript());
    documentCatalogAdditionalActions.setWC(new PDActionJavaScript());
    documentCatalogAdditionalActions.setWP(new PDActionJavaScript());
    documentCatalogAdditionalActions.setWS(new PDActionJavaScript());

    instance.sanitizeDocumentActions(session, documentCatalogAdditionalActions);
    assertThreatsFound(session, 5);
    reset(session);

    assertNull(documentCatalogAdditionalActions.getDP());
    assertNull(documentCatalogAdditionalActions.getDS());
    assertNull(documentCatalogAdditionalActions.getWC());
    assertNull(documentCatalogAdditionalActions.getWP());
    assertNull(documentCatalogAdditionalActions.getWS());

    instance.sanitizeDocumentActions(session, documentCatalogAdditionalActions);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeOpenAction() throws IOException {
    PDDocumentCatalog documentCatalog = mock(PDDocumentCatalog.class);

    when(documentCatalog.getOpenAction()).thenReturn(new PDActionJavaScript());
    instance.sanitizeOpenAction(session, documentCatalog);

    verify(documentCatalog, atLeastOnce()).getOpenAction();
    verify(documentCatalog, atLeastOnce()).setOpenAction(null);
    assertThreatsFound(session, 1);

    reset(session);
    reset(documentCatalog);

    when(documentCatalog.getOpenAction()).thenReturn(null);
    instance.sanitizeOpenAction(session, documentCatalog);
    verify(documentCatalog, atLeastOnce()).getOpenAction();
    verify(documentCatalog, never()).setOpenAction(null);

    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAnnotationLink() {
    PDAnnotationLink annotationLink = new PDAnnotationLink();
    annotationLink.setAction(new PDActionJavaScript());
    instance.sanitizeLinkAnnotation(session, annotationLink);

    assertThreatsFound(session, 1);
    assertNull(annotationLink.getAction());

    reset(session);

    instance.sanitizeLinkAnnotation(session, annotationLink);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAnnotationWidgetAction() {
    PDAnnotationWidget annotationWidget = new PDAnnotationWidget();
    annotationWidget.setAction(new PDActionJavaScript());

    instance.sanitizeWidgetAnnotation(session, annotationWidget);

    assertThreatsFound(session, 1);
    assertNull(annotationWidget.getAction());
    reset(session);

    instance.sanitizeWidgetAnnotation(session, annotationWidget);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAnnotationWidgetActions() {
    PDAnnotationAdditionalActions annotationAdditionalActions = new PDAnnotationAdditionalActions();
    annotationAdditionalActions.setBl(new PDActionJavaScript());
    annotationAdditionalActions.setD(new PDActionJavaScript());
    annotationAdditionalActions.setE(new PDActionJavaScript());
    annotationAdditionalActions.setFo(new PDActionJavaScript());
    annotationAdditionalActions.setPC(new PDActionJavaScript());
    annotationAdditionalActions.setPI(new PDActionJavaScript());
    annotationAdditionalActions.setPO(new PDActionJavaScript());
    annotationAdditionalActions.setU(new PDActionJavaScript());
    annotationAdditionalActions.setX(new PDActionJavaScript());

    instance.sanitizeAnnotationActions(session, annotationAdditionalActions);
    assertNull(annotationAdditionalActions.getBl());
    assertNull(annotationAdditionalActions.getD());
    assertNull(annotationAdditionalActions.getE());
    assertNull(annotationAdditionalActions.getFo());
    assertNull(annotationAdditionalActions.getPC());
    assertNull(annotationAdditionalActions.getPI());
    assertNull(annotationAdditionalActions.getPO());
    assertNull(annotationAdditionalActions.getU());
    assertNull(annotationAdditionalActions.getX());

    assertThreatsFound(session, 9);
    reset(session);

    instance.sanitizeAnnotationActions(session, annotationAdditionalActions);
    assertThreatsFound(session, 0);
  }
}