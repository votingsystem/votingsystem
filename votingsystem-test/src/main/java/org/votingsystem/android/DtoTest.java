package org.votingsystem.android;


import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.android.xml.XmlReader;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.util.logging.Logger;

public class DtoTest extends BaseTest {

    private static final Logger log = Logger.getLogger(DtoTest.class.getName());

    public static void main(String[] args) throws Exception {
        new DtoTest().voteTest();
        System.exit(0);
    }

    public static void electionResultListTest() throws Exception {
        ResponseDto response = HttpConn.getInstance().doGetRequest(
                OperationType.FETCH_ELECTIONS.getUrl(Constants.VOTING_SERVICE_ENTITY_ID), MediaType.XML);
        ResultListDto<ElectionDto> elections = XmlReader.readElections(response.getMessageBytes());
        log.info("StatusCode: " + response.getStatusCode() + " - message: " + response.getMessage());
    }

    public static void voteTest() throws Exception {
        VoteDto vote = new VoteDto();
        ElectionOptionDto electionOptionDto = new ElectionOptionDto("content", null);
        vote.setRevocationHashBase64("RevocationHashBase64")
                .setOptionSelected(electionOptionDto)
                .setIndentityServiceEntity("IndentityServiceEntity").setVotingServiceEntity("VotingServiceEntity")
                .setOperation(OperationType.SEND_VOTE);
        String voteStr = XML.getMapper().writeValueAsString(vote);
        log.info("" + voteStr);
        VoteDto voteDto = XmlReader.readVoteDto(voteStr.getBytes());
        log.info("voteDto: " + voteDto.getOperation());
    }
}
