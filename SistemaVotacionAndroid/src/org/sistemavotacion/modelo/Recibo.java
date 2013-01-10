package org.sistemavotacion.modelo;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class Recibo {

    private Long id;
    private Tipo tipoDePeticion;
    private Date fecha;
    private String mensajeId;
    private String eventoId;
    private String resultadoOperacion;
    private String remitente;
    private String mensajeRecibido;
    private MensajeMime mensajeMime;
	
    public void setMensajeRecibido(String mensajeRecibido) {
        this.mensajeRecibido = mensajeRecibido;
    }
    public String getMensajeRecibido() {
        return mensajeRecibido;
    }
    private void setTipoDePeticion(Tipo tipoDePeticion) {
        this.tipoDePeticion = tipoDePeticion;
    }
    private Tipo getTipoDePeticion() {
        return tipoDePeticion;
    }
    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
    public Date getFecha() {
        return fecha;
    }
    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }
    public String getMensajeId() {
        return mensajeId;
    }
    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }
    public String getEventoId() {
        return eventoId;
    }
    public void setResultadoOperacion(String resultadoOperacion) {
        this.resultadoOperacion = resultadoOperacion;
    }
    public String getResultadoOperacion() {
        return resultadoOperacion;
    }
    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }
    public String getRemitente() {
        return remitente;
    }
    public void setMensajeMime(MensajeMime mensajeMime) {
        this.mensajeMime = mensajeMime;
    }
    public MensajeMime getMensajeMime() {
        return mensajeMime;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
	
}
