package org.sistemavotacion.modelo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class Evento {
    
    private static Logger logger = LoggerFactory.getLogger(Evento.class);
    
    public enum CardinalidadDeOpciones { MULTIPLES, UNA}
  
    private Long eventoId;
    private OpcionEvento opcionSeleccionada;
    private List<OpcionEvento> opciones;
    private String asunto;
    private String contenido;
    private Usuario usuario;
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
    private ActorConIP centroControl;
    private ActorConIP controlAcceso;
    private String urlSolicitudAcceso;
    private String urlRecolectorVotosCentroControl;
    private Tipo tipo;
    private Integer numeroTotalFirmas;
    private Integer numeroTotalVotos;
    private Boolean firmado;
    
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
    
    public static Evento parse (String eventoStr) {
        if(eventoStr == null) return null;
        JSONObject eventoJSON = (JSONObject)JSONSerializer.toJSON(eventoStr);
        return parse(eventoJSON);
    }
    
   public static Evento parse (JSONObject eventoJSON) {
        if(eventoJSON == null) return null;
        Evento evento = new Evento();
        if(eventoJSON.containsKey("id")) evento.setEventoId(eventoJSON.getLong("id"));
        if(eventoJSON.containsKey("URL")) evento.setUrl(eventoJSON.getString("URL"));
        if(eventoJSON.containsKey("centroControl")) {
            evento.setCentroControl(ActorConIP.parse(eventoJSON.getJSONObject("centroControl")));
        }        
        if(eventoJSON.containsKey("controlAcceso")) {
            evento.setControlAcceso(ActorConIP.parse(eventoJSON.getJSONObject("controlAcceso")));
        } 
        if(eventoJSON.containsKey("asunto")) evento.
                setAsunto(eventoJSON.getString("asunto"));
        if(eventoJSON.containsKey("hashSolicitudAccesoBase64")) evento.
            setHashSolicitudAccesoBase64(eventoJSON.getString("hashSolicitudAccesoBase64"));
        if(eventoJSON.containsKey("opcionSeleccionadaId")) {
            OpcionEvento opcion = new OpcionEvento();
            opcion.setId(eventoJSON.getLong("opcionSeleccionadaId"));
            if(eventoJSON.containsKey("opcionSeleccionadaContenido")) {
                opcion.setContenido(eventoJSON.getString("opcionSeleccionadaContenido"));
            }
            evento.setOpcionSeleccionada(opcion);
        }
        if(eventoJSON.containsKey("eventoURL")) evento.setUrl(
                eventoJSON.getString("eventoURL"));
        if(eventoJSON.containsKey("urlSolicitudAcceso")) evento.
                setUrlSolicitudAcceso(eventoJSON.getString("urlSolicitudAcceso"));
        if(eventoJSON.containsKey("urlRecolectorVotosCentroControl")) evento.
                setUrlRecolectorVotosCentroControl(eventoJSON.getString("urlRecolectorVotosCentroControl"));
        if(eventoJSON.containsKey("contenido")) evento.setContenido(
                eventoJSON.getString("contenido"));   
        try {
            if(eventoJSON.containsKey("fechaInicio")) evento.setFechaInicioStr(
                eventoJSON.getString("fechaInicio")); 
            if(eventoJSON.containsKey("fechaFin")) evento.setFechaFinStr(
                eventoJSON.getString("fechaFin")); 
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if(eventoJSON.containsKey("multiplesFirmas")) evento.setMultiplesFirmas(
                eventoJSON.getBoolean("multiplesFirmas"));
        if(eventoJSON.containsKey("cardinalidadDeOpciones")) {
            evento.setCardinalidadDeOpciones(CardinalidadDeOpciones.valueOf(
                    eventoJSON.getString("cardinalidadDeOpciones")));
        }
        if(eventoJSON.containsKey("campos")) {
            JSONArray arrayCampos = eventoJSON.getJSONArray("campos");
            String[] campos =  new String[arrayCampos.size()];
            for(int i = 0; i < arrayCampos.size(); i++) {
                campos[i] = arrayCampos.getString(i);
            }
            evento.setCampos(campos);
        }
        if(eventoJSON.containsKey("opciones")) {
            JSONArray arrayCampos = eventoJSON.getJSONArray("opciones");
            List<OpcionEvento> opciones =  new ArrayList<OpcionEvento>();
            for(int i = 0; i < arrayCampos.size(); i++) {
                opciones.add(OpcionEvento.parse(arrayCampos.getString(i)));
            }
            evento.setOpciones(opciones);
        }
        if(eventoJSON.containsKey("opcionSeleccionada")) {
            evento.setOpcionSeleccionada(OpcionEvento.parse(
                    eventoJSON.getJSONObject("opcionSeleccionada")));
        }
        if(eventoJSON.containsKey("etiquetas")) {
            JSONArray arrayEtiquetas = eventoJSON.getJSONArray("etiquetas");
            String[] etiquetas =  new String[arrayEtiquetas.size()];
            for(int i = 0; i < arrayEtiquetas.size(); i++) {
                etiquetas[i] = arrayEtiquetas.getString(i);
            }
        }    
        return evento;
    }
    
    public JSONObject obtenerJSON() {
        logger.debug("obtenerJSON");
        Map map = new HashMap();
        map.put("asunto", asunto);
        map.put("url", url);
        map.put("id", eventoId);
        if(usuario != null) {
            JSONObject usuarioJSONObject = new JSONObject();
            usuarioJSONObject.put("nif", usuario.getNif());
            usuarioJSONObject.put("email", usuario.getEmail());
            usuarioJSONObject.put("nombre", usuario.getNombre());
            map.put("votante", usuarioJSONObject);
        }
        if(opcionSeleccionada != null) {
            JSONObject opcionJSONObject = new JSONObject();
            opcionJSONObject.put("id", opcionSeleccionada.getId());
            opcionJSONObject.put("contenido", opcionSeleccionada.getContenido());
            map.put("opcionSeleccionada", opcionJSONObject);
        }
        if(centroControl != null) {
            JSONObject centroControlJSONObject = new JSONObject();
            centroControlJSONObject.put("serverURL", centroControl.getServerURL());
            centroControlJSONObject.put("nombre", centroControl.getNombre());
            map.put("centroControl", centroControlJSONObject);
        }
        map.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);
        map.put("hashCertificadoVotoHex", getBase64ToHexStr(hashCertificadoVotoBase64));
        map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
        map.put("hashSolicitudAccesoHex", getBase64ToHexStr(hashSolicitudAccesoBase64));
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
    }

    public String obtenerJSONStr() {
        logger.debug("obtenerJSONStr");
        JSONObject jsonObject = obtenerJSON();        
        return jsonObject.toString();
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
    public OpcionEvento getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    /**
     * @param opcionSeleccionada the opcionSeleccionada to set
     */
    public void setOpcionSeleccionada(OpcionEvento opcionSeleccionada) {
        this.opcionSeleccionada = opcionSeleccionada;
    }

    /**
     * @return the opciones
     */
    public List<OpcionEvento> getOpciones() {
        return opciones;
    }

    /**
     * @param opciones the opciones to set
     */
    public void setOpciones(List<OpcionEvento> opciones) {
        this.opciones = opciones;
    }

    /**
     * @return the centroControl
     */
    public ActorConIP getCentroControl() {
        return centroControl;
    }

    /**
     * @param centroControl the centroControl to set
     */
    public void setCentroControl(ActorConIP centroControl) {
        this.centroControl = centroControl;
    }

    /**
     * @return the controlAcceso
     */
    public ActorConIP getControlAcceso() {
        return controlAcceso;
    }

    /**
     * @param controlAcceso the controlAcceso to set
     */
    public void setControlAcceso(ActorConIP controlAcceso) {
        this.controlAcceso = controlAcceso;
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

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

    
}
