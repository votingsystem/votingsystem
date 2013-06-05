package org.sistemavotacion.centrocontrol.modelo;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Respuesta {
    
    public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ANULACION_REPETIDA  = 471;
    public static final int SC_NULL_REQUEST        = 472;
    
    public static final int SC_ERROR = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;
    
	public int codigoEstado;
	private Tipo tipo = Tipo.OK; 
	private Date fecha;
	private String mensaje;
	private MensajeSMIME mensajeSMIME;
	private EventoVotacion evento;
	private Usuario usuario;
	private Set<Usuario> usuarios;
	private SMIMEMessageWrapper smimeMessage;
	private String asunto;
	private Voto voto;
    private X509Certificate certificado;
    private byte[] cadenaCertificacion;
    private byte[] messageBytes;
    private ActorConIP actorConIP;

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

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public MensajeSMIME getMensajeSMIME() {
		return mensajeSMIME;
	}

	public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
		this.mensajeSMIME = mensajeSMIME;
	}

	public EventoVotacion getEvento() {
		return evento;
	}

	public void setEvento(EventoVotacion evento) {
		this.evento = evento;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public SMIMEMessageWrapper getSmimeMessage() {
		return smimeMessage;
	}

	public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
		this.smimeMessage = smimeMessage;
	}

	public String getAsunto() {
		return asunto;
	}

	public void setAsunto(String asunto) {
		this.asunto = asunto;
	}
}