package org.sistemavotacion.herramientavalidacion.modelo;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sistemavotacion.modelo.MetaInf;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.seguridad.CertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingBackupData {
    
    private static Logger logger = LoggerFactory.getLogger(VotingBackupData.class);
    
    private List<SignedFile> accessRequests = new ArrayList<SignedFile>();
    private List<SignedFile> votes = new ArrayList<SignedFile>();
    private Map<String, RepresentativeBackupData> representativesMap = 
            new HashMap<String, RepresentativeBackupData>();
    
    private byte[] accessRequestTrustedCertsBytes;
    private byte[] eventTrustedCertsBytes;
    private byte[] representativeReport;
    private Map<Long, Integer> optionsMap = new HashMap<Long, Integer>();
    private Collection<X509Certificate> eventTrustedCerts;
    private Collection<X509Certificate> accessRequestTrustedCerts;
    private MetaInf metaInf;    
    
    public String getFormattedInfo() {
        int numRepresentatives = representativesMap.keySet().size();
        int numRepresentativesWithVote = 0;
        int numRepresented = 0;
        int numVotesOfRepresented = 0;
        int numVotesRepresentedForEvent = 0;
        int numTotalVotesForEvent = 0;
        Collection<RepresentativeBackupData>  repList = 
                representativesMap.values();
        
        calculateOptionsValues(); 
        for(RepresentativeBackupData rep: repList) {
            if(rep.getVote() != null) {
                numRepresentativesWithVote++;
                numVotesRepresentedForEvent += rep.getNumVotesRepresentedForEvent();
                numTotalVotesForEvent += rep.getNumVotesRepresentedForEvent();
            } 
            numRepresented += rep.getRepresentationDocumentList().size();
            numVotesOfRepresented += rep.getNumVotesOfRepresented();
            numTotalVotesForEvent += rep.getNumVotesOfRepresented();
        }
        
        int numVotesUsersWithoutRepresentative = votes.size() - numVotesOfRepresented;
        numTotalVotesForEvent += numVotesUsersWithoutRepresentative;
        
        StringBuilder result = new StringBuilder(
                "\n - with eventTrustedCerts: " + (eventTrustedCerts != null)).append(
                "\n - with accessRequestTrustedCertsBytes: " + (
                    accessRequestTrustedCertsBytes != null)).append(
                "\n - with representative report: " + (representativeReport != null)).append(
                "\n - num. representatives: " + numRepresentatives + 
                "\n - num. total represented: " + numRepresented + 
                "\n - num. representative with vote: " + numRepresentativesWithVote +
                "\n - num. votes represented by representatives: " + numVotesRepresentedForEvent +
                "\n - num. votes users WITH representative: " + numVotesOfRepresented + 
                "\n - num. votes users WITHOUT representative: " + 
                       numVotesUsersWithoutRepresentative +
                "\n - num. access request: " + accessRequests.size());
        

        Set<Long> options = optionsMap.keySet();
        if(!options.isEmpty()) {
            result.append("\n - RECUENTO DE VOTOS: \n - Num. valid votes: " + 
                    numTotalVotesForEvent + " (" + numVotesRepresentedForEvent + 
                " votes represented by representatives + " + votes.size() + " votes from users)");
            for(Long option: options) {
                result.append("\n ----- " + metaInf.getOptionContent(option) + " :" +
                        optionsMap.get(option) + " votes");       
            }
        }
        return result.toString();
    }

    
    //       
    //"opcionSeleccionadaId":1, "opcionSeleccionadaContenido":"si"
    public Map calculateOptionsValues() {
        logger.debug("calculateOptionsValues - num. votes: " + votes.size());
        List<OpcionEvento> optionList = metaInf.getOptionList();
        if(optionList == null || optionList.isEmpty()) {
            logger.debug("Metainf without opions");
            return null;
        }
        for(OpcionEvento option:optionList) {
            optionsMap.put(option.getId(), new Integer(0));
        }
        for(SignedFile vote : votes) {
            Integer numVotes = optionsMap.get(vote.getSelectedOptionId()) + 1;
            optionsMap.put(vote.getSelectedOptionId(), numVotes);
        }
        Collection<RepresentativeBackupData>  repList = 
                representativesMap.values();
        for(RepresentativeBackupData rep: repList) {
            if(rep.getSelectedOptionId() == null) {
                logger.debug(" representative: " + rep.getNif() + " has no vote");
            } else {
                Integer numVotes = optionsMap.get(rep.getSelectedOptionId()) + 
                        rep.getNumVotesRepresentedForEvent();
                        optionsMap.put(rep.getSelectedOptionId(), numVotes);
            }
        }
        return optionsMap;
    }
    
    public void addRepresentationDoc(SignedFile repDoc) {
        if(repDoc == null) {
            logger.debug("addRepresentationDoc - repDoc null");
            return;
        }
        String[] parts = repDoc.getName().split("representative_");
        String nif = parts[1].split("/")[0];
        RepresentativeBackupData repBackup = representativesMap.get(nif);
        if(repBackup == null) {
            repBackup = new RepresentativeBackupData(nif);
        }
        repBackup.addRepresentationDoc(repDoc);
        representativesMap.put(nif, repBackup);
    }
    
    public void addVote(SignedFile vote) {
        if(vote == null) {
            logger.debug("vote null");
            return;
        } 
        votes.add(vote);
    }
    
    public void addAccessRequests(SignedFile accessRequest) {
        if(accessRequest == null) {
            logger.debug("accessRequest null");
            return;
        } 
        accessRequests.add(accessRequest);
    }
    
    public void addRepresentativeVote(SignedFile representativeVote) {
        if(representativeVote == null) {
            logger.debug("representativeVote null");
            return;
        } 
        String[] parts = representativeVote.getName().split("VotoRepresentante_");
        String nif = parts[1].split(".p7m")[0];
        RepresentativeBackupData repBackup = null;
        if(representativesMap == null) {
            representativesMap = new HashMap<String, RepresentativeBackupData>();
        } else {
            repBackup = representativesMap.get(nif);
            if(repBackup == null) {
                repBackup = new RepresentativeBackupData(nif);
            }
            repBackup.setVote(representativeVote);
        } 
        representativesMap.put(nif, repBackup);
    }
    
    /**
     * @return the accessRequests
     */
    public List<SignedFile> getAccessRequests() {
        return accessRequests;
    }

    /**
     * @param accessRequests the accessRequests to set
     */
    public void setAccessRequests(List<SignedFile> accessRequests) {
        this.accessRequests = accessRequests;
    }

    /**
     * @return the votes
     */
    public List<SignedFile> getVotes() {
        return votes;
    }

    /**
     * @param votes the votes to set
     */
    public void setVotes(List<SignedFile> votes) {
        this.votes = votes;
    }

    /**
     * @return the accessRequestTrustedCertsBytes
     */
    public byte[] getAccessRequestTrustedCertsBytes() {
        return accessRequestTrustedCertsBytes;
    }

    /**
     * @param accessRequestTrustedCertsBytes the accessRequestTrustedCertsBytes to set
     */
    public void setAccessRequestTrustedCertsBytes(
            byte[] accessRequestTrustedCertsBytes) throws Exception {
        this.accessRequestTrustedCertsBytes = accessRequestTrustedCertsBytes;
        accessRequestTrustedCerts = CertUtil.fromPEMToX509CertCollection(
                accessRequestTrustedCertsBytes);
    }

    /**
     * @return the eventTrustedCertsBytes
     */
    public byte[] getEventTrustedCertsBytes() {
        return eventTrustedCertsBytes;
    }

    /**
     * @param eventTrustedCertsBytes the eventTrustedCertsBytes to set
     */
    public void setEventTrustedCertsBytes(
            byte[] eventTrustedCertsBytes) throws Exception {
        this.eventTrustedCertsBytes = eventTrustedCertsBytes;
        eventTrustedCerts = CertUtil.fromPEMToX509CertCollection(
                eventTrustedCertsBytes);
    }

    /**
     * @return the representativeReport
     */
    public byte[] getRepresentativeReport() {
        return representativeReport;
    }

    /**
     * @param representativeReport the representativeReport to set
     */
    public void setRepresentativeReport(byte[] representativeReport) {
        this.representativeReport = representativeReport;
    }

    /**
     * @return the representativesMap
     */
    public Map<String,RepresentativeBackupData> getRepresentativesMap() {
        return representativesMap;
    }

    /**
     * @param representativesMap the representativesMap to set
     */
    public void setRepresentativesMap(
            Map<String,RepresentativeBackupData> representativesMap) {
        this.representativesMap = representativesMap;
    }

    /**
     * @return the accessRequestTrustedCerts
     */
    public Collection<X509Certificate> getAccessRequestTrustedCerts() {
        return accessRequestTrustedCerts;
    }

    /**
     * @return the eventTrustedCerts
     */
    public Collection<X509Certificate> getEventTrustedCerts() {
        return eventTrustedCerts;
    }

    /**
     * @return the metaInf
     */
    public MetaInf getMetaInf() {
        return metaInf;
    }

    /**
     * @param metaInf the metaInf to set
     */
    public void setMetaInf(MetaInf metaInf) {
        this.metaInf = metaInf;
    }

}