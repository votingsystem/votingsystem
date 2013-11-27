package org.votingsystem.android.model;

import org.votingsystem.model.TypeVS;

/**
 *
 * @author jgzornoza
 */
public class MetaInfEventVS {

    private long numSignaturesCollected;
    private long numVotesVSAccounted;
    private String URL;
    private TypeVS typeVSEvento;
    private String subject;

    /**
     * @return the numSignaturesCollected
     */
    public long getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    /**
     * @param numSignaturesCollected the numSignaturesCollected to set
     */
    public void setNumSignaturesCollected(long numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
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
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @return the numVotesVSAccounted
     */
    public long getNumVotesVSAccounted() {
        return numVotesVSAccounted;
    }

    /**
     * @param numVotesVSAccounted the numVotesVSAccounted to set
     */
    public void setNumeroVotosContabilizados(long numVotesVSAccounted) {
        this.numVotesVSAccounted = numVotesVSAccounted;
    }
}
