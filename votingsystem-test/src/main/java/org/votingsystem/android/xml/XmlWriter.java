package org.votingsystem.android.xml;

import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.test.util.XMLUtils;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XmlWriter {


    public static byte[] write(IdentityRequestDto identityRequest) throws Exception {
        Document doc = new Document();
        Element identityRequestElement = doc.createElement("", "IdentityRequest");
        identityRequestElement.setAttribute(null, "Type", identityRequest.getType().name());

        if(identityRequest.getIndentityServiceEntity() != null) {
            Element indentityServiceElement = doc.createElement("", "IndentityServiceEntity");
            indentityServiceElement.setAttribute(null, "Id", identityRequest.getIndentityServiceEntity().getId());
            indentityServiceElement.setAttribute(null, "Type",
                    identityRequest.getIndentityServiceEntity().getEntityType().getName());
            indentityServiceElement.addChild(Node.TEXT, "");
            identityRequestElement.addChild(Node.ELEMENT, indentityServiceElement);
        }

        if(identityRequest.getCallbackServiceEntityId() != null) {
            Element indentityServiceElement = doc.createElement("", "CallbackServiceEntity");
            indentityServiceElement.setAttribute(null, "Id", identityRequest.getCallbackServiceEntityId().getId());
            indentityServiceElement.setAttribute(null, "Type",
                    identityRequest.getCallbackServiceEntityId().getEntityType().getName());
            indentityServiceElement.addChild(Node.TEXT, "");
            identityRequestElement.addChild(Node.ELEMENT, indentityServiceElement);
        }

        Element revocationHashElement = doc.createElement("", "RevocationHashBase64");
        revocationHashElement.addChild(Node.TEXT, identityRequest.getRevocationHashBase64());
        identityRequestElement.addChild(Node.ELEMENT, revocationHashElement);

        Element uuidElement = doc.createElement("", "UUID");
        uuidElement.addChild(Node.TEXT, identityRequest.getUUID());
        identityRequestElement.addChild(Node.ELEMENT, uuidElement);

        doc.addChild(Node.ELEMENT, identityRequestElement);
        return XMLUtils.serialize(doc);
    }

    public static byte[] write(VoteDto vote) throws Exception {
        Document doc = new Document();
        Element voteElement = doc.createElement("", "Vote");

        Element operationElement = doc.createElement("", "Operation");
        operationElement.addChild(Node.TEXT, vote.getOperation().name());
        voteElement.addChild(Node.ELEMENT, operationElement);

        Element revocationHashElement = doc.createElement("", "RevocationHashBase64");
        revocationHashElement.addChild(Node.TEXT, vote.getRevocationHashBase64());
        voteElement.addChild(Node.ELEMENT, revocationHashElement);

        Element votingServiceEntityElement = doc.createElement("", "VotingServiceEntity");
        votingServiceEntityElement.addChild(Node.TEXT, vote.getVotingServiceEntity());
        voteElement.addChild(Node.ELEMENT, votingServiceEntityElement);

        Element indentityServiceEntity = doc.createElement("", "IndentityServiceEntity");
        indentityServiceEntity.addChild(Node.TEXT, vote.getIndentityServiceEntity());
        voteElement.addChild(Node.ELEMENT, indentityServiceEntity);

        Element electionUUIDElement = doc.createElement("", "ElectionUUID");
        electionUUIDElement.addChild(Node.TEXT, vote.getElectionUUID());
        voteElement.addChild(Node.ELEMENT, electionUUIDElement);

        if(vote.getOptionSelected() != null) {
            Element optionSelectedElement = doc.createElement("", "OptionSelected");
            Element contentElement = doc.createElement("", "Content");
            contentElement.addChild(Node.TEXT, vote.getOptionSelected().getContent());
            optionSelectedElement.addChild(Node.ELEMENT, contentElement);
            voteElement.addChild(Node.ELEMENT, optionSelectedElement);
        }

        doc.addChild(Node.ELEMENT, voteElement);
        return XMLUtils.serialize(doc);
    }


}
