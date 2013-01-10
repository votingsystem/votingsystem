package org.sistemavotacion.modelo;

import java.text.ParseException;
import java.util.Date;
import org.sistemavotacion.util.*;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class Estadisticas {
   
    private Long id;
    private Evento.Estado estado;
    private Tipo tipo;
    private Usuario usuario;
    private Integer numeroFirmasRecibidas;
    private Integer numeroSolicitudesDeAcceso;
    private Integer numeroVotosContabilizados;
    private String solicitudPublicacionValidadaURL;
    private String solicitudPublicacionURL;
    private String strFechaInicio;
    private String strFechaFin;
    private Date fechaInicio;
    private Date fechaFin;
    
    public void setEstado(Evento.Estado estado) {
        this.estado = estado;
    }
    public Evento.Estado getEstado() {
        return estado;
    }
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    public Usuario getUsuario() {
        return usuario;
    }
    public void setNumeroFirmasRecibidas(int numeroFirmasRecibidas) {
        this.numeroFirmasRecibidas = numeroFirmasRecibidas;
    }
    public Integer getNumeroFirmasRecibidas() {
        return numeroFirmasRecibidas;
    }
    public void setStrFechaInicio(String strFechaInicio) throws ParseException {
        this.strFechaInicio = strFechaInicio;
        fechaInicio = DateUtils.getDateFromString(strFechaInicio);
    }
    public String getStrFechaInicio() {
        return strFechaInicio;
    }
    public void setStrFechaFin(String strFechaFin) throws ParseException {
        this.strFechaFin = strFechaFin;
        fechaFin = DateUtils.getDateFromString(strFechaFin);
    }
    public String getStrFechaFin() {
        return strFechaFin;
    }
    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    public Date getFechaInicio() {
            return fechaInicio;
    }
    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }
    public Date getFechaFin() {
        return fechaFin;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public Tipo getTipo() {
        return tipo;
    }
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    /**
     * @return the numeroSolicitudesDeAcceso
     */
    public Integer getNumeroSolicitudesDeAcceso() {
        return numeroSolicitudesDeAcceso;
    }

    /**
     * @param numeroSolicitudesDeAcceso the numeroSolicitudesDeAcceso to set
     */
    public void setNumeroSolicitudesDeAcceso(Integer numeroSolicitudesDeAcceso) {
        this.numeroSolicitudesDeAcceso = numeroSolicitudesDeAcceso;
    }
	public String getSolicitudPublicacionValidadaURL() {
		return solicitudPublicacionValidadaURL;
	}
	public void setSolicitudPublicacionValidadaURL(
			String solicitudPublicacionValidadaURL) {
		this.solicitudPublicacionValidadaURL = solicitudPublicacionValidadaURL;
	}
	public String getSolicitudPublicacionURL() {
		return solicitudPublicacionURL;
	}
	public void setSolicitudPublicacionURL(String solicitudPublicacionURL) {
		this.solicitudPublicacionURL = solicitudPublicacionURL;
	}

    /**
     * @return the numeroVotosContabilizados
     */
    public Integer getNumeroVotosContabilizados() {
        return numeroVotosContabilizados;
    }

    /**
     * @param numeroVotosContabilizados the numeroVotosContabilizados to set
     */
    public void setNumeroVotosContabilizados(Integer numeroVotosContabilizados) {
        this.numeroVotosContabilizados = numeroVotosContabilizados;
    }

}
