package org.votingsystem.test.android.xml;

import org.kxml2.kdom.Element;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.model.voting.Election;
import org.votingsystem.testlib.android.util.DateUtils;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.util.OperationType;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XmlReader {

    private static final Logger log = Logger.getLogger(XmlReader.class.getName());

    public static ElectionDto readElection(byte[] xmlBytes) throws IOException, XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        return getElection(mainElement);
    }

    public static QRResponseDto readQRResponse(byte[] xmlBytes) throws IOException, XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        QRResponseDto qrResponse = new QRResponseDto();
        mainElement.getAttributeValue(null, "QRResponse");
        qrResponse.setOperationType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        qrResponse.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        return qrResponse;
    }

    public static IdentityRequestDto readIdentityRequest(byte[] xmlBytes) throws IOException, XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        IdentityRequestDto request = new IdentityRequestDto();
        if(mainElement.getAttributeValue(null, "Date") != null) {
            log.info("====" + DateUtils.getXmlDate(mainElement.getAttributeValue(null, "Date")));
        }
        request.setType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        Element identityServiceElement = mainElement.getElement(null, "IndentityServiceEntity");
        if(identityServiceElement != null) {
            SystemEntityDto identityService = new SystemEntityDto(identityServiceElement.getAttributeValue(null, "Id"),
                    SystemEntityType.getByName(identityServiceElement.getAttributeValue(null, "Type")));
            request.setIndentityServiceEntity(identityService);
        }
        Element callbackServiceElement = mainElement.getElement(null, "CallbackServiceEntity");
        if(callbackServiceElement != null) {
            SystemEntityDto callbackService = new SystemEntityDto(callbackServiceElement.getAttributeValue(null, "Id"),
                    SystemEntityType.getByName(callbackServiceElement.getAttributeValue(null, "Type")));
            request.setCallbackServiceEntityId(callbackService);
        }
        request.setRevocationHashBase64(XMLUtils.getTextChild(mainElement, "RevocationHashBase64"));
        request.setUUID(XMLUtils.getTextChild(mainElement, "UUID"));
        return request;
    }

    public static ResponseDto readResponseDto(byte[] xmlBytes) throws IOException, XmlPullParserException {
        ResponseDto responseDto = new ResponseDto();
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        if(mainElement.getAttributeValue(null, "StatusCode") != null) {
            responseDto.setStatusCode(Integer.valueOf(mainElement.getAttributeValue(null, "StatusCode")));
        }
        responseDto.setMessage(XMLUtils.getTextChild(mainElement, "Message"));
        return responseDto;
    }

    public static ElectionDto getElection(Element electionElement) throws IOException, XmlPullParserException {
        ElectionDto election = new ElectionDto();
        String electionId = XMLUtils.getTextChild(electionElement, "Id");
        if(electionId != null)
            election.setId(Long.valueOf(XMLUtils.getTextChild(electionElement, "Id")));

        Element optionsElement = electionElement.getElement(null, "Options");
        if(optionsElement != null) {
            Set<ElectionOptionDto> electionOptions = new HashSet<>();
            for(int i = 0; i < optionsElement.getChildCount(); i++) {
                Element optionElement = optionsElement.getElement(i);
                electionOptions.add(new ElectionOptionDto(XMLUtils.getTextChild(optionElement, "Content"), null));
            }
            election.setElectionOptions(electionOptions);
        }
        String electionState = XMLUtils.getTextChild(electionElement, "State");
        if(electionState != null) {
            election.setState(Election.State.valueOf(electionState));
        }
        election.setDateBegin(org.votingsystem.util.DateUtils.getDateFromISO_OFFSET_DATE_TIME(XMLUtils.getTextChild(electionElement, "DateBegin")));
        election.setDateFinish(org.votingsystem.util.DateUtils.getDateFromISO_OFFSET_DATE_TIME(XMLUtils.getTextChild(electionElement, "DateFinish")));
        election.setEntityId(XMLUtils.getTextChild(electionElement, "EntityId"));
        election.setSubject(XMLUtils.getTextChild(electionElement, "Subject"));
        election.setContent(XMLUtils.getHTMLContent(electionElement, "Content"));
        election.setPublisher(XMLUtils.getTextChild(electionElement, "Publisher"));
        election.setUUID(XMLUtils.getTextChild(electionElement, "UUID"));
        return election;
    }

    public static ResultListDto<ElectionDto> readElections(byte[] xmlBytes) throws IOException, XmlPullParserException {
        ResultListDto<ElectionDto> result = new ResultListDto();
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        if(mainElement.getAttributeValue(null, "Type") != null)
            result.setType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        if(mainElement.getAttributeValue(null, "StatusCode") != null)
            result.setStatusCode(Integer.valueOf(mainElement.getAttributeValue(null, "StatusCode")));
        if(mainElement.getAttributeValue(null, "Offset") != null)
            result.setOffset(Integer.valueOf(mainElement.getAttributeValue(null, "Offset")));
        if(mainElement.getAttributeValue(null, "Max") != null)
            result.setMax(Integer.valueOf(mainElement.getAttributeValue(null, "Max")));
        if(mainElement.getAttributeValue(null, "TotalCount") != null)
            result.setTotalCount(Long.valueOf(mainElement.getAttributeValue(null, "TotalCount")));
        if(mainElement.getElement(null, "ItemList") != null) {
            Set<ElectionDto> elections = new HashSet<>();
            Element itemListElement = mainElement.getElement(null, "ItemList");
            for(int i = 0; i < itemListElement.getChildCount(); i++) {
                Element itemElement = itemListElement.getElement(i);
                elections.add(getElection(itemElement));
            }
            result.setResultList(elections);
        }
        result.setMessage(XMLUtils.getTextChild(mainElement, "Message"));
        result.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        return result;
    }

    //<Vote>
    // <OptionSelected><ElectionUUID>ElectionUUID</ElectionUUID></Vote>
    public static VoteDto readVoteDto(byte[] xmlBytes) throws IOException, XmlPullParserException {
        VoteDto vote = new VoteDto();
        Element voteElement = XMLUtils.parse(xmlBytes).getRootElement();
        vote.setOperation(OperationType.valueOf(XMLUtils.getTextChild(voteElement, "Operation")));
        vote.setRevocationHashBase64(XMLUtils.getTextChild(voteElement, "RevocationHashBase64"));
        vote.setIndentityServiceEntity(XMLUtils.getTextChild(voteElement, "IndentityServiceEntity"));
        vote.setVotingServiceEntity(XMLUtils.getTextChild(voteElement, "VotingServiceEntity"));
        if(voteElement.getElement(null, "OptionSelected") != null) {
            Element optionElement = voteElement.getElement(null, "OptionSelected");
            ElectionOptionDto electionOptionDto = new ElectionOptionDto()
                    .setContent(XMLUtils.getTextChild(optionElement, "Content"));
            if(XMLUtils.getElement(optionElement, "NumVotes") != null) {
                electionOptionDto.setNumVotes(Long.valueOf(XMLUtils.getTextChild(optionElement, "NumVotes")));
            }
            vote.setOptionSelected(electionOptionDto);
        }
        vote.setElectionUUID(XMLUtils.getTextChild(voteElement, "ElectionUUID"));
        return vote;
    }
}
