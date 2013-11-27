package org.votingsystem.applet.validationtool.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.util.DateUtils;

import java.security.cert.X509Certificate;
import java.text.ParseException;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Formatter {
    
   private static Logger logger = Logger.getLogger(Formatter.class);

   private static String accessControlLbl = ContextVS.getInstance().getMessage("accessControlLbl");
   private static String nameLabel = ContextVS.getInstance().getMessage("nameLabel");
   private static String subjectLabel = ContextVS.getInstance().getMessage("subjectLabel");
   private static String contentLabel = ContextVS.getInstance().getMessage("contentLabel");
   private static String dateBeginLabel = ContextVS.getInstance().getMessage("dateBeginLabel");
   private static String dateFinishLabel = ContextVS.getInstance().getMessage("dateFinishLabel");
   private static String urlLabel = ContextVS.getInstance().getMessage("urlLabel");
    private static String hashAccessRequestBase64Label = ContextVS.getInstance().
              getMessage("hashAccessRequestBase64Label");
    private static String optionSelectedContentLabel = ContextVS.getInstance().
              getMessage("optionSelectedContentLabel");
    

    public static String getInfoCert(X509Certificate certificate) {
        return ContextVS.getInstance().getMessage("certInfoFormattedMsg",
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                DateUtils.getSpanishFormattedStringFromDate(
                        certificate.getNotBefore()),
                DateUtils.getSpanishFormattedStringFromDate(
                        certificate.getNotAfter()));
    }
    public static String procesar (String cadena) throws ParseException {
        if(cadena == null) {
            logger.debug(" - procesar null string");
            return null;
        }
        EventVS evento = null;
        String result = null;
        try {
            JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(cadena);
            evento = EventVS.populate(jsonObject);
            result = getEvento(evento);
        } catch(Exception ex) {
            logger.error("cadena: " + cadena + " - " + ex.getMessage(), ex);
        }
        return result;
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