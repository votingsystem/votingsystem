package org.votingsystem.android.model;

import org.votingsystem.model.TypeVS;

/**
 *
 * @author jgzornoza
 */
public class MetaInfoDeEvento {

    private long numeroTotalFirmas;
    private long numeroVotosContabilizados;
    private String URL;
    private TypeVS typeVSEvento;
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
     * @return the typeVSEvento
     */
    public TypeVS getTypeVSEvento() {
        return typeVSEvento;
    }

    /**
     * @param typeVSEvento the typeVSEvento to set
     */
    public void setTypeVSEvento(TypeVS typeVSEvento) {
        this.typeVSEvento = typeVSEvento;
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
