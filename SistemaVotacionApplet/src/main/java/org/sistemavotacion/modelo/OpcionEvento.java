package org.sistemavotacion.modelo;

import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class OpcionEvento {
        
    private static Logger logger = LoggerFactory.getLogger(OpcionEvento.class);
    
    private Long id;
    private String contenido;
    private String valor;
    private Integer numeroVotos;

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

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}
	
    public static OpcionEvento parse (String opcionStr) {
        if(opcionStr == null) return null;
        JSONObject eventoJSON = (JSONObject)JSONSerializer.toJSON(opcionStr);
        return parse(eventoJSON);
    }
    
    public static OpcionEvento parse (JSONObject opcionJSON) {
        if(opcionJSON == null) return null;
        OpcionEvento opcion = new OpcionEvento();       
        if(opcionJSON.containsKey("id")) opcion.setId(opcionJSON.getLong("id"));
        if(opcionJSON.containsKey("contenido")) opcion.setContenido(opcionJSON.getString("contenido"));
        return opcion;
    }
    
    public JSONObject obtenerJSON() {
        logger.debug("obtenerJSON");
        Map map = new HashMap();
        map.put("id", id);
        map.put("contenido", contenido);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
    }
    
    public String obtenerJSONStr() {
        logger.debug("obtenerJSONStr");
        JSONObject jsonObject = obtenerJSON();        
        return jsonObject.toString();
    }

	public Integer getNumeroVotos() {
		return numeroVotos;
	}

	public void setNumeroVotos(Integer numeroVotos) {
		this.numeroVotos = numeroVotos;
	}

}