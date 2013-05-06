package org.sistemavotacion.modelo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class ActorConIP {
        
    private static Logger logger = LoggerFactory.getLogger(ActorConIP.class);
    
    public enum Tipo {CENTRO_CONTROL("Centro de Control"), CONTROL_ACCESO("Control de Acceso");
	    private String mensaje;
	    
	    Tipo(String mensaje) {
	        this.mensaje = mensaje;
	    }
	    public String getMensaje() {
	        return this.mensaje;
	    }
	}
	
	public enum EnvironmentMode {DEVELOPMENT("Desarrollo"), TEST("Pruebas"), 
	    PRODUCTION("Producción");
	    private String mensaje;
	    EnvironmentMode(String mensaje) {
	        this.mensaje = mensaje;
	    }
	    public String getMensaje() {
	        return this.mensaje;
	    }
	}
	
	public enum Estado {
	    SUSPENDIDO ("Suspendido"), ACTIVO("Activo"), INACTIVO("Inactivo");
	    private String mensaje;
	    Estado(String mensaje) {
	        this.mensaje = mensaje;
	    }
	    public String getMensaje() {
	        return this.mensaje;
	    }
	}       
    
    private Long id;
    private String serverURL;
    private String nombre;
    private Estado estado; 
    private Tipo tipo;
    private EnvironmentMode environmentMode;
    private String certificadoURL;
    private String informacionVotosURL;
    private Set<ActorConIP> centrosDeControl;

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
     * @return the serverURL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * @param serverURL the serverURL to set
     */
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    public String getNombreNormalizado () {
        return nombre.replaceAll("[\\/:.]", ""); 
    }
    
    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @return the centrosDeControl
     */
    public Set<ActorConIP> getCentrosDeControl() {
        return centrosDeControl;
    }

    /**
     * @param centrosDeControl the centrosDeControl to set
     */
    public void setCentrosDeControl(Set<ActorConIP> centrosDeControl) {
        this.centrosDeControl = centrosDeControl;
    }
    
    /**
     * @return the informacionVotosURL
     */
    public String getInformacionVotosURL() {
        return informacionVotosURL;
    }

    /**
     * @param informacionVotosURL the informacionVotosURL to set
     */
    public void setInformacionVotosURL(String informacionVotosURL) {
        this.informacionVotosURL = informacionVotosURL;
    }
    
    public static ActorConIP parse (String actorConIPStr) {
        if(actorConIPStr == null) return null;
        JSONObject eventoJSON = (JSONObject)JSONSerializer.toJSON(actorConIPStr);
        return parse(eventoJSON);
    }
    
    public static ActorConIP parse (JSONObject actorConIPJSON) {
        if(actorConIPJSON == null) return null;
        ActorConIP actorConIP = new ActorConIP();       
        if(actorConIPJSON.containsKey("id")) actorConIP.setId(actorConIPJSON.getLong("id"));
        if(actorConIPJSON.containsKey("serverURL")) actorConIP.setServerURL(actorConIPJSON.getString("serverURL"));
        if(actorConIPJSON.containsKey("nombre")) actorConIP.setNombre(actorConIPJSON.getString("nombre"));
        if(actorConIPJSON.containsKey("informacionVotosURL")) actorConIP.setInformacionVotosURL(actorConIPJSON.getString("informacionVotosURL"));
        return actorConIP;
    }
    
    public JSONObject obtenerJSON() {
        logger.debug("obtenerJSON");
        Map map = new HashMap();
        map.put("id", id);
        map.put("serverURL", serverURL);
        map.put("nombre", nombre);
        map.put("informacionVotosURL", informacionVotosURL);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
    }
    
    public String obtenerJSONStr() {
        logger.debug("obtenerJSONStr");
        JSONObject jsonObject = obtenerJSON();        
        return jsonObject.toString();
    }

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public EnvironmentMode getEnvironmentMode() {
		return environmentMode;
	}

	public void setEnvironmentMode(EnvironmentMode environmentMode) {
		this.environmentMode = environmentMode;
	}

	public String getCertificadoURL() {
		return certificadoURL;
	}

	public void setCertificadoURL(String certificadoURL) {
		this.certificadoURL = certificadoURL;
	}

}
