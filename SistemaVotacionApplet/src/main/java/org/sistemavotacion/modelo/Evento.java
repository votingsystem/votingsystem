package org.sistemavotacion.modelo;

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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import static org.sistemavotacion.Contexto.VOTING_DATA_DIGEST;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Evento {
    
    private static Logger logger = LoggerFactory.getLogger(Evento.class);
    
    public enum CardinalidadDeOpciones { MULTIPLES, UNA}
    
    public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, PENDIENTE_COMIENZO,
    	PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}
    
  
    private Long id; //id of the event in the server
    private Long eventoId;//id of the event in the Access Control
    private OpcionEvento opcionSeleccionada;
    private Estado estado;
    private Estado nextState;
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
    
    public static Evento parse (String eventoStr) {
        if(eventoStr == null) return null;
        JSONObject eventoJSON = (JSONObject)JSONSerializer.toJSON(eventoStr);
        return parse(eventoJSON);
    }
    
    public JSONObject toJSON() {
        logger.debug("obtenerJSON");
        Map map = new HashMap();
        if(asunto != null) map.put("asunto", asunto);
        if(contenido != null) map.put("contenido", contenido);
        if(fechaInicio != null) map.put("fechaInicio", DateUtils.getStringFromDate(fechaInicio));
        if(fechaFin != null) map.put("fechaFin", DateUtils.getStringFromDate(fechaFin));
        if(url != null) map.put("url", url);
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
        if(hashCertificadoVotoBase64 != null) {
            map.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);
            map.put("hashCertificadoVotoHex", getBase64ToHexStr(hashCertificadoVotoBase64));
        }
        if(hashSolicitudAccesoBase64 != null) {
            map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
            map.put("hashSolicitudAccesoHex", getBase64ToHexStr(hashSolicitudAccesoBase64)); 
        }
        if (tipo != null) map.put("tipoEvento", tipo.toString()); 
        if (getEventoId() != null) map.put("eventoId", getEventoId());
        if (id != null) map.put("id", id);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        if (etiquetas != null) {
            JSONArray jsonArray = new JSONArray();
            for (String etiqueta : etiquetas) {
                jsonArray.element(etiqueta);
            }
            jsonObject.put("etiquetas", jsonArray);
        }
        if (centroControl != null) {
            Map centroControlMap = new HashMap(); 
            centroControlMap.put("id", centroControl.getId());
            centroControlMap.put("nombre", centroControl.getNombre());
            centroControlMap.put("serverURL", centroControl.getServerURL());
            JSONObject centroControlJSON = (JSONObject) JSONSerializer.toJSON( centroControlMap );
            jsonObject.put("centroControl", centroControlJSON);
        }        
        if (opciones != null) {
            JSONArray jsonArray = new JSONArray();
            for (OpcionEvento opcion : opciones) {
                Map campoMap = new HashMap();
                campoMap.put("contenido", opcion.getContenido());
                campoMap.put("valor", opcion.getValor());
                campoMap.put("id", opcion.getId());
                JSONObject camposJSON = (JSONObject) JSONSerializer.toJSON(campoMap);
                jsonArray.element(camposJSON);
            }
            if(tipo == Tipo.VOTACION) jsonObject.put("opciones", jsonArray);
            else jsonObject.put("campos", jsonArray);
        }
        if (cardinalidadDeOpciones != null) map.put("cardinalidad", 
                cardinalidadDeOpciones.toString()); 
        if (opcionSeleccionada != null) {
            Map opcionSeleccionadaMap = new HashMap(); 
            opcionSeleccionadaMap.put("id", opcionSeleccionada.getId());
            opcionSeleccionadaMap.put("contenido", opcionSeleccionada.getContenido());
            JSONObject opcionSeleccionadaJSON = (JSONObject) JSONSerializer.toJSON( opcionSeleccionadaMap );
            jsonObject.put("opcionSeleccionada", opcionSeleccionadaJSON);
        }     
        return jsonObject;
    }
     
    public JSONObject getVoteJSON() {
        logger.debug("getVoteJSON");
        Map map = new HashMap();
        map.put("operation", Operacion.Tipo.ENVIO_VOTO_SMIME.toString());
        map.put("eventoURL", url);
        map.put("opcionSeleccionadaId", opcionSeleccionada.getId());
        map.put("opcionSeleccionadaContenido", opcionSeleccionada.getContenido());
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject;
    }
    
    public JSONObject getAccessRequestJSON() {
        logger.debug("getAccessRequestJSON");
        Map map = new HashMap();
        map.put("operation", Operacion.Tipo.SOLICITUD_ACCESO.toString());
        if(eventoId != null) map.put("eventId", eventoId);
        else map.put("eventId", id);
        map.put("eventURL", url);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
    }
    
    
    public JSONObject getCancelVoteJSON() {
        logger.debug("getCancelVoteJSON");
        Map map = new HashMap();
        map.put("operation", Operacion.Tipo.ANULADOR_VOTO.toString());
        map.put("origenHashCertificadoVoto", origenHashCertificadoVoto);
        map.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);
        map.put("origenHashSolicitudAcceso", origenHashSolicitudAcceso);
        map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject;
    }
    
    public JSONObject getCancelEventJSON(String serverURL, Estado state) {
        logger.debug("getCancelEventJSON");
        Map map = new HashMap();
        map.put("operation", Operacion.Tipo.CANCELAR_EVENTO.toString());
        map.put("accessControlURL", serverURL);
        if(eventoId != null) map.put("eventId", eventoId);
        else map.put("eventId", id);
        map.put("estado", state.toString());
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
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

    public void genVote() throws NoSuchAlgorithmException {  
        origenHashSolicitudAcceso = UUID.randomUUID().toString();
        hashSolicitudAccesoBase64 = CMSUtils.getHashBase64(
            origenHashSolicitudAcceso, VOTING_DATA_DIGEST);
        origenHashCertificadoVoto = UUID.randomUUID().toString();
        hashCertificadoVotoBase64 = CMSUtils.getHashBase64(
            origenHashCertificadoVoto, VOTING_DATA_DIGEST);
    } 
    
    public Evento genRandomVote (String digestAlg)throws NoSuchAlgorithmException {
        Evento voto = new Evento();
        voto.setAsunto(asunto);
        voto.setCentroControl(centroControl);
        voto.setContenido(contenido);
        voto.setControlAcceso(controlAcceso);
        voto.setEventoId(getEventoId());
        voto.setUrl(url);
        voto.setOpciones(opciones);
        String origenHashSolicitudAcceso = UUID.randomUUID().toString();
        voto.setOrigenHashSolicitudAcceso(origenHashSolicitudAcceso);
        voto.setHashSolicitudAccesoBase64(CMSUtils.getHashBase64(
            origenHashSolicitudAcceso, digestAlg));
        String origenHashCertificadoVoto = UUID.randomUUID().toString();
        voto.setOrigenHashCertificadoVoto(origenHashCertificadoVoto);
        voto.setHashCertificadoVotoBase64(CMSUtils.getHashBase64(
            origenHashCertificadoVoto, digestAlg));  
        voto.setOpcionSeleccionada(getRandomOption());
        return voto;
    }
    
    public JSONObject getSignatureContentJSON(Evento evento) {
        logger.debug("obtenerFirmaParaEventoJSON");
        Map map = new HashMap();
        Map controlAccesoMap = new HashMap();
        controlAccesoMap.put("serverURL", controlAcceso.getServerURL());
        controlAccesoMap.put("nombre", controlAcceso.getNombre());
        map.put("controlAcceso", controlAccesoMap);
        if(eventoId != null) map.put("eventoId", eventoId);
        else map.put("eventoId", id);
        map.put("asunto", asunto);
        map.put("contenido", contenido);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( map );
        if (evento.getOpciones() != null) {
            List<OpcionEvento> opciones = evento.getOpciones();
            JSONArray jsonArray = new JSONArray();
            for (OpcionEvento opcion : opciones) {
                Map campoMap = new HashMap();
                campoMap.put("id", opcion.getId());
                campoMap.put("contenido", opcion.getContenido());
                campoMap.put("valor", opcion.getValor());
                JSONObject camposJSON = (JSONObject) JSONSerializer.toJSON(campoMap);
                jsonArray.element(camposJSON);
            }
            jsonObject.put("opciones", jsonArray);
        }
        return jsonObject;
    }
    
    private OpcionEvento getRandomOption () {
        int size = opciones.size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        return opciones.get(item);
    }
    
    public static Evento parse (JSONObject eventoJSON) {
        if(eventoJSON == null) return null;
        Evento evento = null;
        try {
            evento = new Evento();
            if(eventoJSON.containsKey("id")) 
                evento.setId(eventoJSON.getLong("id"));
            if(eventoJSON.containsKey("eventoId")) evento.setEventoId(
                    eventoJSON.getLong("eventoId"));
            if(eventoJSON.containsKey("URL")) 
                evento.setUrl(eventoJSON.getString("URL"));
            if(eventoJSON.containsKey("centroControl")) {
                evento.setCentroControl(ActorConIP.parse(eventoJSON.
                        getJSONObject("centroControl")));
            }        
            if(eventoJSON.containsKey("controlAcceso")) {
                evento.setControlAcceso(ActorConIP.parse(
                        eventoJSON.getJSONObject("controlAcceso")));
            } 
            if(eventoJSON.containsKey("asunto")) evento.
                    setAsunto(eventoJSON.getString("asunto"));
            if(eventoJSON.containsKey("hashSolicitudAccesoBase64")) evento.
                setHashSolicitudAccesoBase64(
                    eventoJSON.getString("hashSolicitudAccesoBase64"));
            if(eventoJSON.containsKey("opcionSeleccionadaId")) {
                OpcionEvento opcion = new OpcionEvento();
                opcion.setId(eventoJSON.getLong("opcionSeleccionadaId"));
                if(eventoJSON.containsKey("opcionSeleccionadaContenido")) {
                    opcion.setContenido(
                            eventoJSON.getString("opcionSeleccionadaContenido"));
                }
                evento.setOpcionSeleccionada(opcion);
            }
            if(eventoJSON.containsKey("estado")) {
                Evento.Estado estado = Evento.Estado.valueOf(
                        eventoJSON.getString("estado"));
                evento.setEstado(estado);
            }
            if(eventoJSON.containsKey("eventoURL")) evento.setUrl(
                    eventoJSON.getString("eventoURL"));
            if(eventoJSON.containsKey("urlSolicitudAcceso")) evento.
                    setUrlSolicitudAcceso(eventoJSON.getString("urlSolicitudAcceso"));
            if(eventoJSON.containsKey("urlRecolectorVotosCentroControl")) evento.
                    setUrlRecolectorVotosCentroControl(
                    eventoJSON.getString("urlRecolectorVotosCentroControl"));
            if(eventoJSON.containsKey("contenido")) evento.setContenido(
                    eventoJSON.getString("contenido"));   
            if(eventoJSON.containsKey("fechaInicio")) evento.setFechaInicioStr(
                    eventoJSON.getString("fechaInicio")); 
            if(eventoJSON.containsKey("fechaFin")) evento.setFechaFinStr(
                    eventoJSON.getString("fechaFin"));
            if (eventoJSON.containsKey("numeroTotalFirmas"))        
                evento.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalFirmas"));
            if (eventoJSON.containsKey("numeroTotalVotos"))        
                evento.setNumeroTotalVotos(eventoJSON.getInt("numeroTotalVotos"));
            
            if(eventoJSON.containsKey("multiplesFirmas")) evento.setMultiplesFirmas(
                    eventoJSON.getBoolean("multiplesFirmas"));
            if(eventoJSON.containsKey("cardinalidadDeOpciones")) {
                evento.setCardinalidadDeOpciones(CardinalidadDeOpciones.valueOf(
                        eventoJSON.getString("cardinalidadDeOpciones")));
            }
            if (eventoJSON.containsKey("usuario")) {
                Usuario usuario = new Usuario();
                usuario.setNombre(eventoJSON.getString("usuario"));
                evento.setUsuario(usuario);
            }
            if(eventoJSON.containsKey("campos")) {
                List<OpcionEvento> campos = new ArrayList<OpcionEvento>();
                JSONArray jsonArray = eventoJSON.getJSONArray("campos");
                 for (int i = 0; i< jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    OpcionEvento campo = new OpcionEvento();
                    if(eventoJSON.containsKey("id"))
                        campo.setId(jsonObject.getLong("id"));
                    if(eventoJSON.containsKey("contenido"))
                        campo.setContenido(jsonObject.getString("contenido"));
                    campos.add(campo);
                 }
                evento.setOpciones(campos);
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
}
