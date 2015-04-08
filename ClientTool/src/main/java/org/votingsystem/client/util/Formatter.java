package org.votingsystem.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.votingsystem.model.EventVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.OperationVS;
import org.votingsystem.util.TypeVS;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class Formatter {
    
    private static Logger log = Logger.getLogger(Formatter.class.getSimpleName());

    private static final int INDENT_FACTOR = 7;

    private static String accessControlLbl = ContextVS.getMessage("accessControlLbl");
    private static String nameLabel = ContextVS.getMessage("nameLabel");
    private static String subjectLabel = ContextVS.getMessage("subjectLabel");
    private static String contentLabel = ContextVS.getMessage("contentLabel");
    private static String dateBeginLabel = ContextVS.getMessage("dateBeginLabel");
    private static String dateFinishLabel = ContextVS.getMessage("dateFinishLabel");
    private static String urlLabel = ContextVS.getMessage("urlLabel");
    private static String hashAccessRequestBase64Label = ContextVS.getMessage("hashAccessRequestBase64Label");
    private static String optionSelectedContentLabel = ContextVS.getMessage("optionSelectedContentLabel");

    public static String getInfoCert(X509Certificate certificate) {
        return ContextVS.getMessage("certInfoFormattedMsg",
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                DateUtils.getDayWeekDateStr(certificate.getNotBefore()),
                DateUtils.getDayWeekDateStr(certificate.getNotAfter()));
    }

    public static String format(Map dataMap) {
        String result = null;
        try {
            TypeVS operation = TypeVS.valueOf((String) dataMap.get("operation"));
            switch(operation) {
                case SEND_SMIME_VOTE:
                    result = formatVote(dataMap);
                    break;
                case FROM_GROUP_TO_ALL_MEMBERS:
                    result = formatTransactionVSFromGroupToAllMembers(dataMap);
                    break;
                default:
                    log.info("Formatter not found for " + operation);
                    result =new ObjectMapper().configure(
                            SerializationFeature.INDENT_OUTPUT,true).writeValueAsString(dataMap);
            }

        } catch(Exception ex) {
            log.log(Level.SEVERE, "format - dataMap: " + dataMap + " - " + ex.getMessage(), ex);
        }
        return result;
    }
    
    private static String formatVote(Map dataMap) throws JsonProcessingException {
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b><u><big>" + ContextVS.getMessage("voteLbl") +"</big></u></b><br/>");
        result.append("<b>" + ContextVS.getMessage("eventURLLbl") +": </b>");
        result.append("<a href='" + dataMap.get("eventURL") + "'>" + dataMap.get("eventURL") +"</a><br/>");
        Map optionSelectedJSON = (Map) dataMap.get("optionSelected");
        result.append("<b>" + ContextVS.getMessage("optionSelectedLbl") +": </b>" + optionSelectedJSON.get("content"));
        result.append("</html>");
        return new ObjectMapper().writeValueAsString(result);
    }

    private static String formatTransactionVSFromGroupToAllMembers(Map dataMap){
        return dataMap.toString();
    }



    public static String getEvent (EventVS eventVS) {
        log.info("getEvent - eventVS: " + eventVS.getId());
        if (eventVS == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        String dateBegin = null;
        String dateFinish = null;
        if(eventVS.getDateBegin() != null) dateBegin = DateUtils.getDateStr(eventVS.getDateBegin());
        if(eventVS.getDateFinish() != null) dateFinish = DateUtils.getDateStr(eventVS.getDateFinish());


        if(eventVS.getAccessControlVS() != null) {
            result.append("<b>" + accessControlLbl + "</b>:").append(
                eventVS.getAccessControlVS().getName()).append("<br/>");
        }
        if(eventVS.getSubject() != null) result.append("<b>" + subjectLabel + "</b>: ").
                append(eventVS.getSubject() + "<br/>");
        if(eventVS.getContent() != null) result.append("<b>" + contentLabel + "</b>: ").
                append(eventVS.getContent() + "<br/>");
        if(dateBegin != null) result.append("<b>" + dateBeginLabel + "</b>: ").append(dateBegin + "<br/>");
        if(dateFinish!= null) result.append("<b>" + dateFinishLabel + "</b>: ").append(dateFinish + "<br/>");
        if(eventVS.getUrl() != null) result.append("<b>" + urlLabel + "</b>: ").
                append(eventVS.getUrl() + "<br/>");
        if(eventVS.getVoteVS() != null) {
            if(eventVS.getVoteVS().getHashCertVSBase64() != null) result.append("<b>" +
                    hashAccessRequestBase64Label + "</b>: ").append(
                    eventVS.getVoteVS().getHashAccessRequestBase64() + "<br/>");
            if(eventVS.getVoteVS().getOptionSelected() != null) {
                result.append("<b>" +
                        optionSelectedContentLabel + "</b>: ").append(
                        eventVS.getVoteVS().getOptionSelected().getContent() + "<br/>");
            }
        }
        return result.toString();
    }
    
}