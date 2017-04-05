package org.votingsystem.idprovider;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.indentity.IdentityTokenDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        createResponse();
    }

    private static void createIdentityRequestDto() throws IOException {
        IdentityRequestDto requestDto = new IdentityRequestDto();
        //requestDto.setLanguage().; ZoneId.systemDefault().toString()
        log.info(XML.getMapper().writeValueAsString(requestDto));
    }

    private static void createResponse() throws IOException {
        SystemEntityDto identityEntityDto = new SystemEntityDto("https://voting.ddns.net/voting-service",
                SystemEntityType.VOTING_SERVICE_PROVIDER);
        IdentityTokenDto indentityResponseDto = new IdentityTokenDto(OperationType.ANON_VOTE_CERT_REQUEST,
                identityEntityDto, "electionId");
        String identityResponseDto = new XmlMapper().writerWithDefaultPrettyPrinter().writeValueAsString(indentityResponseDto);
        System.out.println("identityResponseDto: " + identityResponseDto);
        System.out.println("identityResponseDtobase64: " + Base64.getEncoder().encodeToString(identityResponseDto.getBytes()));
        IdentityTokenDto indentityResponseDto1 = new XmlMapper().readValue(identityResponseDto.getBytes(), IdentityTokenDto.class);
        System.out.println("indentityResponseDto1: " + indentityResponseDto1);
    }

}
