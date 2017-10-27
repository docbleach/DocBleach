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
   
    /** bleach DDE in external link and document */
    private static final String DDEAUTO = "DDEAUTO";
    
    private static final String ATTRIBUTE_DDESERVICE_DATA = "ddeService";
    private static final String REGEXP_DDESERVICE_DATA = ATTRIBUTE_DDESERVICE_DATA + "=\".*?\"";
   
    private static final String ATTRIBUTE_DDETOPIC_DATA = "ddeTopic";
    private static final String REGEXP_DDETOPIC_DATA = ATTRIBUTE_DDETOPIC_DATA + "=\".*?\"";
    
    private static final String DDE_DATA_BLEACHED1 = "cmd";
    private static final String DDE_DATA_BLEACHED2 = "exit";
    
    
    private OOXMLTagHelper() {
    }
    
    /**
     * The externalData tag is embedded in xml files to automatically load OLE object or
     * macro, or any kind of potential threat. Remove this tag prevent MS Office from
     * crashing. Actually, if you only remove the relation it crashes, that's why you have 
     * to remove the relation and the reference of the relation (via the externalData tag)
     * in the xml file.
     * Also removes DDE.
     * @param session The current bleach session where the threat can be reported
     * @param part The package part to sanitize
     */
    protected static void removeExternalDataTagAndDDE(BleachSession session, PackagePart part) {
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
    	
    	boolean external = content.indexOf(TAG_EXTERNAL_DATA) != -1;
    	boolean ddeauto = content.indexOf(DDEAUTO) != -1 || content.indexOf(ATTRIBUTE_DDESERVICE_DATA) != -1;
    	
    	// The evil tag has not been found, return
    	if (!external && !ddeauto) {
    		return;
    	}
    	
    	LOGGER.debug((external ? "externalData tag" : "DDE ") + " has been spotted {}", part);
    	
    	// Replace the tag by a comment
    	content = content.replaceAll(REGEXP_EXTERNAL_DATA, XML_COMMENT_BLEACHED);
    	
    	// Replace DDEAUTO with nothing, DDE will not trigger
    	content = content.replaceAll(DDEAUTO, "");
    	
    	// Replace ddeService & ddeTopic with cmd.exe exit
    	content = content.replaceAll(REGEXP_DDESERVICE_DATA, ATTRIBUTE_DDESERVICE_DATA + "=\""+ DDE_DATA_BLEACHED1 +"\"");
    	content = content.replaceAll(REGEXP_DDETOPIC_DATA, ATTRIBUTE_DDETOPIC_DATA + "=\""+ DDE_DATA_BLEACHED2 +"\"");
    	
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
        		.type(external ? ThreatType.EXTERNAL_CONTENT : ThreatType.ACTIVE_CONTENT)
    		    .severity(ThreatSeverity.HIGH)
    		    .action(ThreatAction.REMOVE)
    		    .location(part.getPartName().getName())
    		    .details("Removed tag \" " + (external ? "externalData" : "DDEAUTO") + "\" from the document.")
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
