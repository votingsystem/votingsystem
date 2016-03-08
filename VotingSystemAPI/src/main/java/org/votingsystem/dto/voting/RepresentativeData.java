package org.votingsystem.dto.voting;


import org.votingsystem.cms.CMSSignedMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeData {
    
    private static Logger log = Logger.getLogger(RepresentativeData.class.getName());
    
    private String nif;
    private Long id;
    private CMSSignedMessage accessRequest;
    private CMSSignedMessage vote;
    private List<CMSSignedMessage> representationDocumentList = new ArrayList<CMSSignedMessage>();
    private byte[] representedReport;

    private Long optionSelectedId = null;
    private Long numRepresentedWithVote = null;
    private Long numRepresentations = null;
    private Long numVotesRepresented = null;
    private String representationDocumentFileName = null;


    public RepresentativeData() { }
    
    public RepresentativeData(String nif) {
        this.nif = nif;
    }
    
    public void addRepresentationDoc(CMSSignedMessage repDoc, String representationDocumentFileName) {
        representationDocumentList.add(repDoc);
        this.representationDocumentFileName = representationDocumentFileName;
    }
    
    public int getNumVotesRepresentedForEvent() {
        if(vote == null) return 0;
        else return representationDocumentList.size() - 
                getNumVotesOfRepresented() + 1;
    }
    
    public int getNumVotesOfRepresented () {
        int numVotesOfRepresented = 0;
        for(CMSSignedMessage repDoc:representationDocumentList) {
            if(representationDocumentFileName.contains("WithRequest_")) numVotesOfRepresented++;
        }      
        return numVotesOfRepresented;
    }    
    
    /**
     * @return the accessRequest
     */
    public CMSSignedMessage getAccessRequest() {
        return accessRequest;
    }

    /**
     * @param accessRequest the accessRequest to set
     */
    public void setAccessRequest(CMSSignedMessage accessRequest) {
        this.accessRequest = accessRequest;
    }

    /**
     * @return the vote
     */
    public CMSSignedMessage getVote() {
        return vote;
    }

    /**
     * @param vote the vote to set
     */
    public void setVote(CMSSignedMessage vote) {
        this.vote = vote;
    }

    /**
     * @return the representedReport
     */
    public byte[] getRepresentedReport() {
        return representedReport;
    }

    /**
     * @param representedReport the representedReport to set
     */
    public void setRepresentedReport(byte[] representedReport) {
        this.representedReport = representedReport;
    }

    /**
     * @return the representationDocumentList
     */
    public List<CMSSignedMessage> getRepresentationDocumentList() {
        return representationDocumentList;
    }
    
    
    /**
     * @return the number of users represented by the representative on this voting.
     * 
     * This is the result of total number of representations minus the one who has voted.
     */
    public Integer getNumRepresented() {
        if(representationDocumentList == null) return null;
        return new Integer(representationDocumentList.size() -
                getNumVotesOfRepresented());
    }

    /**
     * @param representationDocumentList the representationDocumentList to set
     */
    public void setRepresentationDocument(
            List<CMSSignedMessage> representationDocumentList) {
        this.representationDocumentList = representationDocumentList;
    }

    /**
     * @return the nif
     */
    public String getNif() {
        return nif;
    }

    /**
     * @param nif the nif to set
     */
    public void setNif(String nif) {
        this.nif = nif;
    }

    /**
     * @return the optionSelectedId
     */
    public Long getOptionSelectedId() {
        return optionSelectedId;
    }
   
    public Long getOptionSelectedIdFromVote() throws Exception {
        if(vote == null) return null;
        VoteVSDto voteVS = vote.getSignedContent(VoteVSDto.class);
        return voteVS.getOptionSelected().getId();
    }
    /**
     * @param optionSelectedId the optionSelectedId to set
     */
    public void setOptionSelectedId(Long optionSelectedId) {
        this.optionSelectedId = optionSelectedId;
    }

    /**
     * @return the numRepresentedWithVote
     */
    public Long getNumRepresentedWithVote() {
        return numRepresentedWithVote;
    }

    /**
     * @param numRepresentedWithVote the numRepresentedWithVote to set
     */
    public void setNumRepresentedWithVote(Long numRepresentedWithVote) {
        this.numRepresentedWithVote = numRepresentedWithVote;
    }

    /**
     * @return the numRepresentations
     */
    public Long getNumRepresentations() {
        return numRepresentations;
    }

    /**
     * @param numRepresentations the numRepresentations to set
     */
    public void setNumRepresentations(Long numRepresentations) {
        this.numRepresentations = numRepresentations;
    }

    /**
     * @return the numVotesRepresented
     */
    public Long getNumVotesRepresented() {
        return numVotesRepresented;
    }

    /**
     * @param numVotesRepresented the numVotesRepresented to set
     */
    public void setNumVotesRepresented(Long numVotesRepresented) {
        this.numVotesRepresented = numVotesRepresented;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getString() {
        return "[numRepresentations: " + numRepresentations + 
                " - numRepresentedWithVote: "+ numRepresentedWithVote+ 
                " - numVotesRepresented: " + numVotesRepresented + "]";
    }

}
