package org.votingsystem.serviceprovider.cdi;

import com.google.zxing.WriterException;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;
import org.votingsystem.qr.QRRequestBundle;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * Helper class to hold variables and methods needed by some web pages
 */
@SessionScoped
@ManagedBean(name="publishElection")
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class PublishElectionBean implements Serializable {

    private static final Logger log = Logger.getLogger(PublishElectionBean.class.getName());

    private static final String ELECTION_OPTIONS_SEPARATOR = "#@#@#@";

    @PersistenceContext
    private EntityManager em;
    @Inject private Config config;
    @EJB private QRSessionsEJB qrSessions;

    private String electionContent;
    private String electionDate;
    private String electionUUID;
    private String errorMsg;
    private String electionSubject;
    private String electionOptions;
    private String qrId;
    private String qrUUID;


    public String onload() {
        if(electionUUID != null) {
            List<Election> electionList = em.createQuery("select e from Election e where e.uuid=:electionUUID")
                    .setParameter("electionUUID", electionUUID).getResultList();
            if(!electionList.isEmpty()) {
                resetForm();
            }
        }
        return null;
    }

    public String publish() {
        errorMsg = null;
        try {
            HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            Set<ElectionOption> electionOptionSet = new HashSet<>();
            for(String electionOption : electionOptions.split(ELECTION_OPTIONS_SEPARATOR)){
                electionOptionSet.add(new ElectionOption(electionOption));
            }
            //All elections begin at 00:00 GMT+00:00
            LocalDate localDate = LocalDate.parse(electionDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            ZonedDateTime zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
            LocalDateTime dateBegin = zonedDateBegin.toLocalDateTime();
            log.info("electionSubject: " + electionSubject + " - dateBegin: " + dateBegin);

            Election election = new Election(electionSubject, electionContent, dateBegin, dateBegin.plusDays(1),
                    Election.State.PENDING);
            electionUUID = UUID.randomUUID().toString();
            election.setUUID(electionUUID).setElectionOptions(electionOptionSet);

            ElectionDto electionDto = new ElectionDto(election);
            electionDto.setEntityId(config.getEntityId());

            byte[] reqBytes = XML.getMapper().writeValueAsBytes(electionDto);
            log.info("reqBytes: " + new String(reqBytes));
            //XMLValidator.validatePublishElectionRequest(reqBytes);

            qrUUID = election.getUUID();
            qrId = qrUUID.substring(0, 4).toUpperCase();
            Set<String> qrSessionsSet = (Set<String>) req.getSession().getAttribute(Constants.QR_OPERATIONS);
            if(qrSessionsSet == null)
                qrSessionsSet = new HashSet<>();
            qrSessionsSet.add(qrUUID);
            qrSessions.putOperation(qrUUID, new QRRequestBundle<>(OperationType.PUBLISH_ELECTION, electionDto));
            return "/publish-election/publish.xhtml?faces-redirect=true&includeViewParams=true";
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            errorMsg = ex.getMessage();
            return "editor";
        }
    }

    public String getQRCodeURL() throws WriterException, IOException {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String qrCodeURL = req.getContextPath() + "/api/qr?cht=qr&chs=200x200&chl=" +
                QRUtils.SYSTEM_ENTITY_KEY + "=" + config.getEntityId() + ";" +
                QRUtils.UUID_KEY + "=" + qrUUID + ";";
        return qrCodeURL;
    }

    private void resetForm() {
        electionContent = null;
        electionSubject = null;
        electionDate = null;
        electionUUID = null;
        errorMsg = null;
        electionOptions = null;
        qrId = null;
        qrUUID = null;
    }

    public String getElectionContent() {
        return electionContent;
    }

    public void setElectionContent(String electionContent) {
        this.electionContent = electionContent;
    }

    public String getElectionDate() {
        return electionDate;
    }

    public void setElectionDate(String electionDate) {
        this.electionDate = electionDate;
    }

    public String getElectionSubject() {
        return electionSubject;
    }

    public void setElectionSubject(String electionSubject) {
        this.electionSubject = electionSubject;
    }

    public String getElectionOptions() {
        return electionOptions;
    }

    public void setElectionOptions(String electionOptions) {
        this.electionOptions = electionOptions;
    }

    public String getQrId() {
        return qrId;
    }

    public void setQrId(String qrId) {
        this.qrId = qrId;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getQrUUID() {
        return qrUUID;
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }

}