package org.sistemavotacion.controlacceso.modelo;

import java.io.File;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Respuesta implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    public static final int SC_OK                            = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_ERROR_PETICION                = 400;
    public static final int SC_NOT_FOUND                     = 404;
    public static final int SC_ERROR_VOTO_REPETIDO           = 470;
    public static final int SC_ANULACION_REPETIDA            = 471;
    public static final int SC_NULL_REQUEST                  = 472;

    public static final int SC_ERROR           = 500;
    public static final int SC_ERROR_TIMESTAMP = 570;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO  = 0;
    
    private int codigoEstado;
	private Tipo tipo = Tipo.OK; 
	private Date fecha;
	private String mensaje;
	private MensajeSMIME mensajeSMIME;
	private Evento evento;
	private Usuario usuario;
	private Set<Usuario> usuarios;
	private SMIMEMessageWrapper smimeMessage;
	private SolicitudAcceso solicitudAcceso;
	private AnuladorVoto anuladorVoto;
	private String asunto;
	private String hashCertificadoVotoBase64;
	private Voto voto;
	private File file;
    private X509Certificate certificado;
    private Certificado certificadoDB;
    private byte[] messageBytes;
    private ActorConIP actorConIP;
    private CentroControl centroControl;
    private SolicitudCSRVoto solicitudCSR;
    private Dispositivo dispositivo;
    private Documento documento;
    private Object data;

    
	public String getAsunto() {
		return asunto;
	}

	public void setAsunto(String asunto) {
		this.asunto = asunto;
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

	public Certificado getCertificadoDB() {
		return certificadoDB;
	}

	public void setCertificadoDB(Certificado certificadoDB) {
		this.certificadoDB = certificadoDB;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public Set<Usuario> getUsuarios() {
		return usuarios;
	}

	public void setUsuarios(Set<Usuario> usuarios) {
		this.usuarios = usuarios;
	}

	public CentroControl getCentroControl() {
		return centroControl;
	}

	public void setCentroControl(CentroControl centroControl) {
		this.centroControl = centroControl;
	}


	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}