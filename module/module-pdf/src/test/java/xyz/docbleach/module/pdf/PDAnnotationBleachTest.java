package xyz.docbleach.module.pdf;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static xyz.docbleach.api.BleachTestBase.assertThreatsFound;

import java.lang.reflect.Method;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.docbleach.api.BleachSession;

class PDAnnotationBleachTest {

  private BleachSession session;
  private PDAnnotationBleach instance;

  @BeforeEach
  void setUp() {
    session = mock(BleachSession.class);
    instance = new PDAnnotationBleach(new PdfBleachSession(session));
  }


  @Test
  void sanitizeAnnotationLink() {
    PDAnnotationLink annotationLink = new PDAnnotationLink();
    annotationLink.setAction(new PDActionJavaScript());
    instance.sanitizeLinkAnnotation(annotationLink);

    assertThreatsFound(session, 1);
    assertNull(annotationLink.getAction());

    reset(session);

    instance.sanitizeLinkAnnotation(annotationLink);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAnnotationWidgetAction() {
    PDAnnotationWidget annotationWidget = new PDAnnotationWidget();
    annotationWidget.setAction(new PDActionJavaScript());

    instance.sanitizeWidgetAnnotation(annotationWidget);

    assertThreatsFound(session, 1);
    assertNull(annotationWidget.getAction());
    reset(session);

    instance.sanitizeWidgetAnnotation(annotationWidget);
    assertThreatsFound(session, 0);
  }

  @Test
  void sanitizeAnnotationWidgetActions() throws ReflectiveOperationException {
    final String[] methods = {"Bl", "D", "E", "Fo", "PC", "PI", "PO", "U", "X"};
    final PDAction action = new PDActionJavaScript();
    final Class<PDAnnotationAdditionalActions> clazz = PDAnnotationAdditionalActions.class;

    for (String name : methods) {
      PDAnnotationAdditionalActions annotationAdditionalActions = new PDAnnotationAdditionalActions();

      Method setMethod = clazz.getMethod("set" + name, PDAction.class);
      Method getMethod = clazz.getMethod("get" + name);

      setMethod.invoke(annotationAdditionalActions, action);
      assertNotNull(getMethod.invoke(annotationAdditionalActions),
          "get" + name + " returned null after being set");

      instance.sanitizeAnnotationActions(annotationAdditionalActions);

      assertNull(getMethod.invoke(annotationAdditionalActions)); // make sure it was removed
      assertThreatsFound(session, 1);
      reset(session);
    }
  }
}