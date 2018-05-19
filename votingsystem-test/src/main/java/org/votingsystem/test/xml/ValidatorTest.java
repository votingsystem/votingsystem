package org.votingsystem.test.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.test.Constants;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;
import org.votingsystem.xml.XMLValidator;

import java.net.URL;
import java.time.LocalDateTime;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidatorTest {

    private static final Logger log = LoggerFactory.getLogger(ValidatorTest.class);

    public static void main(String[] args) throws Exception {
        //String entityId, OperationType operationType, T data, LocalDateTime localDateTime
        OperationDto operation = new OperationDto(Constants.ID_PROVIDER_ENTITY_ID, OperationType.PUBLISH_ELECTION,
                null, LocalDateTime.now());
        log.info(new XML().getMapper().writeValueAsString(operation));
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