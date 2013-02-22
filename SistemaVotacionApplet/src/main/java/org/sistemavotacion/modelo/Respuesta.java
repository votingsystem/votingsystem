package org.sistemavotacion.modelo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.PKIXParameters;
import java.util.Date;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailValidator;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class Respuesta<T> {
    
    private static Logger logger = LoggerFactory.getLogger(Respuesta.class);
    
    public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ANULACION_REPETIDA = 471;
    
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;

    public static final int SC_ERROR = 400;

    private int codigoEstado;
    private String mensaje;
    private Operacion operacion;
    private Object objeto;
    private T data;
    private Tipo tipo;
    private Date fecha;
    private Long eventoId;
    private SMIMEMessageWrapper smimeMessage;
    private ReciboVoto reciboVoto;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private byte[] bytesArchivo;
    private File archivo;
        
    public Respuesta () {  }
    
        
    public Respuesta (int codigoEstado) {
        this.codigoEstado = codigoEstado;    
    }
    
    public Respuesta (int codigoEstado, ReciboVoto reciboVoto) {
        this.codigoEstado = codigoEstado;
        this.reciboVoto = reciboVoto;
    }
    
    public Respuesta (int codigoEstado, String mensaje, Object objeto) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
        this.objeto = objeto;
    }

    public Respuesta (int codigoEstado, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
        
    public Respuesta (int codigoEstado, PKCS10WrapperClient pkcs10WrapperClient) {
        this.codigoEstado = codigoEstado;
        this.pkcs10WrapperClient = pkcs10WrapperClient;
    }
    
    public Respuesta (int codigoEstado, 
        SMIMEMessageWrapper recibo, PKIXParameters params) throws Exception {
        this.codigoEstado = codigoEstado;
        this.smimeMessage = recibo;
        SignedMailValidator.ValidationResult validationResult = recibo.verify(params);        
        if (validationResult.isValidSignature()) {
            JSONObject resultadoJSON = (JSONObject)JSONSerializer.toJSON(
                    recibo.getSignedContent());
            logger.debug("Respuesta - resultadoJSON: " + resultadoJSON.toString());
            if (resultadoJSON.containsKey("tipoRespuesta")) 
                tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.containsKey("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.containsKey("mensaje"))
                mensaje = resultadoJSON.getString("mensaje");
         } else logger.error(" Error en la validación de la respuesta");
    }
    
    public Respuesta (int codigoEstado, SMIMEMessageWrapper recibo) throws Exception {
        this.codigoEstado = codigoEstado;    
        this.smimeMessage = recibo;
        if (recibo.isValidSignature()) {
            JSONObject resultadoJSON = (JSONObject)JSONSerializer.toJSON(
                    recibo.getSignedContent());
            logger.debug("Respuesta - resultadoJSON: " + resultadoJSON.toString());
            if (resultadoJSON.containsKey("tipoRespuesta")) 
                tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.containsKey("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.containsKey("mensaje"))
                mensaje = resultadoJSON.getString("mensaje");
         } else logger.error(" Error en la validación de la respuesta");
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
    
    
    public String toString () {
    	StringBuilder respuesta = new StringBuilder();
    	respuesta.append("Estado: ").append(codigoEstado).append(" - Mensaje: ").append(mensaje);
    	if (tipo != null) {
    		respuesta.append(" - Tipo: ").append(tipo.toString());
    	}
    	return respuesta.toString();
    }

    /**
     * @return the codigoEstadoHTTP
     */
    public int getCodigoEstado() {
        return codigoEstado;
    }

    /**
     * @param codigoEstadoHTTP the codigoEstadoHTTP to set
     */
    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    /**
     * @return the mensajeDeError
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param mensajeDeError the mensajeDeError to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public void setFecha(Date fecha) {
            this.fecha = fecha;
    }

    public Date getFecha() {
            return fecha;
    }
    
    /**
     * @return the eventoId
     */
    public Long getEventoId() {
        return eventoId;
    }

    /**
     * @param eventoId the eventoId to set
     */
    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    /**
     * @return the objeto
     */
    public Object getObjeto() {
        return objeto;
    }

    /**
     * @param objeto the objeto to set
     */
    public void setObjeto(Object objeto) {
        this.objeto = objeto;
    }

    /**
     * @return the pkcs10WrapperClient
     */
    public PKCS10WrapperClient getPkcs10WrapperClient() {
        return pkcs10WrapperClient;
    }

    /**
     * @param pkcs10WrapperClient the pkcs10WrapperClient to set
     */
    public void setPkcs10WrapperClient(PKCS10WrapperClient pkcs10WrapperClient) {
        this.pkcs10WrapperClient = pkcs10WrapperClient;
    }

    /**
     * @return the bytesArchivo
     */
    public byte[] getBytesArchivo() {
        return bytesArchivo;
    }

    /**
     * @param bytesArchivo the bytesArchivo to set
     */
    public void setBytesArchivo(byte[] bytesArchivo) {
        this.bytesArchivo = bytesArchivo;
    }

    /**
     * @return the archivo
     */
    public File getArchivo() {
        return archivo;
    }

    /**
     * @param archivo the archivo to set
     */
    public void setArchivo(File archivo) {
        this.archivo = archivo;
    }

    /**
     * @return the reciboVoto
     */
    public ReciboVoto getReciboVoto() {
        return reciboVoto;
    }

    /**
     * @param reciboVoto the reciboVoto to set
     */
    public void setReciboVoto(ReciboVoto reciboVoto) {
        this.reciboVoto = reciboVoto;
    }

    /**
     * @return the operacion
     */
    public Operacion getOperacion() {
        return operacion;
    }

    /**
     * @param operacion the operacion to set
     */
    public void setOperacion(Operacion operacion) {
        this.operacion = operacion;
    }

    public String getContenidoRecibo() throws Exception {
        if(operacion == null || !operacion.isRespuestaConRecibo()
                || mensaje == null || "".equals(mensaje)) return null;
        return comprobarRecibo(mensaje.getBytes());
    }
        
    private String comprobarRecibo(byte[] bytesFirmados) throws Exception {
        SMIMEMessageWrapper smimeMessage = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(bytesFirmados), null);
        if (smimeMessage.isValidSignature()) {
            logger.debug("Firma valida - contenido mensaje: " + smimeMessage.getSignedContent());
            return smimeMessage.getSignedContent();
        } else {
            logger.debug("Firma con errores");
            return null;
        }  
    }

    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }
}