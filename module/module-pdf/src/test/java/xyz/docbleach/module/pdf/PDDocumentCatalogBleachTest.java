package xyz.docbleach.module.pdf;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static xyz.docbleach.api.BleachTestBase.assertThreatsFound;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDDocumentCatalogAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachSession;

class PDDocumentCatalogBleachTest {

  private BleachSession session;
  private PDDocumentCatalogBleach instance;

  @BeforeEach
  void setUp() {
    session = mock(BleachSession.class);
    instance = new PDDocumentCatalogBleach(new PdfBleachSession(session));
  }


  @Test
  void sanitizeAcroFormActions() {
    PDFormFieldAdditionalActions fieldActions = new PDFormFieldAdditionalActions();
    fieldActions.setC(new PDActionJavaScript());
    fieldActions.setF(new PDActionJavaScript());
    fieldActions.setK(new PDActionJavaScript());
    fieldActions.setV(new PDActionJavaScript());

    instance.sanitizeFieldAdditionalActions(fieldActions);
    assertThreatsFound(session, 4);

    assertNull(fieldActions.getC());
    assertNull(fieldActions.getF());
    assertNull(fieldActions.getK());
    assertNull(fieldActions.getV());

    reset(session);

    // Simple check to make sure this method is idempotent
    instance.sanitizeFieldAdditionalActions(fieldActions);
    assertThreatsFound(session, 0);
  }


  @Test
  void sanitizePageActions() {
    PDPageAdditionalActions actions = new PDPageAdditionalActions();
    actions.setC(new PDActionJavaScript());
    actions.setO(new PDActionJavaScript());

    instance.sanitizePageActions(actions);
    assertThreatsFound(session, 2);

    assertNull(actions.getC());
    assertNull(actions.getO());

    reset(session);

    instance.sanitizePageActions(actions);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAdditionalActions() {
    PDDocumentCatalogAdditionalActions documentCatalogAdditionalActions =
        new PDDocumentCatalogAdditionalActions();
    instance.sanitizeDocumentActions(documentCatalogAdditionalActions);
    documentCatalogAdditionalActions.setDP(new PDActionJavaScript());
    documentCatalogAdditionalActions.setDS(new PDActionJavaScript());
    documentCatalogAdditionalActions.setWC(new PDActionJavaScript());
    documentCatalogAdditionalActions.setWP(new PDActionJavaScript());
    documentCatalogAdditionalActions.setWS(new PDActionJavaScript());

    instance.sanitizeDocumentActions(documentCatalogAdditionalActions);
    assertThreatsFound(session, 5);
    reset(session);

    assertNull(documentCatalogAdditionalActions.getDP());
    assertNull(documentCatalogAdditionalActions.getDS());
    assertNull(documentCatalogAdditionalActions.getWC());
    assertNull(documentCatalogAdditionalActions.getWP());
    assertNull(documentCatalogAdditionalActions.getWS());

    instance.sanitizeDocumentActions(documentCatalogAdditionalActions);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeOpenAction() throws IOException {
    PDDocumentCatalog documentCatalog = mock(PDDocumentCatalog.class);

    when(documentCatalog.getOpenAction()).thenReturn(new PDActionJavaScript());
    instance.sanitizeOpenAction(documentCatalog);

    verify(documentCatalog, atLeastOnce()).getOpenAction();
    verify(documentCatalog, atLeastOnce()).setOpenAction(null);
    assertThreatsFound(session, 1);

    reset(session);
    reset(documentCatalog);

    when(documentCatalog.getOpenAction()).thenReturn(null);
    instance.sanitizeOpenAction(documentCatalog);
    verify(documentCatalog, atLeastOnce()).getOpenAction();
    verify(documentCatalog, never()).setOpenAction(null);

    assertThreatsFound(session, 0);
  }
}