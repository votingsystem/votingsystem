package org.sistemavotacion.centrocontrol.modelo;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class Respuesta {
    
    public static final int SC_OK = 200;
    
	public int codigoEstado;
	private Tipo tipo; 
	private Tipo tipoRespuestaSMIME = Tipo.INDEFINIDO;
	private Date fecha;
	private String mensaje;
	private Map<String, Object> datos;
	private MensajeSMIME mensajeSMIME;
	private MensajeSMIME mensajeSMIMEValidado;
	private EventoVotacion evento;
	private Usuario usuario;
	private SMIMEMessageWrapper smimeMessage;
	private String asunto;
	private Voto voto;
    private X509Certificate certificado;
    private byte[] cadenaCertificacion;
    private ActorConIP actorConIP;
	
	public Map getMap() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("codigoEstado", codigoEstado);
		map.put("tipoRespuesta", getTipo().toString());
		map.put("datos", getDatos());
		//TODO
		if(mensaje == null) mensaje = tipo.getMensaje();
		map.put("mensaje", mensaje);
		return map;
	}
	
	public Tipo getTipo () {
		if (tipo != null) return tipo;
		switch (codigoEstado) {
			case 200:
				tipo = Tipo.OK;
				break;
			case 201:
				tipo = Tipo.OK;
				break;
		}
		return tipo;
	}
	
	public Map<String, Object> getDatos () throws Exception {
		if (datos != null) return datos;
		Map<String, Object> datos = new HashMap<String, Object>();
		if(mensajeSMIME != null) {
			datos.put("Fecha", mensajeSMIME.getDateCreated());
			datos.put("TipoDePeticion", mensajeSMIME.getTipo().toString());
			datos.put("MensajeId", mensajeSMIME.getId());
			if (smimeMessage != null && smimeMessage.isValidSignature()) {
				switch(mensajeSMIME.getTipo()) {
					case EVENTO_VOTACION:
						datos.put("ResultadoOperacion", "OK");
						break;
					case EVENTO_VOTACION_ERROR:
						datos.put("ResultadoOperacion", "ERROR");
						break;
				}
			} else datos.put("ResultadoOperacion", "ERROR");
		}
		if(usuario != null) datos.put("Remitente", usuario.getNif());
		if(smimeMessage != null) datos.put("MensajeRecibido", smimeMessage.getSignedContent());
		if (evento != null) datos.put("eventoId", evento.getId());
		return datos;
	}

	public Voto getVoto() {
		return voto;
	}

	public void setVoto(Voto voto) {
		this.voto = voto;
	}

    /**
     * @return the certificado
     */
    public X509Certificate getCertificado() {
        return certificado;
    }

    /**
     * @param certificado the certificado to set
     */
    public void setCertificado(X509Certificate certificado) {
        this.certificado = certificado;
    }

    /**
     * @return the actorConIP
     */
    public ActorConIP getActorConIP() {
        return actorConIP;
    }

    /**
     * @param actorConIP the actorConIP to set
     */
    public void setActorConIP(ActorConIP actorConIP) {
        this.actorConIP = actorConIP;
    }

    /**
     * @return the cadenaCertificacion
     */
    public byte[] getCadenaCertificacion() {
        return cadenaCertificacion;
    }

    /**
     * @param cadenaCertificacion the cadenaCertificacion to set
     */
    public void setCadenaCertificacion(byte[] cadenaCertificacion) {
        this.cadenaCertificacion = cadenaCertificacion;
    }

    public String getMensaje() {
    	if(mensaje != null) return mensaje;
    	if(tipo != null) return tipo.toString();
		return null;
    }
}