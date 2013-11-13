package org.votingsystem.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.votingsystem.util.DateUtils;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

public class EventVSBase implements EventVS {

	private static Logger logger = Logger.getLogger(EventVSBase.class);

	public enum CardinalidadDeOpciones { MULTIPLES, UNA}
	
	public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, 
                PENDIENTE_COMIENZO, PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}
	
	public static final String VOTING_DATA_DIGEST = "SHA256";
	
	private Long id; //id of the event in the server
	private Long eventoId;//id of the event in the Access Control
	private OptionVS opcionSeleccionada;
	private Estado estado;
	private Estado nextState;
	private List<OptionVS> opciones;
	private String asunto;
	private String contenido;
	private UserVS userVS;
	private Date fechaInicio;
	private String fechaInicioStr;
	private Date fechaFin;
	private String fechaFinStr;
	private Boolean multiplesFirmas = Boolean.FALSE;
	private CardinalidadDeOpciones cardinalidadDeOpciones = CardinalidadDeOpciones.UNA;
	private String[] campos;
	private String[] etiquetas;
	private String origenHashCertificadoVoto;
	private String hashCertificadoVotoBase64;
	private String origenHashSolicitudAcceso;
	private String hashSolicitudAccesoBase64;
	private String url;
	private ActorVS centroControl;
	private ActorVS controlAcceso;
	private String urlSolicitudAcceso;
	private String urlRecolectorVotosCentroControl;
	private TypeVS type;
	private Integer numeroTotalFirmas;
	private Integer numeroTotalVotos;
	private Boolean firmado;
	private Boolean copiaSeguridadDisponible = true;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
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
	 * @return the hashCertificadoVotoBase64
	 */
	public String getHashCertificadoVotoHex() {
		if (hashCertificadoVotoBase64 == null) return null;
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		return hexConverter.marshal(hashCertificadoVotoBase64.getBytes());
	}
	

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	
	/**
	 * @return the eventoId
	 */
	public Long getEventoId() {
		if(eventoId == null) return id;
		return eventoId;
	}

	/**
	 * @param eventoId the eventoId to set
	 */
	public void setEventoId(Long eventoId) {
		this.eventoId = eventoId;
	}

		/**
	 * @return the cardinalidadDeOpciones
	 */
	public CardinalidadDeOpciones getCardinalidadDeOpciones() {
		return cardinalidadDeOpciones;
	}

	/**
	 * @param cardinalidadDeOpciones the cardinalidadDeOpciones to set
	 */
	public void setCardinalidadDeOpciones(CardinalidadDeOpciones cardinalidadDeOpciones) {
		this.cardinalidadDeOpciones = cardinalidadDeOpciones;
	}

	/**
	 * @return the campos
	 */
	public String[] getCampos() {
		return campos;
	}

	/**
	 * @param campos the campos to set
	 */
	public void setCampos(String[] campos) {
		this.campos = campos;
	}

	/**
	 * @return the etiquetas
	 */
	public String[] getEtiquetas() {
		return etiquetas;
	}

	/**
	 * @param etiquetas the etiquetas to set
	 */
	public void setEtiquetas(String[] etiquetas) {
		this.etiquetas = etiquetas;
	}
	
	/**
	 * @param hashSolicitudAccesoBase64 the hashSolicitudAccesoBase64 to set
	 */
	public void setHashSolicitudAccesoBase64(String hashSolicitudAccesoBase64) {
		this.hashSolicitudAccesoBase64 = hashSolicitudAccesoBase64;
	}
	
	
	public Map getDataMap() {
		logger.debug("getDataMap");
		Map map = new HashMap();
		if(asunto != null) map.put("asunto", asunto);
		if(contenido != null) map.put("contenido", contenido);
		if(fechaInicio != null) map.put("fechaInicio", DateUtils.getStringFromDate(fechaInicio));
		if(fechaFin != null) map.put("fechaFin", DateUtils.getStringFromDate(fechaFin));
		if(url != null) map.put("url", url);
		map.put("copiaSeguridadDisponible", copiaSeguridadDisponible);
		if(userVS != null) {
			Map userVSHashMap = new HashMap();
			userVSHashMap.put("nif", userVS.getNif());
			userVSHashMap.put("email", userVS.getEmail());
			userVSHashMap.put("nombre", userVS.getNombre());
			map.put("votante", userVSHashMap);
		}
		if(opcionSeleccionada != null) {
			HashMap opcionHashMap = new HashMap();
			opcionHashMap.put("id", opcionSeleccionada.getId());
			opcionHashMap.put("contenido", opcionSeleccionada.getContent());
			map.put("opcionSeleccionada", opcionHashMap);
		}
		if(centroControl != null) {
			HashMap centroControlHashMap = new HashMap();
			centroControlHashMap.put("serverURL", centroControl.getServerURL());
			centroControlHashMap.put("nombre", centroControl.getNombre());
			map.put("centroControl", centroControlHashMap);
		}
		if(hashCertificadoVotoBase64 != null) {
			map.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);
			map.put("hashCertificadoVotoHex", getBase64ToHexStr(hashCertificadoVotoBase64));
		}
		if(hashSolicitudAccesoBase64 != null) {
			map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
			map.put("hashSolicitudAccesoHex", getBase64ToHexStr(hashSolicitudAccesoBase64));
		}
		if (type != null) map.put("tipoEvento", type.toString());
		if (getEventoId() != null) map.put("eventoId", getEventoId());
		if (id != null) map.put("id", id);
		map.put("UUID", UUID.randomUUID().toString());
		HashMap resultMap = new HashMap(map);
		if (etiquetas != null) {
			List<String> labelList = new ArrayList<String>();
			for (String etiqueta : etiquetas) {
				labelList.add(etiqueta);
			}
			resultMap.put("etiquetas", labelList);
		}
		if (centroControl != null) {
			Map centroControlMap = new HashMap();
			centroControlMap.put("id", centroControl.getId());
			centroControlMap.put("nombre", centroControl.getNombre());
			centroControlMap.put("serverURL", centroControl.getServerURL());
			HashMap centroControlJSON = new HashMap( centroControlMap );
			resultMap.put("centroControl", centroControlJSON);
		}
		if (opciones != null) {
			List<Map> optionList = new ArrayList<Map>();
			for (OptionVS opcion : opciones) {
				Map campoMap = new HashMap();
				campoMap.put("contenido", opcion.getContent());
				campoMap.put("valor", opcion.getValue());
				campoMap.put("id", opcion.getId());
				optionList.add(campoMap);
			}
			if(type == TypeVS.VOTING_EVENT) resultMap.put("opciones", optionList);
			else resultMap.put("campos", optionList);
		}
		if (cardinalidadDeOpciones != null) map.put("cardinalidad",
				cardinalidadDeOpciones.toString());
		if (opcionSeleccionada != null) {
			Map opcionSeleccionadaMap = new HashMap();
			opcionSeleccionadaMap.put("id", opcionSeleccionada.getId());
			opcionSeleccionadaMap.put("contenido", opcionSeleccionada.getContent());
			HashMap opcionSeleccionadaJSON = new HashMap(opcionSeleccionadaMap);
			resultMap.put("opcionSeleccionada", opcionSeleccionadaJSON);
		}
		return resultMap;
	}
	 
	public HashMap getVoteDataMap() {
		logger.debug("getVoteDataMap");
		Map map = new HashMap();
		map.put("operation", TypeVS.SEND_SMIME_VOTE.toString());
		map.put("eventoURL", url);
		map.put("opcionSeleccionadaId", opcionSeleccionada.getId());
		map.put("opcionSeleccionadaContenido", opcionSeleccionada.getContent());
		map.put("UUID", UUID.randomUUID().toString());
		HashMap jsonObject = new HashMap(map);
		return jsonObject;
	}
	
	public HashMap getAccessRequestDataMap() {
		logger.debug("getAccessRequestDataMap");
		Map map = new HashMap();
		map.put("operation", TypeVS.ACCESS_REQUEST.toString());
		if(eventoId != null) map.put("eventId", eventoId);
		else map.put("eventId", id);
		map.put("eventURL", url);
		map.put("UUID", UUID.randomUUID().toString());
		map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
		HashMap jsonObject = new HashMap(map);
		return jsonObject;
	}
	
	
	public HashMap getCancelVoteDataMap() {
		logger.debug("getCancelVoteDataMap");
		Map map = new HashMap();
		map.put("operation", TypeVS.CANCEL_VOTE.toString());
		map.put("origenHashCertificadoVoto", origenHashCertificadoVoto);
		map.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);
		map.put("origenHashSolicitudAcceso", origenHashSolicitudAcceso);
		map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
		map.put("UUID", UUID.randomUUID().toString());
		HashMap jsonObject = new HashMap(map);
		return jsonObject;
	}
	
	public HashMap getChangeEventDataMap(String serverURL, Estado state) {
		logger.debug("getCancelEventDataMap");
		Map map = new HashMap();
		map.put("operation", TypeVS.EVENT_CANCELLATION.toString());
		map.put("accessControlURL", serverURL);
		if(eventoId != null) map.put("eventId", eventoId);
		else map.put("eventId", id);
		map.put("estado", state.toString());
		map.put("UUID", UUID.randomUUID().toString());
		HashMap jsonObject = new HashMap(map);
		return jsonObject;
	}
	
	public String getBase64ToHexStr(String base64Str) {
		if (base64Str == null) return null;
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		return hexConverter.marshal(base64Str.getBytes());
	}

	/**
	 * @return the asunto
	 */
	public String getAsunto() {
		return asunto;
	}

    public String getSubject() {
    	return asunto;
    }
    
	/**
	 * @param asunto the asunto to set
	 */
	public void setAsunto(String asunto) {
		this.asunto = asunto;
	}

	/**
	 * @return the urlSolicitudAcceso
	 */
	public String getUrlSolicitudAcceso() {
		return urlSolicitudAcceso;
	}

	/**
	 * @param urlSolicitudAcceso the urlSolicitudAcceso to set
	 */
	public void setUrlSolicitudAcceso(String urlSolicitudAcceso) {
		this.urlSolicitudAcceso = urlSolicitudAcceso;
	}

	/**
	 * @return the urlRecolectorVotosCentroControl
	 */
	public String getUrlRecolectorVotosCentroControl() {
		return urlRecolectorVotosCentroControl;
	}

	/**
	 * @param urlRecolectorVotosCentroControl the urlRecolectorVotosCentroControl to set
	 */
	public void setUrlRecolectorVotosCentroControl(String urlRecolectorVotosCentroControl) {
		this.urlRecolectorVotosCentroControl = urlRecolectorVotosCentroControl;
	}

	/**
	 * @return the contenido
	 */
	public String getContenido() {
		return contenido;
	}

	/**
	 * @param contenido the contenido to set
	 */
	public void setContenido(String contenido) {
		this.contenido = contenido;
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
		this.fechaInicioStr = DateUtils.getStringFromDate(fechaInicio);
	}

	/**
	 * @return the fechaInicioStr
	 */
	public String getFechaInicioStr() {
		return fechaInicioStr;
	}

	/**
	 * @param fechaInicioStr the fechaInicioStr to set
	 */
	public void setFechaInicioStr(String fechaInicioStr) throws ParseException {
		this.fechaInicioStr = fechaInicioStr;
		this.fechaInicio = DateUtils.getDateFromString(fechaInicioStr);
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
		this.fechaFinStr = DateUtils.getStringFromDate(fechaFin);
	}

	/**
	 * @return the fechaFinStr
	 */
	public String getFechaFinStr() {
		return fechaFinStr;
	}

	/**
	 * @param fechaFinStr the fechaFinStr to set
	 */
	public void setFechaFinStr(String fechaFinStr) throws ParseException {
		this.fechaFinStr = fechaFinStr;
		this.fechaFin = DateUtils.getDateFromString(fechaFinStr);
	}

	/**
	 * @return the multiplesFirmas
	 */
	public Boolean getMultiplesFirmas() {
		return multiplesFirmas;
	}

	/**
	 * @param multiplesFirmas the multiplesFirmas to set
	 */
	public void setMultiplesFirmas(Boolean multiplesFirmas) {
		this.multiplesFirmas = multiplesFirmas;
	}

	/**
	 * @return the opcionSeleccionada
	 */
	public OptionVS getOpcionSeleccionada() {
		return opcionSeleccionada;
	}

	/**
	 * @param opcionSeleccionada the opcionSeleccionada to set
	 */
	public void setOpcionSeleccionada(OptionVS opcionSeleccionada) {
		this.opcionSeleccionada = opcionSeleccionada;
	}

	/**
	 * @return the opciones
	 */
	public List<OptionVS> getOpciones() {
		return opciones;
	}

	/**
	 * @param opciones the opciones to set
	 */
	public void setOpciones(List<OptionVS> opciones) {
		this.opciones = opciones;
	}

	/**
	 * @return the centroControl
	 */
	public ActorVS getCentroControl() {
		return centroControl;
	}

	/**
	 * @param centroControl the centroControl to set
	 */
	public void setCentroControl(ActorVS centroControl) {
		this.centroControl = centroControl;
	}

	/**
	 * @return the controlAcceso
	 */
	public ActorVS getControlAcceso() {
		return controlAcceso;
	}

	/**
	 * @param controlAcceso the controlAcceso to set
	 */
	public void setControlAcceso(ActorVS controlAcceso) {
		this.controlAcceso = controlAcceso;
	}

	/**
	 * @return the userVS
	 */
	public UserVS getUserVS() {
		return userVS;
	}

	/**
	 * @param userVS the userVS to set
	 */
	public void setUsuario(UserVS userVS) {
		this.userVS = userVS;
	}

	public Integer getNumeroTotalFirmas() {
			return numeroTotalFirmas;
	}

	public void setNumeroTotalFirmas(Integer numeroTotalFirmas) {
			this.numeroTotalFirmas = numeroTotalFirmas;
	}

	public Integer getNumeroTotalVotos() {
			return numeroTotalVotos;
	}

	public void setNumeroTotalVotos(Integer numeroTotalVotos) {
			this.numeroTotalVotos = numeroTotalVotos;
	}

	public Boolean getFirmado() {
			return firmado;
	}

	public void setFirmado(Boolean firmado) {
			this.firmado = firmado;
	}

	public TypeVS getType() {
			return type;
	}

	public void setType(TypeVS type) {
			this.type = type;
	}

	public void genVote() throws NoSuchAlgorithmException {
		origenHashSolicitudAcceso = UUID.randomUUID().toString();
		hashSolicitudAccesoBase64 =  getHashBase64(
			origenHashSolicitudAcceso, VOTING_DATA_DIGEST);
		origenHashCertificadoVoto = UUID.randomUUID().toString();
		hashCertificadoVotoBase64 = getHashBase64(
			origenHashCertificadoVoto, VOTING_DATA_DIGEST);
	}
	
	public static String getHashBase64 (String cadenaOrigen,
		String digestAlgorithm) throws NoSuchAlgorithmException {
		MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
		byte[] resultDigest =  sha.digest( cadenaOrigen.getBytes() );
		return DatatypeConverter.printBase64Binary(resultDigest);
	}
	
	public EventVSBase genRandomVote (String digestAlg)throws NoSuchAlgorithmException {
		EventVSBase voto = new EventVSBase();
		voto.setAsunto(asunto);
		voto.setCentroControl(centroControl);
		voto.setContenido(contenido);
		voto.setControlAcceso(controlAcceso);
		voto.setEventoId(getEventoId());
		voto.setUrl(url);
		voto.setOpciones(opciones);
		String origenHashSolicitudAcceso = UUID.randomUUID().toString();
		voto.setOrigenHashSolicitudAcceso(origenHashSolicitudAcceso);
		voto.setHashSolicitudAccesoBase64(getHashBase64(
			origenHashSolicitudAcceso, digestAlg));
		String origenHashCertificadoVoto = UUID.randomUUID().toString();
		voto.setOrigenHashCertificadoVoto(origenHashCertificadoVoto);
		voto.setHashCertificadoVotoBase64(getHashBase64(
			origenHashCertificadoVoto, digestAlg));
		voto.setOpcionSeleccionada(getRandomOption());
		return voto;
	}
	
	public HashMap getSignatureContentMap(EventVSBase evento) {
		logger.debug("obtenerFirmaParaEventoJSON");
		Map map = new HashMap();
		Map controlAccesoMap = new HashMap();
		controlAccesoMap.put("serverURL", controlAcceso.getServerURL());
		controlAccesoMap.put("nombre", controlAcceso.getNombre());
		map.put("controlAcceso", controlAccesoMap);
		map.put("id", id);
		if(eventoId != null) map.put("eventoId", eventoId);
		else map.put("eventoId", id);
		map.put("asunto", asunto);
		map.put("contenido", contenido);
		map.put("UUID", UUID.randomUUID().toString());
		HashMap resultMap = new HashMap( map );
		if (evento.getOpciones() != null) {
			List<OptionVS> opciones = evento.getOpciones();
			List<Map> optionsMapList = new ArrayList<Map>();
			for (OptionVS opcion : opciones) {
				Map campoMap = new HashMap();
				campoMap.put("id", opcion.getId());
				campoMap.put("contenido", opcion.getContent());
				campoMap.put("valor", opcion.getValue());
				optionsMapList.add(campoMap);
			}
			resultMap.put("opciones", optionsMapList);
		}
		return resultMap;
	}
	
	private OptionVS getRandomOption () {
		int size = opciones.size();
		int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
		return opciones.get(item);
	}
	
	public static EventVSBase populate (Map eventMap) {
		if(eventMap == null) return null;
		EventVSBase evento = null; 
		try {
			evento = new EventVSBase();
			if(eventMap.get("id") != null && 
	        		!"null".equals(eventMap.get("id").toString())) {
				evento.setId(((Integer) eventMap.get("id")).longValue());
			}
			if(eventMap.containsKey("eventoId")) evento.setEventoId(
					((Integer) eventMap.get("eventoId")).longValue());
			if(eventMap.containsKey("URL"))
				evento.setUrl((String) eventMap.get("URL"));
			if(eventMap.containsKey("centroControl")) {
				evento.setCentroControl(ActorVS.populate((Map) eventMap.get("centroControl")));
			}
			if(eventMap.containsKey("controlAcceso")) {
				evento.setControlAcceso(ActorVS.populate((Map)eventMap.get("controlAcceso")));
			}
			if(eventMap.containsKey("asunto")) evento.
					setAsunto((String) eventMap.get("asunto"));
			if(eventMap.containsKey("hashSolicitudAccesoBase64")) evento.
				setHashSolicitudAccesoBase64(
					(String) eventMap.get("hashSolicitudAccesoBase64"));
			if(eventMap.containsKey("opcionSeleccionadaId")) {
				OptionVS opcion = new OptionVS();
				opcion.setId(((Integer) eventMap.get("opcionSeleccionadaId")).longValue());
				if(eventMap.containsKey("opcionSeleccionadaContenido")) {
					opcion.setContent(
							(String) eventMap.get("opcionSeleccionadaContenido"));
				}
				evento.setOpcionSeleccionada(opcion);
			}
			if(eventMap.containsKey("estado")) {
				EventVSBase.Estado estado = EventVSBase.Estado.valueOf(
						(String) eventMap.get("estado"));
				evento.setEstado(estado);
			}
			if(eventMap.containsKey("eventoURL")) evento.setUrl(
					(String) eventMap.get("eventoURL"));
			if(eventMap.containsKey("urlSolicitudAcceso")) evento.
					setUrlSolicitudAcceso((String) eventMap.get("urlSolicitudAcceso"));
			if(eventMap.containsKey("urlRecolectorVotosCentroControl")) evento.
					setUrlRecolectorVotosCentroControl(
					(String) eventMap.get("urlRecolectorVotosCentroControl"));
			if(eventMap.containsKey("contenido")) evento.setContenido(
					(String) eventMap.get("contenido"));
			if(eventMap.containsKey("fechaInicio")) evento.setFechaInicioStr(
					(String) eventMap.get("fechaInicio"));
			if(eventMap.containsKey("fechaFin")) evento.setFechaFinStr(
					(String) eventMap.get("fechaFin"));
			if (eventMap.containsKey("numeroTotalFirmas"))
				evento.setNumeroTotalFirmas((Integer) eventMap.get("numeroTotalFirmas"));
			if (eventMap.containsKey("numeroTotalVotos"))
				evento.setNumeroTotalVotos((Integer) eventMap.get("numeroTotalVotos"));
			
			if(eventMap.containsKey("multiplesFirmas")) evento.setMultiplesFirmas(
					(Boolean) eventMap.get("multiplesFirmas"));
			if(eventMap.containsKey("cardinalidadDeOpciones")) {
				evento.setCardinalidadDeOpciones(CardinalidadDeOpciones.valueOf(
						(String) eventMap.get("cardinalidadDeOpciones")));
			}
			if (eventMap.containsKey("userVS")) {
				UserVS userVS = ContextVS.INSTANCE.getUserVS();
				userVS.setNombre((String) eventMap.get("userVS"));
				evento.setUsuario(userVS);
			}
			if (eventMap.containsKey("copiaSeguridadDisponible")) {
				evento.setCopiaSeguridadDisponible((Boolean) eventMap.get(
						"copiaSeguridadDisponible"));
			}
			if(eventMap.containsKey("campos")) {
				List<OptionVS> campos = new ArrayList<OptionVS>();
				for (Map fieldMap : (List<Map>) eventMap.get("campos")) {
					OptionVS campo = new OptionVS();
					if(eventMap.containsKey("id"))
						campo.setId(((Integer) fieldMap.get("id")).longValue());
					if(eventMap.containsKey("contenido"))
						campo.setContent((String) fieldMap.get("contenido"));
					campos.add(campo);
				 }
				evento.setOpciones(campos);
			}
			if(eventMap.containsKey("opciones")) {
				List<OptionVS> opciones =  new ArrayList<OptionVS>();
				for(Map option : (List<Map>) eventMap.get("opciones")) {
					opciones.add(OptionVS.populate(option));
				}
				evento.setOpciones(opciones);
			}
			if(eventMap.containsKey("opcionSeleccionada")) {
				evento.setOpcionSeleccionada(OptionVS.populate((Map)eventMap.get("opcionSeleccionada")));
			}
			if(eventMap.get("etiquetas") != null && 
					!"null".equals(eventMap.get("etiquetas").toString())) {
				List<String> labelList = (List<String>)eventMap.get("etiquetas");
				evento.setEtiquetas(labelList.toArray(new String[labelList.size()]));
			}
		} catch(Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return evento;
	}
	
	
	/**
	 * @return the estado
	 */
	public Estado getEstado() {
		return estado;
	}

	/**
	 * @param estado the estado to set
	 */
	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	/**
	 * @return the nextState
	 */
	public Estado getNextState() {
		return nextState;
	}

	/**
	 * @param nextState the nextState to set
	 */
	public void setNextState(Estado nextState) {
		this.nextState = nextState;
	}

	/**
	 * @return the copiaSeguridadDisponible
	 */
	public Boolean getCopiaSeguridadDisponible() {
		return copiaSeguridadDisponible;
	}

	/**
	 * @param copiaSeguridadDisponible the copiaSeguridadDisponible to set
	 */
	public void setCopiaSeguridadDisponible(Boolean copiaSeguridadDisponible) {
		this.copiaSeguridadDisponible = copiaSeguridadDisponible;
	}
       
}
