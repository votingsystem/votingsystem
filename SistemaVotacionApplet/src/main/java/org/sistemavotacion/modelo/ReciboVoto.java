package org.sistemavotacion.modelo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.mail.MessagingException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ReciboVoto {
    
    private static Logger logger = LoggerFactory.getLogger(ReciboVoto.class);
    
    public static final String MENSAJE_ERROR_FIRMA_RECIBO = 
            "<html>Recibo de voto con errores.<br/>"
           + "<b>Desea notificar la incidencia</b></html>";
    public static final String MENSAJE_ERROR_OPCION = 
            "<html>La opción seleccionada del recibo no coincide con la que "
            + "usted ha enviado.<br/> ¿Desea anular el recibo?</html>";
    
    public static String obtenerMensajeVotoOk (Evento voto) {
        return "<html>En el asunto <b>" + voto.getAsunto() + "</b><br/>"
                + " usted ha elegido la opción <b>" 
                + voto.getOpcionSeleccionada().getContenido() + "</b></html>";
    }

    
    public static String obtenerMensajeErrorVotoRepetido(String asunto, String usuario) {
        return "<html><b>Voto anulado.</b><br/>Ya se había recibido un voto del usuario <b>" 
                + usuario + "</b> para el asunto <b>" + asunto + "</b>";
    }  

    private Long id;
    private int codigoEstado;
    private String mensaje;
    private String eventoURL;    
    private Long eventoVotacionId;
    private Long opcionSeleccionadaId;
    private Tipo tipo;
    private String controlAccesoServerURL;    
    private boolean esValido = false;
    private SMIMEMessageWrapper smimeMessage;
    private byte[] encryptedSMIMEMessage;
    private Evento voto;
    
    public ReciboVoto () {  }
    
    public ReciboVoto (int codigoEstado, String mensaje) { 
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
    
    public ReciboVoto (int codigoEstado, 
            SMIMEMessageWrapper votoValidado, Evento voto) throws Exception { 
        this.smimeMessage = votoValidado;
        this.codigoEstado = codigoEstado;
        this.voto = voto;
        String contenidoRecibo = votoValidado.getSignedContent();
        Object jsonData = JSONSerializer.toJSON(contenidoRecibo);
        if(jsonData instanceof JSONObject) {
            JSONObject contenidoReciboJSON = (JSONObject) JSONSerializer.toJSON(contenidoRecibo);
            this.opcionSeleccionadaId = contenidoReciboJSON.getLong("opcionSeleccionadaId");
            this.eventoURL = contenidoReciboJSON.getString("eventoURL");
            comprobarRecibo();	
        } else {
        	logger.debug("ERROR - contenido del recibo: " + contenidoRecibo);
        }
    }
    
    public ReciboVoto (int codigoEstado, SMIMEMessageWrapper votoValidado,
            String opcionSeleccionada) throws Exception { 
        this.smimeMessage = votoValidado;
        this.codigoEstado = codigoEstado;
        String contenidoRecibo = votoValidado.getSignedContent();
        JSONObject contenidoReciboJSON = (JSONObject) JSONSerializer.toJSON(contenidoRecibo);
        if (contenidoReciboJSON.containsKey("opcionSeleccionadaId")) 
            this.opcionSeleccionadaId = contenidoReciboJSON.getLong("opcionSeleccionadaId");
        else logger.debug("No se ha encontrado opcionSeleccionadaId opcionSeleccionadaId opcionSeleccionadaId");
        this.eventoURL = contenidoReciboJSON.getString("eventoURL");
        comprobarRecibo(opcionSeleccionada);
    }
    
    private void comprobarRecibo (String opcionSeleccionada) throws Exception {
        logger.debug("opcionSeleccionadaId.toString(): " + opcionSeleccionadaId.toString()
                + " - opcionSeleccionada: " + opcionSeleccionada);
        if (!opcionSeleccionadaId.toString().equals(opcionSeleccionada)) {
            logger.error(MENSAJE_ERROR_OPCION);
            esValido = false; 
            mensaje = MENSAJE_ERROR_OPCION;
            return;
        }
        if (smimeMessage.isValidSignature()) {
            esValido = true;
            mensaje = "seleccionado: " + opcionSeleccionada;
        } 
    }
    
    public void setVoto(Evento voto) {
        this.voto = voto;
    }
    
    public Evento getVoto() {
        return this.voto;
    }
    
    public boolean esValido () throws Exception {
        return esValido;
    }

    
    private void comprobarRecibo () throws Exception {
        if (!opcionSeleccionadaId.equals(voto.getOpcionSeleccionada().getId())) {
            logger.error(MENSAJE_ERROR_OPCION);
            esValido = false; 
            mensaje = MENSAJE_ERROR_OPCION;
            return;
        }
        if (smimeMessage.isValidSignature()) {
            esValido = true;
            mensaje = obtenerMensajeVotoOk(voto);
        } 
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }

    /**
     * @return the eventoVotacionId
     */
    public Long getEventoVotacionId() {
        return eventoVotacionId;
    }

    /**
     * @param eventoVotacionId the eventoVotacionId to set
     */
    public void setEventoVotacionId(Long eventoVotacionId) {
        this.eventoVotacionId = eventoVotacionId;
    }

    /**
     * @return the opcionSeleccionadaId
     */
    public Long getOpcionSeleccionadaId() {
        return opcionSeleccionadaId;
    }

    /**
     * @param opcionSeleccionadaId the opcionSeleccionadaId to set
     */
    public void setOpcionSeleccionadaId(Long opcionSeleccionadaId) {
        this.opcionSeleccionadaId = opcionSeleccionadaId;
    }

    /**
     * @return the archivoRecibo
     */
    public File getArchivoRecibo() throws IOException, MessagingException {
        File archivoRecibo = null;
        if (smimeMessage != null) {
            archivoRecibo = new File("recibo");
            smimeMessage.writeTo(new FileOutputStream(archivoRecibo));
        }
        return archivoRecibo;
    }

    /**
     * @return the controlAccesoServerURL
     */
    public String getControlAccesoServerURL() {
        return controlAccesoServerURL;
    }

    /**
     * @param controlAccesoServerURL the controlAccesoServerURL to set
     */
    public void setControlAccesoServerURL(String controlAccesoServerURL) {
        this.controlAccesoServerURL = controlAccesoServerURL;
    }

    /**
     * @return the tipo
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param tipo the tipo to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    /**
     * @return the codigoEstado
     */
    public int getCodigoEstado() {
        return codigoEstado;
    }

    /**
     * @param codigoEstado the codigoEstado to set
     */
    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    /**
     * @return the mensaje
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * @param mensaje the mensaje to set
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * @return the smimeMessage
     */
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

    /**
     * @param smimeMessage the smimeMessage to set
     */
    public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    /**
     * @return the eventoURL
     */
    public String getEventoURL() {
        return eventoURL;
    }

    /**
     * @param eventoURL the eventoURL to set
     */
    public void setEventoURL(String eventoURL) {
        this.eventoURL = eventoURL;
    }

    /**
     * @return the encryptedSMIMEMessage
     */
    public byte[] getEncryptedSMIMEMessage() {
        return encryptedSMIMEMessage;
    }

    /**
     * @param encryptedSMIMEMessage the encryptedSMIMEMessage to set
     */
    public void setEncryptedSMIMEMessage(byte[] encryptedSMIMEMessage) {
        this.encryptedSMIMEMessage = encryptedSMIMEMessage;
    }
	
}