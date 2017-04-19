package xyz.docbleach.modules.ooxml;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachException;
import xyz.docbleach.api.IBleach;
import xyz.docbleach.api.IBleachSession;
import xyz.docbleach.api.IBleachSession.SEVERITY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Sanitizes Office 2007+ documents (OOXML), in place. This format has two interesting things for
 * us: a list of elements and a list of relationships. It is an Open Format, well documented and we
 * have a list of possible values. Each element/relationship is tied to a mime type, so we just
 * filter at this level: if the mime type is a macro, or an ActiveX object, we remove it.
 */
public class OOXMLBleach implements IBleach {
    private static final Logger LOGGER = LoggerFactory.getLogger(OOXMLBleach.class);
    private static final String SUSPICIOUS_OOXML_FORMAT = "Found and removed suspicious content type: '{}' in '{}' (Size: {})";
    private static final String EXTERNAL_RELATION_FORMAT = "Found an external relationship from '{}' to '{}' (type '{}')";
    private static final String OOXWORD_SCHEME = "ooxWord";

    private static final String[] WHITELISTED_RELATIONS = new String[]{
            // Hyperlinks should be safe enough, right?
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink"
    };

    private static final String[] BLACKLISTED_RELATIONS = new String[]{
            // Macro
            "http://schemas.microsoft.com/office/2006/relationships/vbaProject",

            // OLE Objects
            "http://schemas.microsoft.com/office/2006/relationships/oleObject",
            "http://schemas.microsoft.com/office/2006/relationships/e2Object",
            "http://schemas.microsoft.com/office/2006/relationships/e1Object",

            // ActiveX Controls
            "http://schemas.microsoft.com/office/2006/relationships/activeXControl"
    };

    private static final String[] BLACKLISTED_CONTENT_TYPES = new String[]{
            // Macro related content types
            "application/vnd.ms-word.vbaData+xml",
            "application/vnd.ms-office.vbaProject",
            "application/vnd.ms-office.vbaProjectSignature",

            // Blacklisting Postscript to prevent 0days
            "application/postscript",

            // OLE Objects
            "application/vnd.openxmlformats-officedocument.oleObject",

            // ActiveX objects
            "application/vnd.ms-office.activeX"
    };

    @Override
    public boolean handlesMagic(InputStream stream) throws IOException {
        return DocumentFactoryHelper.hasOOXMLHeader(stream);
    }

    @Override
    public String getName() {
        return "Office Bleach";
    }

    @Override
    public void sanitize(InputStream inputStream, OutputStream outputStream, IBleachSession session) throws BleachException {
        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
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
                // TODO: remap content types for macroEnabled documents
            }
            pkg.save(outputStream);

            // Prevent from writing the InputStream, even if this sounds absurd.
            pkg.revert();
        } catch (InvalidFormatException | IOException e) {
            throw new BleachException(e);
        }
    }

    private Iterator<PackagePart> getPartsIterator(OPCPackage pkg) throws BleachException {
        try {
            return pkg.getParts().iterator();
        } catch (InvalidFormatException e) {
            throw new BleachException(e);
        }
    }

    private void sanitize(IBleachSession session, RelationshipSource pkg, Iterable<PackageRelationship> relationships) {
        relationships.iterator().forEachRemaining(packageRelationship -> sanitize(session, pkg, packageRelationship));
    }

    private void sanitize(IBleachSession session, RelationshipSource pkg, PackageRelationship relationship) {
        String relationshipType = relationship.getRelationshipType();
        LOGGER.debug("Relation type '{}' found from '{}' to '{}'", relationshipType, relationship.getSource(), relationship.getTargetURI());

        if (isWhitelistedRelationType(relationshipType)) {
            return;
        }

        if (isBlacklistedRelationType(relationshipType)) {
            pkg.removeRelationship(relationship.getId());
            session.recordThreat("Blacklisted relationship type", SEVERITY.HIGH);
            return;
        }

        if (TargetMode.EXTERNAL.equals(relationship.getTargetMode())) {
            pkg.removeRelationship(relationship.getId());
            SEVERITY severity = SEVERITY.HIGH;

            if (OOXWORD_SCHEME.equals(relationship.getTargetURI().getScheme())) {
                // Fake external relationship
                severity = SEVERITY.EXTREME;
            }

            if (LOGGER.isDebugEnabled()) {
                String sourceUri = relationship.getSourceURI().toASCIIString();
                String targetUri = relationship.getTargetURI().toASCIIString();
                String relationType = relationship.getRelationshipType();

                LOGGER.debug(EXTERNAL_RELATION_FORMAT, sourceUri, targetUri, relationType);
            }
            session.recordThreat("External relationship", severity);
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

    void sanitize(IBleachSession session, OPCPackage pkg, PackagePart part) throws InvalidFormatException {
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
            session.recordThreat("Dynamic content", SEVERITY.HIGH);
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
        String full_type = type.toString(false);

        for (String _type : BLACKLISTED_CONTENT_TYPES) {
            if (_type.equalsIgnoreCase(full_type))
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