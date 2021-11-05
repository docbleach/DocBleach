package xyz.docbleach.module.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.exception.BleachException;

class PDEmbeddedFileBleach {

  private static final Logger LOGGER = LoggerFactory.getLogger(COSObjectBleach.class);
  private final PdfBleachSession pdfBleachSession;
  private final PDDocument doc;

  PDEmbeddedFileBleach(PdfBleachSession pdfBleachSession, PDDocument doc) {
    this.pdfBleachSession = pdfBleachSession;
    this.doc = doc;
  }

  private void sanitizeEmbeddedFile(PDComplexFileSpecification fileSpec) {
    LOGGER.trace("Embedded file found: {}", fileSpec.getFilename());

    fileSpec.setEmbeddedFile(sanitizeEmbeddedFile(fileSpec.getEmbeddedFile()));
    //fileSpec.setEmbeddedFileDos(sanitizeEmbeddedFile(fileSpec.getEmbeddedFileDos()));
    //fileSpec.setEmbeddedFileMac(sanitizeEmbeddedFile(fileSpec.getEmbeddedFileMac()));
    fileSpec.setEmbeddedFileUnicode(sanitizeEmbeddedFile(fileSpec.getEmbeddedFileUnicode()));
    //fileSpec.setEmbeddedFileUnix(sanitizeEmbeddedFile(fileSpec.getEmbeddedFileUnix()));
  }

  private PDEmbeddedFile sanitizeEmbeddedFile(PDEmbeddedFile file) {
    if (file == null) {
      return null;
    }

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
      pdfBleachSession.getSession().sanitize(is, os);
    } catch (BleachException e) {
      LOGGER.error("Error during the bleach process", e);
      return null;
    }

    ByteArrayInputStream fakeFile = new ByteArrayInputStream(os.toByteArray());

    PDEmbeddedFile ef;
    try {
      ef = new PDEmbeddedFile(doc, fakeFile, COSName.FLATE_DECODE);
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


  private <T extends COSObjectable> void sanitizeRecursiveNameTree(PDNameTreeNode<T> efTree,
      Consumer<T> callback) {
    if (efTree == null) {
      return;
    }

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
    if (efTree.getKids() == null) {
      return;
    }
    for (PDNameTreeNode<T> node : efTree.getKids()) {
      sanitizeRecursiveNameTree(node, callback);
    }
  }


  void sanitize(PDEmbeddedFilesNameTreeNode embeddedFiles) {
    sanitizeRecursiveNameTree(embeddedFiles, this::sanitizeEmbeddedFile);
  }
}
