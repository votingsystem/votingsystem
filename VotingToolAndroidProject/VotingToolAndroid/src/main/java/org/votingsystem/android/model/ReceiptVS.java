package org.votingsystem.android.model;

import org.votingsystem.model.TypeVS;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ReceiptVS {

    private Long id;
    private TypeVS typeVSDePeticion;
    private Date fecha;
    private String mensajeId;
    private String eventId;
    private String resultadoOperacion;
    private String remitente;
    private String mensajeRecibido;
	
    public void setMensajeRecibido(String mensajeRecibido) {
        this.mensajeRecibido = mensajeRecibido;
    }
    public String getMensajeRecibido() {
        return mensajeRecibido;
    }
    private void setTypeVSDePeticion(TypeVS typeVSDePeticion) {
        this.typeVSDePeticion = typeVSDePeticion;
    }
    private TypeVS getTypeVSDePeticion() {
        return typeVSDePeticion;
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
    public void setEventVSId(String eventId) {
        this.eventId = eventId;
    }
    public String getEventVSId() {
        return eventId;
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
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
	
}
