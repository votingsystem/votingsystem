package org.sistemavotacion.modelo;

import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class MensajeMime {
    
    private static final long serialVersionUID = 1L;

    private Long id;
    private Tipo tipo;
    private byte[] contenido;

    private Evento evento;

    private Recibo recibo;

    private Date dateCreated;

    /**
     * @return the contenido
     */
    public byte[] getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }
    
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

    public void setEvento(Evento evento) {
            this.evento = evento;
    }

    public Evento getEvento() {
            return evento;
    }

    public void setTipo(Tipo tipo) {
            this.tipo = tipo;
    }

    public Tipo getTipo() {
            return tipo;
    }

    public void setRecibo(Recibo recibo) {
            this.recibo = recibo;
    }

    public Recibo getRecibo() {
            return recibo;
    }

}
