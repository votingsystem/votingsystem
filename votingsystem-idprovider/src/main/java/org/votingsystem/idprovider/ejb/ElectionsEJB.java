package org.votingsystem.idprovider.ejb;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.Election;
import org.votingsystem.throwable.NotFoundException;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@Lock(LockType.READ)
public class ElectionsEJB {

    private static final Logger log = Logger.getLogger(ElectionsEJB.class.getName());

    private static final Map<String, Election> electionsMap = new ConcurrentHashMap<>();

    @PersistenceContext
    private EntityManager em;
    @EJB private TrustedServicesEJB trustedServicesEJB;
    @EJB private SignatureServiceEJB signatureService;


    /**
     * Method that checks all trusted services every hour -> **:00:00
     */
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void checkActiveElections() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        for(Election election : electionsMap.values()) {
            if(now.isAfter(election.getDateFinish())) {
                electionsMap.remove(election.getUUID());
                em.persist(election.setState(Election.State.TERMINATED));
            }
            if(now.isBefore(election.getDateBegin())) {
                electionsMap.remove(election.getUUID());
            }
        }
    }

    public Election getElection(String electionUUID, String entityId) throws NotFoundException {
        if(electionsMap.containsKey(electionUUID)) {
            Election election = electionsMap.get(electionUUID);
            if(election.getEntityId().equals(entityId))
                return election;
        }
        List<Election> electionList = em.createNamedQuery(Election.FIND_BY_UUID_AND_SYSTEM_ENTITY_ID)
                .setParameter("electionUUID", electionUUID).setParameter("entityId", entityId).getResultList();
        if(electionList.isEmpty())
            return fetchElectionFromEntity(electionUUID, entityId);
        else {
            Election election = electionList.iterator().next();
            electionsMap.put(election.getUUID(), election);
            return election;
        }
    }

    public Election fetchElectionFromEntity(String electionUUID, String entityId) throws NotFoundException {
        MetadataDto entityMetadata =  trustedServicesEJB.getEntity(entityId);
        if(entityMetadata == null)
            throw new NotFoundException("Entity: " + entityId + " isn't a trusted entity");
        try {
            ResponseDto response = HttpConn.getInstance().doPostRequest(electionUUID.getBytes(), null,
                    OperationType.FETCH_ELECTION.getUrl(entityId));
            if(ResponseDto.SC_OK == response.getStatusCode()) {
                byte[] requestSignedXML = response.getMessageBytes();
                ElectionDto electionDto = XML.getMapper().readValue(requestSignedXML, ElectionDto.class);
                SignatureParams signatureParams = new SignatureParams(null, User.Type.ENTITY,
                        SignedDocumentType.NEW_ELECTION_REQUEST).setWithTimeStampValidation(true);
                SignedDocument signedDocument = signatureService.validateXAdESAndSave(
                        new InMemoryDocument(requestSignedXML), signatureParams);
                Election election = new Election(electionDto, signedDocument).setEntityId(entityId);
                em.persist(election);
                electionsMap.put(election.getUUID(), election);
                return election;
            } else {
                log.log(Level.SEVERE, response.getMessage());
                throw new NotFoundException(Messages.currentInstance().get(
                        "fetchElectionFromEntityErrorMsg", electionUUID, entityId, response.getMessage()));
            }
        } catch (Exception ex) {
            throw new NotFoundException(ex.getMessage(), ex);
        }
    }

}
