package org.votingsystem.serviceprovider.cdi;

import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;
import org.votingsystem.qr.QRRequestBundle;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.flow.FlowScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Named("publishElection")
@FlowScoped("publish-election")
public class PublishElectionBean implements Serializable {

    private static final Logger log = Logger.getLogger(PublishElectionBean.class.getName());

    public static final int ELECTION_CONTENT_MAX_LENGTH = 2000;

    @Inject private Config config;
    @EJB private QRSessionsEJB qrSessions;

    private String electionContent;
    private String electionOptionsSeparator;
    private String electionDate;
    private String electionUUID;
    private String electionSubject;
    private String electionOptionsBase64;
    private String qrId;
    private String qrUUID;
    private int maxContentLength = ELECTION_CONTENT_MAX_LENGTH;
    private List<ElectionOption> optionList;

    public PublishElectionBean() {
        electionOptionsSeparator = UUID.randomUUID().toString().substring(0, 7);
    }


    public String publish() {
        try {
            HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            optionList = new ArrayList();

            String electionOptions = new String(Base64.getDecoder().decode(electionOptionsBase64));
            for(String electionOption : electionOptions.split(electionOptionsSeparator)){
                optionList.add(new ElectionOption(electionOption.replaceAll("\"", "'")));
            }
            //All elections begin at 00:00 GMT+00:00
            LocalDate localDate = LocalDate.parse(electionDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            ZonedDateTime zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
            LocalDateTime dateBegin = zonedDateBegin.toLocalDateTime();
            log.info("electionSubject: " + electionSubject + " - dateBegin: " + dateBegin);

            if(electionContent != null && electionContent.length() > maxContentLength) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        Messages.currentInstance().get("errorLbl"),
                        Messages.currentInstance().get("maxContentSizeExceededErrorMsg")));
                return "publish-election";
            }

            Election election = new Election(electionSubject, electionContent, dateBegin, dateBegin.plusDays(1),
                    Election.State.PENDING);
            electionUUID = UUID.randomUUID().toString();
            election.setUUID(electionUUID).setElectionOptions(new HashSet<>(optionList));

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
            return "publish-election-page2";
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    Messages.currentInstance().get("errorLbl"), ex.getMessage()));
            return null;
        }
    }

    public String getQRCodeURL() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String qrCodeURL = req.getContextPath() + "/api/qr?cht=qr&chs=200x200&chl=" +
                QRUtils.SYSTEM_ENTITY_KEY + "=" + config.getEntityId() + ";" +
                QRUtils.UUID_KEY + "=" + qrUUID + ";";
        return qrCodeURL;
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

    public String getQrId() {
        return qrId;
    }

    public void setQrId(String qrId) {
        this.qrId = qrId;
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

    public String getElectionOptionsSeparator() {
        return electionOptionsSeparator;
    }

    public void setElectionOptionsSeparator(String electionOptionsSeparator) {
        this.electionOptionsSeparator = electionOptionsSeparator;
    }

    public String getElectionOptionsBase64() {
        return electionOptionsBase64;
    }

    public void setElectionOptionsBase64(String electionOptionsBase64) {
        this.electionOptionsBase64 = electionOptionsBase64;
    }

    public List<ElectionOption> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<ElectionOption> optionList) {
        this.optionList = optionList;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

}