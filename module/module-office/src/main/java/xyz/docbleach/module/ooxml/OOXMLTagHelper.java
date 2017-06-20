package xyz.docbleach.module.ooxml;

import static xyz.docbleach.api.threat.ThreatBuilder.threat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.ZipPackagePart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.threat.ThreatAction;
import xyz.docbleach.api.threat.ThreatSeverity;
import xyz.docbleach.api.threat.ThreatType;

/**
 * Helper dedicated to alter XML tags.
 */
public class OOXMLTagHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OOXMLTagHelper.class);
	
    private static final String XML_EXTENSION = "xml";
    private static final String XML_COMMENT_BLEACHED = "<!-- bleached -->";
    private static final String TAG_EXTERNAL_DATA = "externalData";
    /** The regexp try to catch the whole tag, including namespace and attributes */
    private static final String REGEXP_EXTERNAL_DATA = "<.." + TAG_EXTERNAL_DATA + ".*?/>";
    
    private OOXMLTagHelper() {
    }
    
    /**
     * The externalData tag is embedded in xml files to automatically load OLE object or
     * macro, or any kind of potential threat. Remove this tag prevent MS Office from
     * crashing. Actually, if you only remove the relation it crashes, that's why you have 
     * to remove the relation and the reference of the relation (via the externalData tag)
     * in the xml file.
     * @param session The current bleach session where the threat can be reported
     * @param part The package part to sanitize
     */
    protected static void removeExternalDataTag(BleachSession session, PackagePart part) {
    	// Only applicable if the file is an XML file (not a _refs or whatever)
    	// And is a ZipPackagePart, not a config file or whatever.
    	if (!XML_EXTENSION.equals(part.getPartName().getExtension()) || !(part instanceof ZipPackagePart)) {
    		return;
    	}
   		
    	String content = readPartContent(part);
    	// An error occured
    	if (content == null) {
    		return;
    	}
    	
    	// The evil tag has not been found, return
    	if (content.indexOf(TAG_EXTERNAL_DATA) == -1) {
    		return;
    	}
    	
    	// Replace the tag by a comment
    	LOGGER.debug("externalData tag has been spotted {}", part);
    	content = content.replaceAll(REGEXP_EXTERNAL_DATA, XML_COMMENT_BLEACHED);
    	
    	// Write the result
    	try (OutputStream os = part.getOutputStream()) {
    		os.write(content.getBytes());
    		os.close();
    	} catch (IOException ex) {
    		LOGGER.error("Error while writing the part content. The file may be corrupted.", ex);
    		return;
    	}
    			
        session.recordThreat(
        	threat()
        		.type(ThreatType.EXTERNAL_CONTENT)
    		    .severity(ThreatSeverity.HIGH)
    		    .action(ThreatAction.REMOVE)
    		    .location(part.getPartName().getName())
    		    .details("Removed tag \"externalData\" from the document.")
    		    .build()
        );
	}
    
	/**
	 * Read the part content.
	 * @param part The {@link org.apache.poi.openxml4j.opc.PackagePart PackagePart} the content must be read from
	 * @return A string containing the content of the part.
	 */
    private static String readPartContent(PackagePart part) {
    	try (InputStream is = part.getInputStream()) {
    		// Read the file content first
    		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    		
        	StringBuilder buf = new StringBuilder();
    		String line = reader.readLine();
    		while (line != null) {
    			buf.append(line);
    			line = reader.readLine();
    		}
    		
    		return buf.toString();
    	} catch (IOException ex) {
    		LOGGER.error("Error while bleaching {}. The file may be corrupted :/", part.getPartName().getName(), ex);
    		return null;
    	}
    }
}
