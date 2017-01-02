package org.votingsystem;


import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;

import java.text.MessageFormat;
import java.util.logging.Logger;


public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {

        System.out.println(dtoJSON("identityServiceEntity", "votingServiceEntity", "revocationHashBase64",
                "electionUUID"));
    }

    public static String dtoJSON(String identityServiceEntity, String votingServiceEntity, String revocationHashBase64
            , String electionUUID) throws Exception {
        return "{\"identityServiceEntity\":\"" + identityServiceEntity + "\",\"votingServiceEntity\":\"" +
                votingServiceEntity + "\"," + "\"revocationHashBase64\":\"" + revocationHashBase64 +
                "\",\"electionUUID\":\"" + electionUUID + "\"}";
    }

    public static void test() throws Exception {
        CertVoteExtensionDto dto = new CertVoteExtensionDto("indentityServiceEntity", "votingServiceEntity",
                "revocationHashBase64", "electionUUID");
        log.info("dto: " + JSON.getMapper().writeValueAsString(dto));
    }

}
