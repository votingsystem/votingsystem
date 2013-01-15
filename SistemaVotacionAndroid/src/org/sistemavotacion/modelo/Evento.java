package org.sistemavotacion.modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.HttpHelper;
import org.sistemavotacion.util.ServerPaths;
import org.bouncycastle2.util.encoders.Hex;

import android.util.Log;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Evento implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final String TAG = "Evento";
    
    public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, PENDIENTE_COMIENZO,
    	PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}  

    public enum CardinalidadDeOpciones { MULTIPLES, UNA}
    
    public static final String MENSAJE_VOTACION_PENDIENTE = "Pendiente de abrir";
    public static final String MENSAJE_VOTACION_ABIERTA = "Quedan ";
    public static final String MENSAJE_VOTACION_CERRADA = "Cerrado";
    
    private Long id;
    private Long eventoId;
    private Tipo tipo;
    private CardinalidadDeOpciones cardinalidadDeOpciones;
    private String contenido;
    private String asunto;
    private Integer numeroTotalFirmas;
    private Integer numeroTotalVotos;
    private String controlAccesoServerURL;    
    private Boolean firmado;
    private MensajeMime mensajeMime;
    private CentroControl centroControl;
    private Usuario usuario;
    private ControlAcceso controlAcceso;
    private Integer numeroComentarios = 0;

    private Set<OpcionDeEvento> opciones = new HashSet<OpcionDeEvento>(0);
    private Set<CampoDeEvento> campos = new HashSet<CampoDeEvento>(0);
    private Set<EventoEtiqueta> eventoEtiquetas = new HashSet<EventoEtiqueta>(0);
    private Set<AlmacenClaves> tokensAcceso = new HashSet<AlmacenClaves>(0);     
    private Set<ConsultaVoto> consultasDeVoto = new HashSet<ConsultaVoto>(0);    
    private Set<Comentario> comentarios = new HashSet<Comentario>(0);    

    private Date fechaInicio;
    private Date fechaFin;
    private Date dateCreated;
    private Date lastUpdated;

    private String origenHashCertificadoVoto;
    private String hashCertificadoVotoBase64;
    private String origenHashSolicitudAcceso;
    private String hashSolicitudAccesoBase64; 
    
    private String[] etiquetas;

    private OpcionDeEvento opcionSeleccionada;
    private String estado;

    public String getContenidoOpcion (Long opcionSeleccionada) {
        String resultado = null;
        for (OpcionDeEvento opcion : opciones) {
            if (opcionSeleccionada.equals(opcion.getId())) {
                resultado = opcion.getContenido();
                break;
            }
        }        
        return resultado;
    } 
    
    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getContenido () {
        return contenido;
    }

    public void setContenido (String contenido) {
        this.contenido = contenido;
    }

    public String getAsunto () {
        return asunto;
    }

    public void setAsunto (String asunto) {
        this.asunto = asunto;
    }

    /**
     * @return the tipoEleccion
     */
    public CardinalidadDeOpciones getTipoEleccion() {
        return cardinalidadDeOpciones;
    }

        /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @param tipoEleccion the tipoEleccion to set
     */
    public void setTipoEleccion(CardinalidadDeOpciones cardinalidadDeOpciones) {
        this.cardinalidadDeOpciones = cardinalidadDeOpciones;
    }

    /**
     * @return the opciones
     */
    public Set<OpcionDeEvento> getOpciones() {
        return opciones;
    }

    /**
     * @param opciones the opciones to set
     */
    public void setOpciones(Set<OpcionDeEvento> opciones) {
        this.opciones = opciones;
    }

    public void setEventoEtiquetas(Set<EventoEtiqueta> eventoEtiquetas) {
        this.eventoEtiquetas = eventoEtiquetas;
    }

    public Set<EventoEtiqueta> getEventoEtiquetas() {
        return eventoEtiquetas;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setMensajeMime(MensajeMime mensajeMime) {
        this.mensajeMime = mensajeMime;
    }

    public MensajeMime getMensajeMime() {
        return mensajeMime;
    }
	
    /**
     * @return the valido
     */
    public Boolean getFirmado() {
        return firmado;
    }

    /**
     * @param valido the valido to set
     */
    public void setFirmado(Boolean firmado) {
        this.firmado = firmado;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    /**
     * @return the etiquetas
     */
    public String[] getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(String[] etiquetas) {
        if (etiquetas.length == 0) return;
        ArrayList<String> arrayEtiquetas = new ArrayList<String>();
        for (String etiqueta:etiquetas) {
            arrayEtiquetas.add(etiqueta.toLowerCase());
        }
        this.etiquetas = arrayEtiquetas.toArray(etiquetas);
    }

    public void setOpcionSeleccionada(OpcionDeEvento opcionSeleccionada) {
        this.opcionSeleccionada = opcionSeleccionada;
    }

    public OpcionDeEvento getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public void setCampos(Set<CampoDeEvento> campos) {
        this.campos = campos;
    }

    public Set<CampoDeEvento> getCampos() {
        return campos;
    }

    public void setComentarios(Set<Comentario> comentarios) {
        this.comentarios = comentarios;
    }

    public Set<Comentario> getComentarios() {
        return comentarios;
    }

    public void setNumeroComentarios(int numeroComentarios) {
        this.numeroComentarios = numeroComentarios;
    }

    public int getNumeroComentarios() {
        return numeroComentarios;
    }

            /**
     * @return the centroControl
     */
    public CentroControl getCentroControl() {
        return centroControl;
    }

    /**
     * @param centroControl the centroControl to set
     */
    public void setCentroControl(CentroControl centroControl) {
        this.centroControl = centroControl;
    }
    
    /**
     * @return the numeroTotalFirmas
     */
    public Integer getNumeroTotalFirmas() {
        return numeroTotalFirmas;
    }

    /**
     * @param numeroTotalFirmas the numeroTotalFirmas to set
     */
    public void setNumeroTotalFirmas(Integer numeroTotalFirmas) {
        this.numeroTotalFirmas = numeroTotalFirmas;
    }

    /**
     * @return the numeroTotalVotos
     */
    public Integer getNumeroTotalVotos() {
        return numeroTotalVotos;
    }

    /**
     * @param numeroTotalVotos the numeroTotalVotos to set
     */
    public void setNumeroTotalVotos(Integer numeroTotalVotos) {
        this.numeroTotalVotos = numeroTotalVotos;
    }

    /**
     * @return the usuario
     */
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * @param usuario the usuario to set
     */
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * @return the fechaInicio
     */
    public Date getFechaInicio() {
        return fechaInicio;
    }

    /**
     * @param fechaInicio the fechaInicio to set
     */
    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    /**
     * @return the fechaFin
     */
    public Date getFechaFin() {
        return fechaFin;
    }

    /**
     * @param fechaFin the fechaFin to set
     */
    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    /**
     * @return the tokensAcceso
     */
    public Set<AlmacenClaves> getTokensAcceso() {
        return tokensAcceso;
    }

    /**
     * @param tokensAcceso the tokensAcceso to set
     */
    public void setTokensAcceso(Set<AlmacenClaves> tokensAcceso) {
        this.tokensAcceso = tokensAcceso;
    }

    /**
     * @return the consultasDeVoto
     */
    public Set<ConsultaVoto> getConsultasDeVoto() {
        return consultasDeVoto;
    }

    /**
     * @param consultasDeVoto the consultasDeVoto to set
     */
    public void setConsultasDeVoto(Set<ConsultaVoto> consultasDeVoto) {
        this.consultasDeVoto = consultasDeVoto;
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
     * @return the controlAcceso
     */
    public ControlAcceso getControlAcceso() {
        return controlAcceso;
    }

    /**
     * @param controlAcceso the controlAcceso to set
     */
    public void setControlAcceso(ControlAcceso controlAcceso) {
        this.controlAcceso = controlAcceso;
    }    
    
    public Estado getEstadoEnumValue () {
        if(estado == null) return null;
        else return Estado.valueOf(estado);
    }
    
    public void comprobarFechas() {
    	if(estado == null) return;
        Date fecha = DateUtils.getTodayDate();
        Estado estadoEnum = Estado.valueOf(estado);
        if(!(fecha.after(fechaInicio) 
        		&& fecha.before(fechaFin))){
        	if(estadoEnum == Estado.ACTIVO){
        		final String checkURL = ServerPaths.getURLCheckEvent(
        				Aplicacion.CONTROL_ACCESO_URL, eventoId);
                Runnable runnable = new Runnable() {
                    public void run() { 
                    	try {
							HttpHelper.getFile(checkURL);
						} catch (Exception e) {
							e.printStackTrace();
						} 
                    }
                };
                new Thread(runnable).start();
        	} 
        }
    }
    
    public void comprobarFechasJJGZ() {
		final String checkURL = ServerPaths.getURLCheckEvent(
		Aplicacion.CONTROL_ACCESO_URL, eventoId);
        Runnable runnable = new Runnable() {
            public void run() { 
            	try {
					HttpHelper.getFile(checkURL);
				} catch (Exception e) {
					e.printStackTrace();
				} 
            }
        };
        new Thread(runnable).start();
    }

    /**
     * @return the origenHashCertificadoVoto
     */
    public String getOrigenHashCertificadoVoto() {
        return origenHashCertificadoVoto;
    }

    /**
     * @param origenHashCertificadoVoto the origenHashCertificadoVoto to set
     */
    public void setOrigenHashCertificadoVoto(String origenHashCertificadoVoto) {
        this.origenHashCertificadoVoto = origenHashCertificadoVoto;
    }

    /**
     * @return the hashCertificadoVotoBase64
     */
    public String getHashCertificadoVotoBase64() {
        return hashCertificadoVotoBase64;
    }

    /**
     * @return the hashCertificadoVotoBase64
     */
    public String getHashCertificadoVotoHex() {
        if (hashCertificadoVotoBase64 == null) return null;
        return new String(Hex.encode(hashCertificadoVotoBase64.getBytes()));
    }
    
    
    /**
     * @param hashCertificadoVotoBase64 the hashCertificadoVotoBase64 to set
     */
    public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
        this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
    }

    /**
     * @return the origenHashSolicitudAcceso
     */
    public String getOrigenHashSolicitudAcceso() {
        return origenHashSolicitudAcceso;
    }

    /**
     * @param origenHashSolicitudAcceso the origenHashSolicitudAcceso to set
     */
    public void setOrigenHashSolicitudAcceso(String origenHashSolicitudAcceso) {
        this.origenHashSolicitudAcceso = origenHashSolicitudAcceso;
    }

    /**
     * @return the hashSolicitudAccesoBase64
     */
    public String getHashSolicitudAccesoBase64() {
        return hashSolicitudAccesoBase64;
    }

    /**
     * @param hashSolicitudAccesoBase64 the hashSolicitudAccesoBase64 to set
     */
    public void setHashSolicitudAccesoBase64(String hashSolicitudAccesoBase64) {
        this.hashSolicitudAccesoBase64 = hashSolicitudAccesoBase64;
    }
    
	public boolean estaAbierto() {
		Date todayDate = DateUtils.getTodayDate();
		if (todayDate.after(fechaInicio) && todayDate.before(fechaFin)) return true;
		else return false;
	}
	
	public String getMensajeEstado () {
		Date todayDate = DateUtils.getTodayDate();
		String mensaje = null;
		if (todayDate.before(fechaInicio)) {
			mensaje = MENSAJE_VOTACION_PENDIENTE;
		} else if (todayDate.after(fechaInicio) && todayDate.before(fechaFin)) {
			long tiempoRestante = fechaFin.getTime() - todayDate.getTime(); 
	    	Log.d(TAG + ".getMensajeTiempoRestante", "tiempoRestante: " + tiempoRestante);
	        long diff = fechaFin.getTime() - todayDate.getTime();

	        long secondInMillis = 1000;
	        long minuteInMillis = secondInMillis * 60;
	        long hourInMillis = minuteInMillis * 60;
	        long dayInMillis = hourInMillis * 24;
	        long yearInMillis = dayInMillis * 365;

	        long elapsedDays = diff / dayInMillis;
	        diff = diff % dayInMillis;
	        long elapsedHours = diff / hourInMillis;
	        diff = diff % hourInMillis;
	        long elapsedMinutes = diff / hourInMillis;
	        diff = diff % hourInMillis;

	        StringBuilder duracion = new StringBuilder(MENSAJE_VOTACION_ABIERTA);
	        if (elapsedDays > 0) duracion.append(elapsedDays + " días");
	        else if (elapsedHours > 0) duracion.append(elapsedHours + " horas");
	        else if (elapsedMinutes > 0) duracion.append(elapsedMinutes + " minutos");
	        mensaje = duracion.toString();
		} else if (todayDate.after(fechaFin)) {
			mensaje = MENSAJE_VOTACION_CERRADA;
		}
		return mensaje;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}
 
	public String getEstado() {
		return this.estado;
	}
}
