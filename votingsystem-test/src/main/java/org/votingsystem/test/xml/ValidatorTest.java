package org.votingsystem.test.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.votingsystem.util.FileUtils;
import org.votingsystem.xml.XMLValidator;

import java.net.URL;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidatorTest {

    private static final Logger log = LoggerFactory.getLogger(ValidatorTest.class);

    public static void main(String[] args) throws Exception {
        //QRResponseDto qrResponseDto = new QRResponseDto(OperationType.ANON_VOTE_CERT_REQUEST, LocalDateTime.now());
        //log.info(XML.getMapper().writeValueAsString(qrResponseDto));
        validate();
    }

    public static void validate() throws Exception {
        URL reqXML = Thread.currentThread().getContextClassLoader().getResource("xml/QRResponse.xml");
        URL resSchema = Thread.currentThread().getContextClassLoader().getResource("xml/QRResponse.xsd");
        byte[] reqBytes = FileUtils.getBytesFromStream(reqXML.openStream());
        XMLValidator.validate(reqBytes, resSchema);
        System.exit(0);
    }

}
