package org.votingsystem.client.model;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativeData {
    
    private static Logger logger = Logger.getLogger(
            RepresentativeData.class);
    
    private String nif;
    private Long id;
    private SignedFile accessRequest;
    private SignedFile vote;
    private List<SignedFile> representationDocumentList = new ArrayList<SignedFile>();
    private byte[] representedReport;

    private Long optionSelectedId = null;
    private Long numRepresentedWithVote = null;
    private Long numRepresentations = null;
    private Long numVotesRepresented = null;
   
    public RepresentativeData() { }
    
    public RepresentativeData(String nif) {
        this.nif = nif;
    }
    
    public void addRepresentationDoc(SignedFile repDoc) {
        if(repDoc == null) {
            logger.debug("repDoc null");
            return;
        } 
        representationDocumentList.add(repDoc);
    }
    
    public int getNumVotesRepresentedForEvent() {
        if(vote == null) return 0;
        else return representationDocumentList.size() - 
                getNumVotesOfRepresented() + 1;
    }
    
    public int getNumVotesOfRepresented () {
        int numVotesOfRepresented = 0;
        for(SignedFile repDoc:representationDocumentList) {
            if(repDoc.getName().contains("WithRequest_")) numVotesOfRepresented++;
        }      
        return numVotesOfRepresented;
    }    
    
    /**
     * @return the accessRequest
     */
    public SignedFile getAccessRequest() {
        return accessRequest;
    }

    /**
     * @param accessRequest the accessRequest to set
     */
    public void setAccessRequest(SignedFile accessRequest) {
        this.accessRequest = accessRequest;
    }

    /**
     * @return the vote
     */
    public SignedFile getVote() {
        return vote;
    }

    /**
     * @param vote the vote to set
     */
    public void setVote(SignedFile vote) {
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
    public List<SignedFile> getRepresentationDocumentList() {
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
            List<SignedFile> representationDocumentList) {
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
   
    public Long getOptionSelectedIdFromVote() {
        if(vote == null) {
            return null;
        }
        return vote.getSelectedOptionId();
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
