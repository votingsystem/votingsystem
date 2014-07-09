package org.votingsystem.client.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.util.DateUtils;

import java.security.cert.X509Certificate;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class Formatter {
    
    private static Logger logger = Logger.getLogger(Formatter.class);

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
                DateUtils.getLongDate_Es(
                        certificate.getNotBefore()),
                DateUtils.getLongDate_Es(
                        certificate.getNotAfter()));
    }

    public static String format(String textToFormat) {
        String result = null;
        try {
            JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(textToFormat);
            OperationVS operation = OperationVS.populate(jsonObject);
            switch(operation.getType()) {
                case SEND_SMIME_VOTE:
                    result = formatVote(jsonObject);
                    break;
                default:
                    logger.debug("Formatter nor found for "  + operation.getType());
                    result = textToFormat;
            }

        } catch(Exception ex) {
            logger.error("textToFormat: " + textToFormat + " - " + ex.getMessage(), ex);
        }
        return result;
    }
    
    private static String formatVote(JSONObject jsonObject){
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b><u><big>" + ContextVS.getMessage("voteLbl") +"</big></u></b><br/>");
        result.append("<b>" + ContextVS.getMessage("eventURLLbl") +": </b>");
        result.append("<a href='" + jsonObject.get("eventURL") + "'>" + jsonObject.get("eventURL") +"</a><br/>");
        JSONObject optionSelectedJSON = (JSONObject) jsonObject.get("optionSelected");
        result.append("<b>" + ContextVS.getMessage("optionSelectedLbl") +": </b>" + optionSelectedJSON.get("content"));
        result.append("</html>");
        return result.toString();
    }

    public static String getEvento (EventVS evento) {
        logger.debug("getEvento - evento: " + evento.getId());
        if (evento == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        if(evento.getAccessControlVS() != null) {
            result.append("<b>" + accessControlLbl + "</b>:").append(
                evento.getAccessControlVS().getName()).append("<br/>");
        }
        if(evento.getSubject() != null) result.append("<b>" + subjectLabel + "</b>: ").
                append(evento.getSubject() + "<br/>");
        if(evento.getContent() != null) result.append("<b>" + contentLabel + "</b>: ").
                append(evento.getContent() + "<br/>");
        if(evento.getDateBeginStr() != null) result.append("<b>" + dateBeginLabel + "</b>: ").
                append(evento.getDateBeginStr() + "<br/>");
        if(evento.getDateFinishStr() != null) result.append("<b>" + dateFinishLabel + "</b>: ").
                append(evento.getDateFinishStr() + "<br/>");
        if(evento.getUrl() != null) result.append("<b>" + urlLabel + "</b>: ").
                append(evento.getUrl() + "<br/>");
        if(evento.getVoteVS() != null) {
            if(evento.getVoteVS().getAccessRequestHashBase64() != null) result.append("<b>" +
                    hashAccessRequestBase64Label + "</b>: ").append(
                    evento.getVoteVS().getAccessRequestHashBase64() + "<br/>");
            if(evento.getVoteVS().getOptionSelected() != null) {
                result.append("<b>" +
                        optionSelectedContentLabel + "</b>: ").append(
                        evento.getVoteVS().getOptionSelected().getContent() + "<br/>");
            }
        }
        return result.toString();
    }
    
}