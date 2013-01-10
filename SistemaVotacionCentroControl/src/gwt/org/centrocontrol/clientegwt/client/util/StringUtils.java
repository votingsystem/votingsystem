package org.centrocontrol.clientegwt.client.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.centrocontrol.clientegwt.client.Constantes;
import org.centrocontrol.clientegwt.client.modelo.SistemaVotacionQueryString;
import org.centrocontrol.clientegwt.client.HistoryToken;
import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;

public class StringUtils {
	
    private static Logger logger = Logger.getLogger("StringUtils");

  	public static String truncateEventSubject(String subject) {
  		String result = "";
  		if(subject == null || "".equals(subject)) return "";
  		if(subject.length() < 20) return subject;
  		String[] words = subject.split(" ");
  		for(int i = 0; i < words.length; i++) {
  			if(words[i].length() >= 20) {
  				result = result + words[i].substring(0, 20) + " " + Constantes.INSTANCIA.continuaLabel();
  				return result;
  			} else result = result + " " + words[i];
  		}
  		if(result.length() >= 80) {
  			return result.substring(0, 80) + " " + Constantes.INSTANCIA.continuaLabel();
  		}
  		return result.trim();	
	}
  	
  	public static String partirTexto(String subject, int lineLength) {
  		String result = "";
  		if(subject == null || "".equals(subject)) return "";
  		if(subject.length() < lineLength) return subject;
  		String[] words = subject.split(" ");
  		for(int i = 0; i < words.length; i++) {
  			if(words[i].length() >= lineLength) {
  				String subcadena = words[i];
  				int length = subcadena.length();
  				while(subcadena.length() >= lineLength) {
  					result = result + subcadena.substring(0, lineLength) + "<br/>";
  					subcadena = subcadena.substring(lineLength, length);
  					length -= lineLength; 
  				}
  				result = result + subcadena;
  			} else {
  				result = result + " " + words[i];
  			} 
  		}
  		return result.trim();	
	}

	
  	public static SistemaVotacionQueryString getQueryString(String queryString) {
  		SistemaVotacionQueryString svQueryString = new SistemaVotacionQueryString();
  		svQueryString.setEventoId(getEventoId(queryString));
  		svQueryString.setEstadoEvento(getEstadoEvento(queryString));
  		svQueryString.setHistoryToken(getHistoryToken(queryString));
  		return svQueryString;
  	}
  	
  	private static Integer getEventoId(String query) {
  		Integer result = null;
  		if(query == null || "".equals(query)) return null;
  		String eventoId = null;
  		if(query.contains("&eventoId=")) {
  			eventoId = query.split("&eventoId=")[1];
  			if(eventoId.contains("&")) {
  				eventoId = eventoId.split("&")[0];
  			}
			try {
				result = Integer.valueOf(eventoId);
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				return null;
			}
  		}
  		return result;
  	}
  	
  	private static EventoSistemaVotacionJso.Estado getEstadoEvento(String query) {
  		EventoSistemaVotacionJso.Estado result = null;
  		if(query == null || "".equals(query)) return null;
  		String estado = null;
  		if(query.contains("&estadoEvento=")) {
  			estado = query.split("&estadoEvento=")[1];
  			if(estado.contains("&")) {
  				estado = estado.split("&")[0];
  			}
			try {
				result = EventoSistemaVotacionJso.Estado.valueOf(estado);
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				return null;
			}
  		}
  		return result;
  	}
  	
  	private static HistoryToken getHistoryToken(String query) {
  		HistoryToken result = null;
  		if(query == null || "".equals(query)) return null;
  		if(query.contains("&")) {
  			try {
				result = HistoryToken.valueOf(query.split("&")[0]);
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				return null;
			}
  		} else {
  			try {
				result = HistoryToken.valueOf(query.split("&")[0]);
			} catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				return null;
			}
  		}
  		return result;
  	}
  	
}
