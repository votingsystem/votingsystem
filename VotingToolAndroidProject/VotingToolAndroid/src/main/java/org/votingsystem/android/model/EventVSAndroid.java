package org.votingsystem.android.model;

import android.util.Log;

import org.bouncycastle2.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.model.CommentVS;
import org.votingsystem.model.EventTagVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.OptionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVSBase;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.util.DateUtils;

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
public class EventVSAndroid implements Serializable, EventVS {
	
	private static final long serialVersionUID = 1L;
	
	public static final String TAG = "EventVSAndroid";
    
    public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, PENDIENTE_COMIENZO,
    	PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}  

    public enum CardinalidadDeOpciones { MULTIPLES, UNA}
    
    public static final String MENSAJE_VOTACION_PENDIENTE = "Pendiente de abrir";
    public static final String MENSAJE_VOTACION_ABIERTA = "Quedan ";
    public static final String MENSAJE_VOTACION_CERRADA = "Cerrado";
    
    private Long id;
    private Long eventoId;
    private TypeVS typeVS;
    private CardinalidadDeOpciones cardinalidadDeOpciones;
    private String contenido;
    private String asunto;
    private String URL;
    private Integer numeroTotalFirmas;
    private Integer numeroTotalVotos;
    private Boolean firmado;
    private ControlCenter controlCenter;
    private UserVSBase userVSBase;
    private AccessControl accessControl;
    private Integer numeroComentarios = 0;

    private Set<OptionVS> opciones = new HashSet<OptionVS>(0);
    private Set<OptionVS> campos = new HashSet<OptionVS>(0);
    private Set<EventTagVS> eventTagVSes = new HashSet<EventTagVS>(0);
    private Set<AlmacenClaves> tokensAcceso = new HashSet<AlmacenClaves>(0);
    private Set<CommentVS> commentVSes = new HashSet<CommentVS>(0);

    private Date fechaInicio;
    private Date fechaFin;
    private Date dateCreated;
    private Date lastUpdated;

    private String origenHashCertificadoVoto;
    private String hashCertificadoVotoBase64;
    private String origenHashSolicitudAcceso;
    private String hashSolicitudAccesoBase64; 
    
    private String[] etiquetas;

    private OptionVS opcionSeleccionada;
    private String estado;

    public String getContenidoOpcion (Long opcionSeleccionada) {
        String resultado = null;
        for (OptionVS opcion : opciones) {
            if (opcionSeleccionada.equals(opcion.getId())) {
                resultado = opcion.getContent();
                break;
            }
        }        
        return resultado;
    } 
    
    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
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

    public String getSubject() { return asunto; }

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

    public void setTipoEleccion(CardinalidadDeOpciones cardinalidadDeOpciones) {
        this.cardinalidadDeOpciones = cardinalidadDeOpciones;
    }

    /**
     * @return the opciones
     */
    public Set<OptionVS> getOpciones() {
        return opciones;
    }

    /**
     * @param opciones the opciones to set
     */
    public void setOpciones(Set<OptionVS> opciones) {
        this.opciones = opciones;
    }

    public void setEventTagVSes(Set<EventTagVS> eventTagVSes) {
        this.eventTagVSes = eventTagVSes;
    }

    public Set<EventTagVS> getEventTagVSes() {
        return eventTagVSes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

	
    /**
     * @return the valido
     */
    public Boolean getFirmado() {
        return firmado;
    }


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

    public void setOpcionSeleccionada(OptionVS opcionSeleccionada) {
        this.opcionSeleccionada = opcionSeleccionada;
    }

    public OptionVS getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public void setCampos(Set<OptionVS> campos) {
        this.campos = campos;
    }

    public Set<OptionVS> getCampos() {
        return campos;
    }

    public void setCommentVSes(Set<CommentVS> commentVSes) {
        this.commentVSes = commentVSes;
    }

    public Set<CommentVS> getCommentVSes() {
        return commentVSes;
    }

    public void setNumeroComentarios(int numeroComentarios) {
        this.numeroComentarios = numeroComentarios;
    }

    public int getNumeroComentarios() {
        return numeroComentarios;
    }

            /**
     * @return the controlCenter
     */
    public ControlCenter getControlCenter() {
        return controlCenter;
    }

    /**
     * @param controlCenter the controlCenter to set
     */
    public void setControlCenter(ControlCenter controlCenter) {
        this.controlCenter = controlCenter;
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
     * @return the userVSBase
     */
    public UserVSBase getUserVSBase() {
        return userVSBase;
    }

    /**
     * @param userVSBase the userVSBase to set
     */
    public void setUserVSBase(UserVSBase userVSBase) {
        this.userVSBase = userVSBase;
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
     * @return the accessControl
     */
    public AccessControl getAccessControl() {
        return accessControl;
    }

    /**
     * @param accessControl the accessControl to set
     */
    public void setAccessControl(AccessControl accessControl) {
        this.accessControl = accessControl;
    }    
    
    public Estado getEstadoEnumValue () {
        if(estado == null) return null;
        else return Estado.valueOf(estado);
    }
    
    /*public void comprobarFechas(String accessControlURL) {
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
    }*/

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

    public EventVS initVoteData() throws NoSuchAlgorithmException {
    	this.origenHashSolicitudAcceso = UUID.randomUUID().toString();
    	this.hashSolicitudAccesoBase64 = CMSUtils.obtenerHashBase64(
    			this.origenHashSolicitudAcceso, ContextVSAndroid.SIG_HASH);
    	this.origenHashCertificadoVoto = UUID.randomUUID().toString();
    	this.hashCertificadoVotoBase64 = CMSUtils.obtenerHashBase64(
    			this.origenHashCertificadoVoto, ContextVSAndroid.SIG_HASH);
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
        map.put("operation", OperationVSAndroid.Tipo.SEND_SMIME_VOTE.toString());
        map.put("eventoURL", URL);
        map.put("opcionSeleccionadaId", opcionSeleccionada.getId());
        map.put("opcionSeleccionadaContenido", opcionSeleccionada.getContent());
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
        if(accessControl != null) {
        	Map<String, Object> controlAccesoMap = new HashMap<String, Object>();
            controlAccesoMap.put("serverURL", accessControl.getServerURL());
            controlAccesoMap.put("nombre", accessControl.getNombre());
            map.put("accessControl", controlAccesoMap);
        }
        map.put("id", id);
        if(eventoId != null) map.put("eventoId", eventoId);
        else map.put("eventoId", id);
        map.put("asunto", asunto);
        map.put("contenido", contenido);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("URL", URL);
        if(TypeVS.CLAIM_EVENT == typeVS) {
            map.put("operation", OperationVSAndroid.Tipo.SMIME_CLAIM_SIGNATURE);
        }
        JSONObject jsonObject = new JSONObject(map);
        if (campos != null) {
            JSONArray jsonArray = new JSONArray();
            for (OptionVS campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("id", campo.getId());
                campoMap.put("contenido", campo.getContent());
                campoMap.put("valor", campo.getValue());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        return jsonObject.toString();
    }
	
    public static EventVSAndroid parse(String eventoStr) throws ParseException, JSONException  {
    	Log.d(TAG + ".parse(...)", eventoStr);
    	return parse(new JSONObject(eventoStr));
    }
    
    public JSONObject getAccessRequestJSON() {
    	Log.d(TAG + ".getAccessRequestJSON(...)", "getAccessRequestJSON");
        Map map = new HashMap();
        map.put("operation", OperationVSAndroid.Tipo.ACCESS_REQUEST.toString());
        if(eventoId != null) map.put("eventId", eventoId);
        else map.put("eventId", id);
        map.put("eventURL", URL);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);       
        return new JSONObject(map);
    }
    
    public static EventVSAndroid parse(JSONObject eventoJSON) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        EventVSAndroid eventVSAndroid = new EventVSAndroid();
        if (eventoJSON.has("URL"))
            eventVSAndroid.setURL(eventoJSON.getString("URL"));
        if (eventoJSON.has("contenido"))
            eventVSAndroid.setContenido(eventoJSON.getString("contenido"));
        if (eventoJSON.has("asunto"))        
            eventVSAndroid.setAsunto(eventoJSON.getString("asunto"));
        if (eventoJSON.has("id")) {
        	eventVSAndroid.setId(eventoJSON.getLong("id"));
        	eventVSAndroid.setEventoId(eventoJSON.getLong("id"));
        }          
        if (eventoJSON.has("eventoId"))        
            eventVSAndroid.setEventoId(eventoJSON.getLong("eventoId"));
        if (eventoJSON.has("userVSBase")) {
            UserVSBase userVSBase = new UserVSBase();
            userVSBase.setNombreCompleto(eventoJSON.getString("userVSBase"));
            eventVSAndroid.setUserVSBase(userVSBase);
        }
        if (eventoJSON.has("numeroTotalFirmas"))        
            eventVSAndroid.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalFirmas"));
        if (eventoJSON.has("numeroTotalVotos"))        
            eventVSAndroid.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalVotos"));
        if (eventoJSON.has("fechaCreacion"))
            eventVSAndroid.setDateCreated(DateUtils.getDateFromString(eventoJSON.getString("fechaCreacion")));
        if (eventoJSON.has("fechaInicio"))
            eventVSAndroid.setFechaInicio(DateUtils.getDateFromString(eventoJSON.getString("fechaInicio")));
        if (eventoJSON.has("fechaFin") && !eventoJSON.isNull("fechaFin"))
            eventVSAndroid.setFechaFin(DateUtils.getDateFromString(eventoJSON.getString("fechaFin")));
        eventVSAndroid.setFirmado(Boolean.FALSE);
        if (eventoJSON.has("accessControl") &&
                eventoJSON.getJSONObject("accessControl") != null) {
            jsonObject = eventoJSON.getJSONObject("accessControl");
            AccessControl accessControl = new AccessControl();
            accessControl.setServerURL(jsonObject.getString("serverURL"));
            accessControl.setNombre(jsonObject.getString("nombre"));
            eventVSAndroid.setAccessControl(accessControl);
        }
        if (eventoJSON.has("etiquetas") && 
                eventoJSON.getJSONArray("etiquetas") != null) {
            List<String> etiquetas = new ArrayList<String>();
            jsonArray = eventoJSON.getJSONArray("etiquetas");
            for (int i = 0; i< jsonArray.length(); i++) {
                etiquetas.add(jsonArray.getString(i));
            }
            eventVSAndroid.setEtiquetas(etiquetas.toArray(new String[jsonArray.length()]));
        }
        if (eventoJSON.has("campos")) {
            Set<OptionVS> campos = new HashSet<OptionVS>();
            jsonArray = eventoJSON.getJSONArray("campos");
             for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OptionVS campo = new OptionVS();
                campo.setId(jsonObject.getLong("id"));
                campo.setContent(jsonObject.getString("contenido"));
                campos.add(campo);
             }
            eventVSAndroid.setCampos(campos);
        }
        if (eventoJSON.has("opciones")) {
            Set<OptionVS> opciones = new HashSet<OptionVS>();
            jsonArray = eventoJSON.getJSONArray("opciones");
             for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OptionVS opcion = new OptionVS();
                opcion.setId(jsonObject.getLong("id"));
                opcion.setContent(jsonObject.getString("contenido"));
                opciones.add(opcion);
             }
            eventVSAndroid.setOpciones(opciones);
        }
        if (eventoJSON.has("controlCenter")) {
            jsonObject = eventoJSON.getJSONObject("controlCenter");
            ControlCenter controlCenter = new ControlCenter();
            if(jsonObject.has("id"))
            	controlCenter.setId(jsonObject.getLong("id"));
            if(jsonObject.has("serverURL"))
            	controlCenter.setServerURL(jsonObject.getString("serverURL"));
            if(jsonObject.has("nombre"))
            	controlCenter.setNombre(jsonObject.getString("nombre"));
            eventVSAndroid.setControlCenter(controlCenter);
        }
        if (eventoJSON.has("estado")) {
            eventVSAndroid.setEstado(eventoJSON.getString("estado"));
        }
        if (eventoJSON.has("hashSolicitudAccesoBase64")) {
            eventVSAndroid.setHashSolicitudAccesoBase64(eventoJSON.
            		getString("hashSolicitudAccesoBase64"));
        }
        if (eventoJSON.has("origenHashSolicitudAcceso")) {
            eventVSAndroid.setOrigenHashSolicitudAcceso(eventoJSON.
            		getString("origenHashSolicitudAcceso"));
        }
        if (eventoJSON.has("hashCertificadoVotoBase64")) {
            eventVSAndroid.setHashCertificadoVotoBase64(eventoJSON.
            		getString("hashCertificadoVotoBase64"));
        }
        if (eventoJSON.has("origenHashCertificadoVoto")) {
            eventVSAndroid.setOrigenHashCertificadoVoto(eventoJSON.
            		getString("origenHashCertificadoVoto"));
        }
        if (eventoJSON.has("opcionSeleccionada")) {
        	jsonObject = eventoJSON.getJSONObject("opcionSeleccionada");
        	OptionVS opcionSeleccionada = new OptionVS();
        	opcionSeleccionada.setId(jsonObject.getLong("id"));
        	opcionSeleccionada.setContent(jsonObject.getString("contenido"));
            eventVSAndroid.setOpcionSeleccionada(opcionSeleccionada);
        }
        return eventVSAndroid;
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
        if (typeVS != null) jsonObject.put("tipoEvento", typeVS.toString());
        if (eventoId != null) jsonObject.put("eventoId", eventoId); 
        if (etiquetas != null) {
            JSONArray jsonArray = new JSONArray();
            for (String etiqueta : etiquetas) {
                jsonArray.put(etiqueta);
            }
            jsonObject.put("etiquetas", jsonArray);
        }
        if (controlCenter != null) {
            Map<String, Object> centroControlMap = new HashMap<String, Object>(); 
            centroControlMap.put("id", controlCenter.getId());
            centroControlMap.put("nombre", controlCenter.getNombre());
            centroControlMap.put("serverURL", controlCenter.getServerURL());
            JSONObject centroControlJSON = new JSONObject(centroControlMap);
            jsonObject.put("controlCenter", centroControlJSON);
        }        
        if (opciones != null) {
            JSONArray jsonArray = new JSONArray();
            Map<String, Object> opcionMap = new HashMap<String, Object>(); 
            for (OptionVS opcion : opciones) {
            	opcionMap.put("id", opcion.getId());
            	opcionMap.put("contenido", opcion.getContent());
            	JSONObject opcionJSON = new JSONObject(opcionMap);
            	jsonArray.put(opcionJSON);
            }
            jsonObject.put("opciones", jsonArray);
        }
        if (campos != null) {
            JSONArray jsonArray = new JSONArray();
            for (OptionVS campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("contenido", campo.getContent());
                campoMap.put("valor", campo.getValue());
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
            opcionSeleccionadaMap.put("contenido", opcionSeleccionada.getContent());
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
