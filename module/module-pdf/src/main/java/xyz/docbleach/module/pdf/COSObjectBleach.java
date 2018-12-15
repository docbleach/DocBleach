package xyz.docbleach.module.pdf;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class COSObjectBleach {

  private static final Logger LOGGER = LoggerFactory.getLogger(COSObjectBleach.class);
  private final PdfBleachSession pdfBleachSession;

  COSObjectBleach(PdfBleachSession pdfBleachSession) {
    this.pdfBleachSession = pdfBleachSession;
  }

  private void crawl(COSBase base) {
    if (base == null) {
      return;
    }

    if (base instanceof COSName
        || base instanceof COSString
        || base instanceof COSStream
        || base instanceof COSNull
        || base instanceof COSObject
        || base instanceof COSNumber
        || base instanceof COSBoolean) {
      return;
    }

    if (base instanceof COSDictionary) {
      COSDictionary dict = (COSDictionary) base;
      Iterator<Entry<COSName, COSBase>> it = dict.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<COSName, COSBase> entry = it.next();
        if ("JS".equals(entry.getKey().getName())
            || "JavaScript".equals(entry.getKey().getName())) {
          it.remove();
          LOGGER.debug("Found and removed Javascript code");
          pdfBleachSession.recordJavascriptThreat("?", "JS Code");
          continue;
        }

        if ("S".equals(entry.getKey().getName())) {
          if (entry.getValue() instanceof COSName) {
            if ("JavaScript".equals(((COSName) entry.getValue()).getName())) {
              LOGGER.debug("Found and removed Javascript code");
              it.remove();
              pdfBleachSession.recordJavascriptThreat("?", "JS Code");
              continue;
            }
          }
        }

        if ("AA".equals(entry.getKey().getName())) {
          LOGGER.debug("Found and removed Additionnal Actions");
          it.remove();
          pdfBleachSession.recordJavascriptThreat("?", "Additional Actions");
          continue;
        }
        crawl(entry.getValue());
      }
    } else if (base instanceof COSArray) {
      COSArray ar = (COSArray) base;

      for (COSBase item : ar) {
        crawl(item);
      }
    } else {
      LOGGER.error("Unknown COS type: {}", base);
    }
  }


  void sanitizeObjects(Collection<COSObject> objects) {
    LOGGER.trace("Checking all objects..."); // Most destructive operation
    objects.forEach(this::crawl);
  }
}
