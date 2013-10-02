package org.sistemavotacion.modelo;

import android.util.Log;

import org.bouncycastle2.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.HttpHelper;
import org.sistemavotacion.util.ServerPaths;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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
    private String URL;
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
    
    public void comprobarFechas(String accessControlURL) {
    	if(estado == null) return;
        Date fecha = DateUtils.getTodayDate();
        Estado estadoEnum = Estado.valueOf(estado);
        if(!(fecha.after(fechaInicio) 
        		&& fecha.before(fechaFin))){
        	if(estadoEnum == Estado.ACTIVO){
        		final String checkURL = ServerPaths.getURLCheckEvent(accessControlURL, eventoId);
                Runnable runnable = new Runnable() {
                    public void run() { 
                    	try {
							HttpHelper.getData(checkURL, null);
						} catch (Exception e) {
							e.printStackTrace();
						} 
                    }
                };
                new Thread(runnable).start();
        	} 
        }
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

    public Evento initVoteData() throws NoSuchAlgorithmException {
    	this.origenHashSolicitudAcceso = UUID.randomUUID().toString();
    	this.hashSolicitudAccesoBase64 = CMSUtils.obtenerHashBase64(
    			this.origenHashSolicitudAcceso, AppData.SIG_HASH);
    	this.origenHashCertificadoVoto = UUID.randomUUID().toString();
    	this.hashCertificadoVotoBase64 = CMSUtils.obtenerHashBase64(
    			this.origenHashCertificadoVoto, AppData.SIG_HASH);
    	return this;
    }
    
    public String getCancelVoteData() throws JSONException {
		JSONObject jsonObject = new JSONObject();
        jsonObject.put("eventURL", URL);
		jsonObject.put("origenHashCertificadoVoto", 
        		getOrigenHashCertificadoVoto());
		jsonObject.put("hashCertificadoVotoBase64", 
        		getHashCertificadoVotoBase64());
		jsonObject.put("origenHashSolicitudAcceso", 
        		getOrigenHashSolicitudAcceso());
		jsonObject.put("hashSolicitudAccesoBase64", 
        		getHashSolicitudAccesoBase64());
        return jsonObject.toString(); 
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
	    	Log.d(TAG + ".getMensajeEstado", "tiempoRestante: " + tiempoRestante);
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
	
    public JSONObject getVoteJSON() {
    	Log.d(TAG + ".getVoteJSON", "getVoteJSON");
        Map map = new HashMap();
        map.put("operation", Operation.Tipo.ENVIO_VOTO_SMIME.toString());
        map.put("eventoURL", URL);
        map.put("opcionSeleccionadaId", opcionSeleccionada.getId());
        map.put("opcionSeleccionadaContenido", opcionSeleccionada.getContenido());
        map.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(map);
    }
	
	public void setEstado(String estado) {
		this.estado = estado;
	}
 
	public String getEstado() {
		return this.estado;
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}
	
   public String getSignatureContentJSON() throws JSONException {
  		Log.d(TAG + ".getSignatureData(...)", "");
        Map<String, Object> map = new HashMap<String, Object>();
        if(controlAcceso != null) {
        	Map<String, Object> controlAccesoMap = new HashMap<String, Object>();
            controlAccesoMap.put("serverURL", controlAcceso.getServerURL());
            controlAccesoMap.put("nombre",controlAcceso.getNombre());
            map.put("controlAcceso", controlAccesoMap);
        }
        map.put("id", id);
        if(eventoId != null) map.put("eventoId", eventoId);
        else map.put("eventoId", id);
        map.put("asunto", asunto);
        map.put("contenido", contenido);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("URL", URL);
        if(Tipo.EVENTO_RECLAMACION == tipo) {
            map.put("operation", Operation.Tipo.FIRMA_RECLAMACION_SMIME);
        }
        JSONObject jsonObject = new JSONObject(map);
        if (campos != null) {
            JSONArray jsonArray = new JSONArray();
            for (CampoDeEvento campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("id", campo.getId());
                campoMap.put("contenido", campo.getContenido());
                campoMap.put("valor", campo.getValor());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        return jsonObject.toString();
    }
	
    public static Evento parse(String eventoStr) throws ParseException, JSONException  {
    	Log.d(TAG + ".parse(...)", eventoStr);
    	return parse(new JSONObject(eventoStr));
    }
    
    public JSONObject getAccessRequestJSON() {
    	Log.d(TAG + ".getAccessRequestJSON(...)", "getAccessRequestJSON");
        Map map = new HashMap();
        map.put("operation", Operation.Tipo.SOLICITUD_ACCESO.toString());
        if(eventoId != null) map.put("eventId", eventoId);
        else map.put("eventId", id);
        map.put("eventURL", URL);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);       
        return new JSONObject(map);
    }
    
    public static Evento parse(JSONObject eventoJSON) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        Evento evento = new Evento();
        if (eventoJSON.has("URL"))
            evento.setURL(eventoJSON.getString("URL"));
        if (eventoJSON.has("contenido"))
            evento.setContenido(eventoJSON.getString("contenido"));
        if (eventoJSON.has("asunto"))        
            evento.setAsunto(eventoJSON.getString("asunto"));
        if (eventoJSON.has("id")) {
        	evento.setId(eventoJSON.getLong("id"));
        	evento.setEventoId(eventoJSON.getLong("id"));
        }          
        if (eventoJSON.has("eventoId"))        
            evento.setEventoId(eventoJSON.getLong("eventoId"));
        if (eventoJSON.has("usuario")) {
            Usuario usuario = new Usuario();
            usuario.setNombreCompleto(eventoJSON.getString("usuario"));
            evento.setUsuario(usuario);
        }
        if (eventoJSON.has("numeroTotalFirmas"))        
            evento.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalFirmas"));
        if (eventoJSON.has("numeroTotalVotos"))        
            evento.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalVotos"));      
        if (eventoJSON.has("fechaCreacion"))
            evento.setDateCreated(DateUtils.getDateFromString(eventoJSON.getString("fechaCreacion")));
        if (eventoJSON.has("fechaInicio"))
            evento.setFechaInicio(DateUtils.getDateFromString(eventoJSON.getString("fechaInicio")));
        if (eventoJSON.has("fechaFin") && !eventoJSON.isNull("fechaFin"))
            evento.setFechaFin(DateUtils.getDateFromString(eventoJSON.getString("fechaFin")));
        evento.setFirmado(Boolean.FALSE);
        if (eventoJSON.has("controlAcceso") && 
                eventoJSON.getJSONObject("controlAcceso") != null) {
            jsonObject = eventoJSON.getJSONObject("controlAcceso");
            ControlAcceso controlAcceso = new ControlAcceso ();
            controlAcceso.setServerURL(jsonObject.getString("serverURL"));
            controlAcceso.setNombre(jsonObject.getString("nombre"));
            evento.setControlAcceso(controlAcceso);
        }
        if (eventoJSON.has("etiquetas") && 
                eventoJSON.getJSONArray("etiquetas") != null) {
            List<String> etiquetas = new ArrayList<String>();
            jsonArray = eventoJSON.getJSONArray("etiquetas");
            for (int i = 0; i< jsonArray.length(); i++) {
                etiquetas.add(jsonArray.getString(i));
            }
            evento.setEtiquetas(etiquetas.toArray(new String[jsonArray.length()]));
        }
        if (eventoJSON.has("campos")) {
            Set<CampoDeEvento> campos = new HashSet<CampoDeEvento>();
            jsonArray = eventoJSON.getJSONArray("campos");
             for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                CampoDeEvento campo = new CampoDeEvento();
                campo.setId(jsonObject.getLong("id"));
                campo.setContenido(jsonObject.getString("contenido"));
                campos.add(campo);
             }
            evento.setCampos(campos);
        }
        if (eventoJSON.has("opciones")) {
            Set<OpcionDeEvento> opciones = new HashSet<OpcionDeEvento>();
            jsonArray = eventoJSON.getJSONArray("opciones");
             for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OpcionDeEvento opcion = new OpcionDeEvento();
                opcion.setId(jsonObject.getLong("id"));
                opcion.setContenido(jsonObject.getString("contenido"));
                opciones.add(opcion);
             }
            evento.setOpciones(opciones);
        }
        if (eventoJSON.has("centroControl")) {
            jsonObject = eventoJSON.getJSONObject("centroControl");
            CentroControl centroControl = new CentroControl();
            if(jsonObject.has("id"))
            	centroControl.setId(jsonObject.getLong("id"));
            if(jsonObject.has("serverURL"))
            	centroControl.setServerURL(jsonObject.getString("serverURL"));
            if(jsonObject.has("nombre"))
            	centroControl.setNombre(jsonObject.getString("nombre"));
            evento.setCentroControl(centroControl);
        }
        if (eventoJSON.has("estado")) {
            evento.setEstado(eventoJSON.getString("estado"));
        }
        if (eventoJSON.has("hashSolicitudAccesoBase64")) {
            evento.setHashSolicitudAccesoBase64(eventoJSON.
            		getString("hashSolicitudAccesoBase64"));
        }
        if (eventoJSON.has("origenHashSolicitudAcceso")) {
            evento.setOrigenHashSolicitudAcceso(eventoJSON.
            		getString("origenHashSolicitudAcceso"));
        }
        if (eventoJSON.has("hashCertificadoVotoBase64")) {
            evento.setHashCertificadoVotoBase64(eventoJSON.
            		getString("hashCertificadoVotoBase64"));
        }
        if (eventoJSON.has("origenHashCertificadoVoto")) {
            evento.setOrigenHashCertificadoVoto(eventoJSON.
            		getString("origenHashCertificadoVoto"));
        }
        if (eventoJSON.has("opcionSeleccionada")) {
        	jsonObject = eventoJSON.getJSONObject("opcionSeleccionada");
        	OpcionDeEvento opcionSeleccionada = new OpcionDeEvento ();
        	opcionSeleccionada.setId(jsonObject.getLong("id"));
        	opcionSeleccionada.setContenido(jsonObject.getString("contenido"));
            evento.setOpcionSeleccionada(opcionSeleccionada);
        }
        return evento;
    }
    
    public JSONObject toJSON() throws JSONException{
    	Log.d(TAG + ".toJSON(...)", " - toJSON -");
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("asunto", asunto);
    	jsonObject.put("contenido", contenido);
    	if(fechaInicio != null)
    		jsonObject.put("fechaInicio", 
    				DateUtils.getStringFromDate(fechaInicio));
    	if(fechaFin != null)
    		jsonObject.put("fechaFin", 
    				DateUtils.getStringFromDate(fechaFin));
        if (tipo != null) jsonObject.put("tipoEvento", tipo.toString()); 
        if (eventoId != null) jsonObject.put("eventoId", eventoId); 
        if (etiquetas != null) {
            JSONArray jsonArray = new JSONArray();
            for (String etiqueta : etiquetas) {
                jsonArray.put(etiqueta);
            }
            jsonObject.put("etiquetas", jsonArray);
        }
        if (centroControl!= null) {
            Map<String, Object> centroControlMap = new HashMap<String, Object>(); 
            centroControlMap.put("id", centroControl.getId());
            centroControlMap.put("nombre", centroControl.getNombre());
            centroControlMap.put("serverURL",centroControl.getServerURL());
            JSONObject centroControlJSON = new JSONObject(centroControlMap);
            jsonObject.put("centroControl", centroControlJSON);
        }        
        if (opciones != null) {
            JSONArray jsonArray = new JSONArray();
            Map<String, Object> opcionMap = new HashMap<String, Object>(); 
            for (OpcionDeEvento opcion : opciones) {
            	opcionMap.put("id", opcion.getId());
            	opcionMap.put("contenido", opcion.getContenido());
            	JSONObject opcionJSON = new JSONObject(opcionMap);
            	jsonArray.put(opcionJSON);
            }
            jsonObject.put("opciones", jsonArray);
        }
        if (campos != null) {
            JSONArray jsonArray = new JSONArray();
            for (CampoDeEvento campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("contenido", campo.getContenido());
                campoMap.put("valor", campo.getValor());
                campoMap.put("id", campo.getId());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        if (cardinalidadDeOpciones != null) jsonObject.put(
        		"cardinalidad", cardinalidadDeOpciones.toString()); 
        if (opcionSeleccionada != null) {
            Map<String, Object> opcionSeleccionadaMap = new HashMap<String, Object>(); 
            opcionSeleccionadaMap.put("id", opcionSeleccionada.getId());
            opcionSeleccionadaMap.put("contenido", opcionSeleccionada.getContenido());
            JSONObject opcionSeleccionadaJSON = new JSONObject(opcionSeleccionadaMap);
            jsonObject.put("opcionSeleccionada", opcionSeleccionadaJSON);
        } 
        if(hashSolicitudAccesoBase64 != null)
        	jsonObject.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64); 
        if(origenHashSolicitudAcceso != null)
        	jsonObject.put("origenHashSolicitudAcceso", origenHashSolicitudAcceso); 
        if(hashCertificadoVotoBase64 != null)
        	jsonObject.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);        
        if(origenHashCertificadoVoto != null)
        	jsonObject.put("origenHashCertificadoVoto", origenHashCertificadoVoto);
        return jsonObject;    
    }

}
