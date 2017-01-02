package org.votingsystem.xml;

import org.votingsystem.throwable.XMLValidationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XMLValidator {

    public static void validate(byte[] xmlBytes, URL schemaRes) throws SAXException, IOException,
            XMLValidationException {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(schemaRes);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xmlBytes)));
        } catch (SAXParseException ex) {
            throw new XMLValidationException("Line number: "+ ex.getLineNumber() + " - Column number: " +
                    ex.getColumnNumber() + " - " + ex.getMessage(), ex);
        }
    }

    public static void validatePublishElectionRequest(byte[] xmlBytes) throws SAXException, IOException,
            XMLValidationException {
        URL schemaURL = Thread.currentThread().getContextClassLoader().getResource("xsd/publishElectionRequest.xsd");
        validate(xmlBytes, schemaURL);
    }

}
