package org.sistemavotacion.herramientavalidacion;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Tipo;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/HerramientaValidacionCopiasDeSeguridad/blob/master/licencia.txt
*/
public class MetaInfoDeEvento {

    private long numeroFirmas;
    private long numeroVotos;
    private long numeroSolicitudesAcceso;
    private String URL;
    private Tipo tipoEvento;
    private String asunto;

    /**
     * @return the numeroFirmas
     */
    public long getNumeroFirmas() {
        return numeroFirmas;
    }

    /**
     * @param numeroFirmas the numeroFirmas to set
     */
    public void setNumeroFirmas(long numeroFirmas) {
        this.numeroFirmas = numeroFirmas;
    }

    /**
     * @return the URL
     */
    public String getURL() {
        return URL;
    }

    /**
     * @param URL the URL to set
     */
    public void setURL(String URL) {
        this.URL = URL;
    }

    /**
     * @return the tipoEvento
     */
    public Tipo getTipoEvento() {
        return tipoEvento;
    }

    /**
     * @param tipoEvento the tipoEvento to set
     */
    public void setTipoEvento(Tipo tipoEvento) {
        this.tipoEvento = tipoEvento;
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

    
    public static MetaInfoDeEvento parse(String metaInfo) {
        JSONObject metaInfoJSON = (JSONObject)JSONSerializer.toJSON(metaInfo);
        MetaInfoDeEvento metaInfoDeEvento = new MetaInfoDeEvento();
        if (metaInfoJSON.containsKey("numeroFirmas")) 
            metaInfoDeEvento.setNumeroFirmas(metaInfoJSON.getLong("numeroFirmas"));
        if (metaInfoJSON.containsKey("numeroVotos")) 
            metaInfoDeEvento.setNumeroVotos(metaInfoJSON.getLong("numeroVotos"));        
        if (metaInfoJSON.containsKey("URL")) 
            metaInfoDeEvento.setURL(metaInfoJSON.getString("URL"));
        if (metaInfoJSON.containsKey("asunto")) 
            metaInfoDeEvento.setAsunto(metaInfoJSON.getString("asunto"));   
        if (metaInfoJSON.containsKey("solicitudesAcceso")) 
            metaInfoDeEvento.setNumeroSolicitudesAcceso(metaInfoJSON.getLong("solicitudesAcceso")); 
        if (metaInfoJSON.containsKey("tipoEvento")) 
            metaInfoDeEvento.setTipoEvento(Tipo.valueOf(metaInfoJSON.getString("tipoEvento")));
        return metaInfoDeEvento;
    }

    /**
     * @return the numeroVotos
     */
    public long getNumeroVotos() {
        return numeroVotos;
    }

    /**
     * @param numeroVotos the numeroVotos to set
     */
    public void setNumeroVotos(long numeroVotos) {
        this.numeroVotos = numeroVotos;
    }

    /**
     * @return the numeroSolicitudesAcceso
     */
    public long getNumeroSolicitudesAcceso() {
        return numeroSolicitudesAcceso;
    }

    /**
     * @param numeroSolicitudesAcceso the numeroSolicitudesAcceso to set
     */
    public void setNumeroSolicitudesAcceso(long numeroSolicitudesAcceso) {
        this.numeroSolicitudesAcceso = numeroSolicitudesAcceso;
    }
    
}
