package org.votingsystem.idprovider;

import org.junit.Test;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.util.DateUtils;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.xml.XML;

import java.time.LocalDateTime;

public class IdentityRequestTest {


    @Test
    public void checkIdentityRequest() throws Exception {
        IdentityRequestDto requestDto = new IdentityRequestDto();
        SystemEntityDto systemEntityDto = new SystemEntityDto();
        systemEntityDto.setId("https://voting.ddns.net/voting-service").setEntityType(SystemEntityType.VOTING_SERVICE_PROVIDER);
        requestDto.setIndentityServiceEntity(systemEntityDto);
        requestDto.setUUID("ElectionUUID---");
        System.out.println(XML.getMapper().writeValueAsString(requestDto));

    }

}