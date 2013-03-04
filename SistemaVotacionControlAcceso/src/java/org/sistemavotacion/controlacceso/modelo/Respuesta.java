package org.sistemavotacion.controlacceso.modelo;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Respuesta {
    
    public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ANULACION_REPETIDA = 471;
    
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;
    
    private int codigoEstado;
    private int cantidad;
	private Tipo tipo = Tipo.ERROR; 
	private Date fecha;
	private String mensaje;
	private Map<String, Object> datos;
	private MensajeSMIME mensajeSMIME;
	private MensajeSMIME mensajeSMIMEValidado;
	private Evento evento;
	private Usuario usuario;
	private SMIMEMessageWrapper smimeMessage;
	private SolicitudAcceso solicitudAcceso;
	private AnuladorVoto anuladorVoto;
	private String asunto;
	private String hashCertificadoVotoBase64;
	private byte[] firmaCSR;
	private Voto voto;
	private File file;
    private X509Certificate certificado;
    private Certificado certificadoDB;
    private byte[] cadenaCertificacion;
    private byte[] timeStampToken;
    private ActorConIP actorConIP;
    private SolicitudCSRVoto solicitudCSR;
    private Dispositivo dispositivo;
    private Documento documento;
	
	public Map getMap() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("codigoEstado", getCodigoEstado());
		map.put("tipoRespuesta", getTipo().toString());
		map.put("datos", getDatos());
		//TODO
		if(getMensaje() == null) setMensaje(getTipo().getMensaje());
		map.put("mensaje", getMensaje());
		return map;
	}
	
	public Tipo getTipo () {
		if (tipo != null) return tipo;
		switch (getCodigoEstado()) {
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
		if(getMensajeSMIME() != null) {
			datos.put("Fecha", getMensajeSMIME().getDateCreated());
			datos.put("TipoDePeticion", getMensajeSMIME().getTipo().toString());
			datos.put("MensajeId", getMensajeSMIME().getId());
			if (getSmimeMessage() != null && getSmimeMessage().isValidSignature()) {
				switch(getMensajeSMIME().getTipo()) {
					case EVENTO_VOTACION:
						datos.put("ResultadoOperacion", "OK");
						break;
					case EVENTO_VOTACION_ERROR:
						datos.put("ResultadoOperacion", "ERROR");
						break;
				}
			} else datos.put("ResultadoOperacion", "ERROR");
		}
		if(getUsuario() != null) datos.put("Remitente", getUsuario().getNif());
		if(getSmimeMessage() != null) datos.put("MensajeRecibido", getSmimeMessage().getSignedContent());
		if (getEvento() != null) datos.put("eventoId", getEvento().getId());
		return datos;
	}

	public MensajeSMIME getMensajeSMIMEValidado() {
		return mensajeSMIMEValidado;
	}

	public void setMensajeSMIMEValidado(MensajeSMIME mensajeSMIMEValidado) {
		this.mensajeSMIMEValidado = mensajeSMIMEValidado;
	}

	public String getAsunto() {
		return asunto;
	}

	public void setAsunto(String asunto) {
		this.asunto = asunto;
	}

	public byte[] getFirmaCSR() {
		return firmaCSR;
	}

	public void setFirmaCSR(byte[] firmaCSR) {
		this.firmaCSR = firmaCSR;
	}

	public SolicitudAcceso getSolicitudAcceso() {
		return solicitudAcceso;
	}

	public void setSolicitudAcceso(SolicitudAcceso solicitudAcceso) {
		this.solicitudAcceso = solicitudAcceso;
	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public String getHashCertificadoVotoBase64() {
		return hashCertificadoVotoBase64;
	}

	public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
		this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
	}

	public Voto getVoto() {
		return voto;
	}

	public void setVoto(Voto voto) {
		this.voto = voto;
	}

	public X509Certificate getCertificado() {
		return certificado;
	}

	public void setCertificado(X509Certificate certificado) {
		this.certificado = certificado;
	}

	public byte[] getCadenaCertificacion() {
		return cadenaCertificacion;
	}

	public void setCadenaCertificacion(byte[] cadenaCertificacion) {
		this.cadenaCertificacion = cadenaCertificacion;
	}

	public ActorConIP getActorConIP() {
		return actorConIP;
	}

	public void setActorConIP(ActorConIP actorConIP) {
		this.actorConIP = actorConIP;
	}

	public MensajeSMIME getMensajeSMIME() {
		return mensajeSMIME;
	}

	public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
		this.mensajeSMIME = mensajeSMIME;
	}

	public Evento getEvento() {
		return evento;
	}

	public void setEvento(Evento evento) {
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

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public int getCodigoEstado() {
		return codigoEstado;
	}

	public void setCodigoEstado(int codigoEstado) {
		this.codigoEstado = codigoEstado;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public void setDatos(Map<String, Object> datos) {
		this.datos = datos;
	}

	public AnuladorVoto getAnuladorVoto() {
		return anuladorVoto;
	}

	public void setAnuladorVoto(AnuladorVoto anuladorVoto) {
		this.anuladorVoto = anuladorVoto;
	}

	public SolicitudCSRVoto getSolicitudCSR() {
		return solicitudCSR;
	}

	public void setSolicitudCSR(SolicitudCSRVoto solicitudCSR) {
		this.solicitudCSR = solicitudCSR;
	}

	public Dispositivo getDispositivo() {
		return dispositivo;
	}

	public void setDispositivo(Dispositivo dispositivo) {
		this.dispositivo = dispositivo;
	}

	public Documento getDocumento() {
		return documento;
	}

	public void setDocumento(Documento documento) {
		this.documento = documento;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public int getCantidad() {
		return cantidad;
	}

	public void setCantidad(int cantidad) {
		this.cantidad = cantidad;
	}

	public byte[] getTimeStampToken() {
		return timeStampToken;
	}

	public void setTimeStampToken(byte[] timeStampToken) {
		this.timeStampToken = timeStampToken;
	}

	public Certificado getCertificadoDB() {
		return certificadoDB;
	}

	public void setCertificadoDB(Certificado certificadoDB) {
		this.certificadoDB = certificadoDB;
	}

}