package org.votingsystem.test.android.xml;

import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.OperationType;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.logging.Logger;


public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        identityRequest();
    }

    public static void identityRequest() throws Exception {
        IdentityRequestDto identityRequest = new IdentityRequestDto(OperationType.ANON_VOTE_CERT_REQUEST,
                UUID.randomUUID().toString(), new SystemEntityDto("ID_PROVIDER_URL", SystemEntityType.ID_PROVIDER))
                .setRevocationHash("RevocationHash").setDate(ZonedDateTime.now())
                .setCallbackServiceEntityId(new SystemEntityDto("SERVICE_PROVIDER_URL", SystemEntityType.VOTING_SERVICE_PROVIDER));
        //byte[] identityRequestBytes = XML.getMapper().writeValueAsBytes(identityRequest);
        byte[] identityRequestBytes = XmlWriter.write(identityRequest);
        log.info("identityRequest: " + new String(identityRequestBytes));
        identityRequest = XmlReader.readIdentityRequest(identityRequestBytes);
        log.info(identityRequest.getType().toString());
    }

}
