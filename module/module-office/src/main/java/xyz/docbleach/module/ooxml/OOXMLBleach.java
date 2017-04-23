package xyz.docbleach.module.ooxml;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sanitizes Office 2007+ documents (OOXML), in place. This format has two interesting things for
 * us: a list of elements and a list of relationships. It is an Open Format, well documented and we
 * have a list of possible values. Each element/relationship is tied to a mime type, so we just
 * filter at this level: if the mime type is a macro, or an ActiveX object, we remove it.
 */
public class OOXMLBleach implements Bleach {
    private static final Logger LOGGER = LoggerFactory.getLogger(OOXMLBleach.class);
    private static final String SUSPICIOUS_OOXML_FORMAT = "Found and removed suspicious content type: '{}' in '{}' (Size: {})";
    private static final String EXTERNAL_RELATION_FORMAT = "Found an external relationship from '{}' to '{}' (type '{}')";
    private static final String OOXWORD_SCHEME = "ooxWord";

    private static final String[] WHITELISTED_RELATIONS = new String[]{
            // Hyperlinks should be safe enough, right?
            Relations.HYPERLINK
    };

    private static final String[] BLACKLISTED_RELATIONS = new String[]{
            // Macro
            Relations.VBA_PROJECT,
            Relations.VBA_PROJECT_SIGNATURE,
            Relations.WORD_VBA_DATA,

            // OLE Objects
            Relations.OPENXML_OLE_OBJECT,
            Relations.OLE_OBJECT,
            Relations.E1_OBJECT,
            Relations.E2_OBJECT,

            // ActiveX Controls
            Relations.OPENXML_CONTROL,
            Relations.OPENXML_ACTIVEX_CONTROL,
            Relations.OPENXML_ACTIVEX_CONTROL_BIN,
            Relations.ACTIVEX_CONTROL,
            Relations.ACTIVEX_CONTROL_BIN
    };

    private static final String[] BLACKLISTED_CONTENT_TYPES = new String[]{
            // Macro related content types
            ContentTypes.VBA_DATA,
            ContentTypes.VBA_PROJECT,
            ContentTypes.VBA_PROJECT_SIGNATURE,

            // Blacklisting Postscript to prevent 0days
            ContentTypes.POSTSCRIPT,

            // OLE Objects
            ContentTypes.OLE_OBJECT,
            ContentTypes.PACKAGE,

            // ActiveX objects
            ContentTypes.ACTIVEX,
            ContentTypes.OPENXML_ACTIVEX,
            ContentTypes.OPENXML_ACTIVEX_XML,
    };

    private static final Map<String, String> REMAPPED_CONTENT_TYPES = new HashMap<>();

    static {
        // Word
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_DOCM, ContentTypes.MAIN_DOCX);
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_DOTM, ContentTypes.MAIN_DOTX);

        // Excel
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_XLTM, ContentTypes.MAIN_XLTX);
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_XLSM, ContentTypes.MAIN_XLSX);

        // PowerPoint
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_PPSM, ContentTypes.MAIN_PPSX);
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_PPTM, ContentTypes.MAIN_PPTX);
        REMAPPED_CONTENT_TYPES.put(ContentTypes.MAIN_POTM, ContentTypes.MAIN_POTX);
    }

    @Override
    public boolean handlesMagic(InputStream stream) {
        try {
            return DocumentFactoryHelper.hasOOXMLHeader(stream);
        } catch (IOException e) {
            LOGGER.warn("An exception occured", e);
            return false;
        }
    }

    @Override
    public String getName() {
        return "Office Bleach";
    }

    @Override
    public void sanitize(InputStream inputStream, OutputStream outputStream, BleachSession session) throws BleachException {
        try {
            inputStream.mark(inputStream.available() + 1);
        } catch (IOException e) {
            LOGGER.error("Error in OOXMLBleach", e);
        }

        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
            sanitize(pkg, session);

            pkg.save(outputStream);

            // Prevent from writing the InputStream, even if this sounds absurd.
            pkg.revert();
        } catch (InvalidFormatException ignored) {
            // We can't canitize this file, so we ignore it
            try {
                inputStream.reset();
                StreamUtils.copy(inputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("Error in OOXMLBleach", e);
            }
        } catch (IOException e) {
            throw new BleachException(e);
        }
    }

    public void sanitize(OPCPackage pkg, BleachSession session) throws BleachException, InvalidFormatException {
        LOGGER.trace("File opened");
        Iterator<PackagePart> it = getPartsIterator(pkg);

        pkg.ensureRelationships();

        sanitize(session, pkg, pkg.getRelationships());

        PackagePart part;
        while (it.hasNext()) {
            part = it.next();
            sanitize(session, pkg, part);

            if (!part.isRelationshipPart()) {
                sanitize(session, part, part.getRelationships());
            }

            if (part.isDeleted())
                continue;

            remapContentType(session, part);
        }
    }

    void remapContentType(BleachSession session, PackagePart part) throws InvalidFormatException {
        String oldContentType = part.getContentType();
        if (!REMAPPED_CONTENT_TYPES.containsKey(oldContentType)) {
            return;
        }

        String newContentType = REMAPPED_CONTENT_TYPES.get(part.getContentType());
        part.setContentType(newContentType);

        LOGGER.debug("Content type of '{}' changed from '{}' to '{}'", part.getPartName(), oldContentType, newContentType);

        Threat threat = new Threat(ThreatType.UNRECOGNIZED_CONTENT,
                ThreatSeverity.LOW,
                part.getPartName().getName(),
                "Remapped content type: " + oldContentType,
                ThreatAction.DISARM
        );
        session.recordThreat(threat);
    }

    private Iterator<PackagePart> getPartsIterator(OPCPackage pkg) throws BleachException {
        try {
            return pkg.getParts().iterator();
        } catch (InvalidFormatException e) {
            throw new BleachException(e);
        }
    }

    private void sanitize(BleachSession session, RelationshipSource pkg, Iterable<PackageRelationship> relationships) {
        relationships.iterator().forEachRemaining(packageRelationship -> sanitize(session, pkg, packageRelationship));
    }

    private void sanitize(BleachSession session, RelationshipSource pkg, PackageRelationship relationship) {
        String relationshipType = relationship.getRelationshipType();
        LOGGER.debug("Relation type '{}' found from '{}' to '{}'", relationshipType, relationship.getSource(), relationship.getTargetURI());

        if (isWhitelistedRelationType(relationshipType)) {
            return;
        }

        if (isBlacklistedRelationType(relationshipType)) {
            pkg.removeRelationship(relationship.getId());

            Threat threat = new Threat(ThreatType.ACTIVE_CONTENT,
                    ThreatSeverity.HIGH,
                    relationship.getSource().getPartName().getName(),
                    "Blacklisted relationship type: " + relationshipType,
                    ThreatAction.REMOVE
            );
            session.recordThreat(threat);
            return;
        }

        if (TargetMode.EXTERNAL.equals(relationship.getTargetMode())) {
            pkg.removeRelationship(relationship.getId());
            ThreatSeverity severity = ThreatSeverity.HIGH;

            if (OOXWORD_SCHEME.equals(relationship.getTargetURI().getScheme())) {
                // Fake external relationship
                severity = ThreatSeverity.EXTREME;
            }

            if (LOGGER.isDebugEnabled()) {
                String sourceUri = relationship.getSourceURI().toASCIIString();
                String targetUri = relationship.getTargetURI().toASCIIString();
                String relationType = relationship.getRelationshipType();

                LOGGER.debug(EXTERNAL_RELATION_FORMAT, sourceUri, targetUri, relationType);
            }

            Threat threat = new Threat(ThreatType.EXTERNAL_CONTENT,
                    severity,
                    relationship.getSource().getPartName().getName(),
                    "External relationship of type: " + relationshipType,
                    ThreatAction.REMOVE
            );
            session.recordThreat(threat);
        }
    }

    private boolean isBlacklistedRelationType(String relationshipType) {
        for (String rel : BLACKLISTED_RELATIONS) {
            if (rel.equalsIgnoreCase(relationshipType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWhitelistedRelationType(String relationshipType) {
        for (String rel : WHITELISTED_RELATIONS) {
            if (rel.equalsIgnoreCase(relationshipType)) {
                return true;
            }
        }
        return false;
    }

    void sanitize(BleachSession session, OPCPackage pkg, PackagePart part) {
        LOGGER.trace("Part name: {}", part.getPartName());

        String contentType = part.getContentType();
        LOGGER.debug("Content type: {} for part {}", contentType, part.getPartName());

        // Sample content types:
        // vnd.ms-word.vbaData+xml, vnd.ms-office.vbaProject
        // cf https://msdn.microsoft.com/fr-fr/library/aa338205(v=office.12).aspx
        ContentType type = part.getContentTypeDetails();
        if (isForbiddenType(type) || isStrangeContentType(type)) {
            LOGGER.debug(SUSPICIOUS_OOXML_FORMAT, contentType, part.getPartName(), part.getSize());
            deletePart(pkg, part.getPartName());

            Threat threat = new Threat(ThreatType.ACTIVE_CONTENT,
                    ThreatSeverity.HIGH,
                    part.getPartName().getName(),
                    "Forbidden content type: " + type,
                    ThreatAction.REMOVE
            );
            session.recordThreat(threat);

        }
    }

    /**
     * Delete the part with the specified name and its associated relationships part if one exists.
     * <p>
     * Unlike {@link OPCPackage#deletePart(PackagePartName)}, this checks if the relationship exists
     * before trying to remove it, instead of throwing an exception.
     *
     * @param partName Name of the part to delete
     */
    private void deletePart(OPCPackage pkg, PackagePartName partName) {
        pkg.removePart(partName);

        PackagePartName relationshipPartName = PackagingURIHelper.getRelationshipPartName(partName);
        if (relationshipPartName != null && pkg.containPart(relationshipPartName)) {
            pkg.removePart(relationshipPartName);
        }
    }

    boolean isForbiddenType(ContentType type) {
        String fullType = type.toString(false);

        for (String _type : BLACKLISTED_CONTENT_TYPES) {
            if (_type.equalsIgnoreCase(fullType))
                return true;
        }

        return false;
    }

    boolean isStrangeContentType(ContentType contentTypeDetails) {
        String contentType = contentTypeDetails.getType();

        switch (contentType) {
            case "application":
            case "image":
            case "audio":
            case "video":
                return false;
            default:
                LOGGER.error("Unknown content type: {}", contentType);
                return true;
        }
    }
}