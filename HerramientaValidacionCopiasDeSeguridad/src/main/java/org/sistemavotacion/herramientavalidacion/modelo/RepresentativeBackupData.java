package org.sistemavotacion.herramientavalidacion.modelo;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeBackupData {
    
    private static Logger logger = LoggerFactory.getLogger(
            RepresentativeBackupData.class);
    
    private String nif;
    private SignedFile accessRequest;
    private SignedFile vote;
    private List<SignedFile> representationDocumentList = new ArrayList<SignedFile>();
    private byte[] representedReport;

    RepresentativeBackupData(String nif) {
        this.nif = nif;
    }
    
    public void addRepresentationDoc(SignedFile repDoc) {
        if(repDoc == null) {
            logger.debug("repDoc null");
            return;
        } 
        representationDocumentList.add(repDoc);
    }
    
    public Long getSelectedOptionId() {
        if(vote == null) {
            return null;
        }
        return vote.getSelectedOptionId();
    }
    
    public int getWeightedVote() {
        if(vote == null) return getNumVotesOfRepresented();
        else return representationDocumentList.size();
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
    
}
