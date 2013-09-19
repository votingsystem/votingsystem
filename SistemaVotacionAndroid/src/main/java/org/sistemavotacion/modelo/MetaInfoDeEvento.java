package org.sistemavotacion.modelo;

/**
 *
 * @author jgzornoza
 */
public class MetaInfoDeEvento {

    private long numeroTotalFirmas;
    private long numeroVotosContabilizados;
    private String URL;
    private Tipo tipoEvento;
    private String asunto;

    /**
     * @return the numeroTotalFirmas
     */
    public long getNumeroTotalFirmas() {
        return numeroTotalFirmas;
    }

    /**
     * @param numeroTotalFirmas the numeroTotalFirmas to set
     */
    public void setNumeroTotalFirmas(long numeroTotalFirmas) {
        this.numeroTotalFirmas = numeroTotalFirmas;
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

    /**
     * @return the numeroVotosContabilizados
     */
    public long getNumeroVotosContabilizados() {
        return numeroVotosContabilizados;
    }

    /**
     * @param numeroVotosContabilizados the numeroVotosContabilizados to set
     */
    public void setNumeroVotosContabilizados(long numeroVotosContabilizados) {
        this.numeroVotosContabilizados = numeroVotosContabilizados;
    }
}
