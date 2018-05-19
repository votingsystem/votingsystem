package org.votingsystem.client.util;

import org.votingsystem.crypto.SignedFile;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionStatsDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class Formatter {
    
    private static Logger log = Logger.getLogger(Formatter.class.getName());

    private static final int INDENT_FACTOR = 7;

    private static String accessControlLbl = Messages.currentInstance().get("accessControlLbl");
    private static String nameLabel = Messages.currentInstance().get("nameLabel");
    private static String subjectLabel = Messages.currentInstance().get("subjectLabel");
    private static String contentLabel = Messages.currentInstance().get("contentLabel");
    private static String dateBeginLabel = Messages.currentInstance().get("dateBeginLabel");
    private static String dateFinishLabel = Messages.currentInstance().get("dateFinishLabel");
    private static String urlLabel = Messages.currentInstance().get("urlLabel");
    private static String hashAccessRequestBase64Label = Messages.currentInstance().get("hashAccessRequestBase64Label");
    private static String optionSelectedContentLabel = Messages.currentInstance().get("optionSelectedContentLabel");

    public static String getInfoCert(X509Certificate certificate) {
        return Messages.currentInstance().get("certInfoFormattedMsg",
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                DateUtils.getDateStr(certificate.getNotBefore()),
                DateUtils.getDateStr(certificate.getNotAfter()));
    }

    public static String format(SignedFile signedFile) {
        String result = null;
        try {
            switch(signedFile.getType()) {
                case VOTE:
                    result = formatVote(signedFile);
                    break;
                default:
                    log.info("Formatter not found for signed file: " + signedFile.getType());
                    result = new String(signedFile.getBody());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return result;
    }
    
    private static String formatVote(SignedFile signedFile) throws Exception {
        VoteDto voteDto = signedFile.getSignedDocument().getSignedContent(VoteDto.class);
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b><u><big>" + Messages.currentInstance().get("voteLbl") +"</big></u></b><br/>");
        result.append("<b>" + Messages.currentInstance().get("eventURLLbl") +": </b>");
        result.append("<a href='" + OperationType.FETCH_ELECTION.getUrl(voteDto.getElectionUUID()) + "'>" +
                OperationType.FETCH_ELECTION.getUrl(voteDto.getElectionUUID()) +"</a><br/>");
        result.append("<b>" + Messages.currentInstance().get("optionSelectedLbl") +": </b>" +
                voteDto.getOptionSelected().getContent());
        result.append("</html>");
        return new JSON().getMapper().writeValueAsString(result);
    }

    public static String formatElectionStats(ElectionStatsDto election) {
        String result = null;
        try {
            result = new XML().getMapper().writeValueAsString(election);
        }catch (Exception ex) {

        }
        return result;
    }

    public static String formatElection(ElectionDto election) {
        log.info("election: " + election.getUUID());
        if (election == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        String dateBegin = null;
        String dateFinish = null;
        if(election.getDateBegin() != null)
            dateBegin = DateUtils.getDateStr(election.getDateBegin());
        if(election.getDateFinish() != null)
            dateFinish = DateUtils.getDateStr(election.getDateFinish());
        if(election.getEntityId() != null) {
            result.append("<b>" + accessControlLbl + "</b>:").append(election.getEntityId()).append("<br/>");
        }
        if(election.getSubject() != null) result.append("<b>" + subjectLabel + "</b>: ").
                append(election.getSubject() + "<br/>");
        if(election.getContent() != null) result.append("<b>" + contentLabel + "</b>: ").
                append(election.getContent() + "<br/>");
        if(dateBegin != null)
            result.append("<b>" + dateBeginLabel + "</b>: ").append(dateBegin + "<br/>");
        if(dateFinish!= null)
            result.append("<b>" + dateFinishLabel + "</b>: ").append(dateFinish + "<br/>");
        return result.toString();
    }
    
}