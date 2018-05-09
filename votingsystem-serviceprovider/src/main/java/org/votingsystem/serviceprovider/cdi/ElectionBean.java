package org.votingsystem.serviceprovider.cdi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.ResponseDto;
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
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Named("electionBean")
@RequestScoped
public class ElectionBean implements Serializable {

    private static final Logger log = Logger.getLogger(ElectionBean.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private Config config;
    @Inject private TrustedServicesEJB trustedServices;
    private Election election;
    private String action;
    private String electionUUID;
    private String selectedCountry;
    //param to set javascript sidebar level
    private String parentURL;


    public String onload() {
        HttpServletRequest req = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String electionIdStr = req.getParameter("id");
        electionUUID = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest()).getParameter("UUID");
        action = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest()).getParameter("action");
        if(electionIdStr != null) {
            election = em.find(Election.class, Long.valueOf(electionIdStr));
        } else if(electionUUID != null) {
            List<Election> elections = em.createQuery("select e from Election e where e.uuid=:electionUUID")
                    .setParameter("electionUUID", electionUUID).getResultList();
            if(!elections.isEmpty())
                election = elections.iterator().next();
            else
                election = null;
        }
        if(election == null)
            return "/election/open.xhtml";
        else {
            electionUUID = election.getUUID();
            switch (election.getState()) {
                case ACTIVE:
                    parentURL = req.getContextPath() + "/election/elections.xhtml?state=ACTIVE";
                    break;
                case CANCELED:
                case TERMINATED:
                    parentURL = req.getContextPath() + "/election/elections.xhtml?state=TERMINATED";
                    break;
                case PENDING:
                    parentURL = req.getContextPath() + "/election/elections.xhtml?state=PENDING";
                    break;
            }
            return null;
        }
    }

    public String initAuthentication() throws JsonProcessingException {
        log.info("selectedCountry: " + selectedCountry + " - electionUUID: " + electionUUID);
        Set<MetadataDto> identityProviders = trustedServices.getEntitySetByTypeAndCountryCode(
                SystemEntityType.ID_PROVIDER, selectedCountry);
        if(identityProviders.isEmpty()) {
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("responseDto",
                    new ResponseDto(ResponseDto.SC_ERROR, Messages.currentInstance().get("serviceUnavailableForCountryMsg"))
                    .setCaption(Messages.currentInstance().get("errorLbl")));
            return "/response.xhtml?faces-redirect=true";
        }
        MetadataDto metadata = identityProviders.iterator().next();
        switch (selectedCountry) {
            case "ES":
                IdentityRequestDto identityRequest = new IdentityRequestDto(OperationType.ANON_VOTE_CERT_REQUEST,
                        electionUUID, new SystemEntityDto(config.getEntityId(), SystemEntityType.VOTING_SERVICE_PROVIDER));
                String xmlInput = Base64.getEncoder().encodeToString(XML.getMapper().writeValueAsBytes(identityRequest));
                String message = Messages.currentInstance().get("connectingToIdProviderMsg");
                String formAction = OperationType.ELECTION_INIT_AUTHENTICATION.getUrl(metadata.getEntity().getId());
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("message", message);
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("formAction", formAction);
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("xmlInput", xmlInput);
                return "/redirectForm.xhtml?faces-redirect=true";
            default:
                return null;
        }
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getParentURL() {
        return parentURL;
    }

    public void setParentURL(String parentURL) {
        this.parentURL = parentURL;
    }

    public String getSelectedCountry() {
        return selectedCountry;
    }

    public void setSelectedCountry(String selectedCountry) {
        this.selectedCountry = selectedCountry;
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }
}
