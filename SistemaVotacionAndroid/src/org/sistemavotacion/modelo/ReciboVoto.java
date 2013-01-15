package org.sistemavotacion.modelo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.mail.MessagingException;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import android.util.Log;
import org.json.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class ReciboVoto {

    
    public static final String MENSAJE_ERROR_FIRMA_RECIBO = 
            "<html>Recibo de voto con errores.<br/>"
           + "<b>Desea notificar la incidencia</b></html>";
    public static final String MENSAJE_ERROR_OPCION = 
            "<html>La opción seleccionada del recibo no coincide con la que "
            + "usted ha enviado.<br/> ¿Desea anular el recibo?</html>";
    
    public static String obtenerMensajeVotoOk (Evento voto) {
        return "En el evento '" + voto.getAsunto() + "' usted ha elegido la opción '" 
                + voto.getOpcionSeleccionada().getContenido() + "'";
    }

    public static String obtenerMensajeVotoRepetido (Evento evento, String opcion) {
        return "Ya había enviado un voto para la convocatoria '" + evento.getAsunto() + 
                "' en el que había elegido la opción '" + opcion + "'";
    }
    
    public static String obtenerMensajeErrorVotoRepetido(String asunto, String usuario) {
        return "Voto anulado. Ya se había recibido un voto del usuario '" 
                + usuario + "' para el asunto '" + asunto + "'";
    }  

    private Long id;
    private int codigoEstado;
    private String mensaje;
    private String eventoURL;    
    private Long eventoVotacionId;
    private Long opcionSeleccionadaId;
    private Tipo tipo;
    private ActorConIP actorConIP;
    private String controlAccesoServerURL;    
    private boolean esValido = false;
    private SMIMEMessageWrapper smimeMessage;
    private Evento voto;
    
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
        JSONObject contenidoReciboJSON = new JSONObject(contenidoRecibo);
        this.opcionSeleccionadaId = contenidoReciboJSON.getLong("opcionSeleccionadaId");
        this.eventoURL = contenidoReciboJSON.getString("eventoURL");
        comprobarRecibo();
    }
    
    public boolean esValido () throws Exception {
        return esValido;
    }

    
    private void comprobarRecibo () throws Exception {
        if (409 == codigoEstado) {//voto repetido
            esValido = true; 
            mensaje = obtenerMensajeVotoRepetido(voto, 
                    voto.getContenidoOpcion(opcionSeleccionadaId));
            return;
        }
        if (!opcionSeleccionadaId.equals(voto.getOpcionSeleccionada().getId())) {
            Log.e("ReciboVoto", MENSAJE_ERROR_OPCION);
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
     * @return the voto
     */
    public Evento getVoto() {
        return voto;
    }

    /**
     * @param voto the voto to set
     */
    public void setVoto(Evento voto) {
        this.voto = voto;
    }
	
}