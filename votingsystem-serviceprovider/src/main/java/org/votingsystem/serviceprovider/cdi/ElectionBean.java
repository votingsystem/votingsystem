package org.votingsystem.serviceprovider.cdi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.model.voting.Election;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@Named("electionBean")
@RequestScoped
public class ElectionBean implements Serializable {

    private static final Logger log = Logger.getLogger(ElectionBean.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private TrustedServicesEJB trustedServices;
    @Inject private Config config;

    private String electionUUID;
    private Election election;

    public Election loadElection() {
        log.info("electionUUID: " + electionUUID);
        List<Election> elections = em.createQuery("select e from Election e JOIN FETCH e.publisher where e.uuid=:electionUUID")
                .setParameter("electionUUID", electionUUID).getResultList();
        if(!elections.isEmpty())
            election = elections.iterator().next();
        return election;
    }

    public String initAuthentication() throws JsonProcessingException {
        MetadataDto metadata = trustedServices.getFirstEntity(SystemEntityType.ID_PROVIDER);
        IdentityRequestDto identityRequest = new IdentityRequestDto(OperationType.ANON_VOTE_CERT_REQUEST,
                electionUUID, new SystemEntityDto(config.getEntityId(), SystemEntityType.VOTING_SERVICE_PROVIDER));
        String xmlInput = Base64.getEncoder().encodeToString(XML.getMapper().writeValueAsBytes(identityRequest));
        String message = Messages.currentInstance().get("connectingToIdProviderMsg");
        String formAction = OperationType.ELECTION_INIT_AUTHENTICATION.getUrl(metadata.getEntity().getId());
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("message", message);
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("formAction", formAction);
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("xmlInput", xmlInput);
        return "/redirectForm.xhtml?faces-redirect=true";
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

}
