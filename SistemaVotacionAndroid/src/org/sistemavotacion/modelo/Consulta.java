package org.sistemavotacion.modelo;

import java.util.List;

public class Consulta {

    private int numeroEventosFirmaEnPeticion;
    private int numeroTotalEventosFirmaEnSistema;
    private int numeroEventosVotacionEnPeticion;
    private int numeroTotalEventosVotacionEnSistema;
    private int numeroEventosReclamacionEnPeticion;
    private int numeroTotalEventosReclamacionEnSistema;
    private int numeroEventosEnPeticion;
    private int numeroTotalEventosEnSistema;
    
    private int offset;
    private List<Evento> eventos;


    public void setOffset(int offset) {
            this.offset = offset;
    }

    public int getOffset() {
            return offset;
    }

    /**
     * @return the numeroEventosFirmaEnPeticion
     */
    public int getNumeroEventosFirmaEnPeticion() {
        return numeroEventosFirmaEnPeticion;
    }

    /**
     * @param numeroEventosFirmaEnPeticion the numeroEventosFirmaEnPeticion to set
     */
    public void setNumeroEventosFirmaEnPeticion(int numeroEventosFirmaEnPeticion) {
        this.numeroEventosFirmaEnPeticion = numeroEventosFirmaEnPeticion;
    }

    /**
     * @return the numeroTotalEventosFirmaEnSistema
     */
    public int getNumeroTotalEventosFirmaEnSistema() {
        return numeroTotalEventosFirmaEnSistema;
    }

    /**
     * @param numeroTotalEventosFirmaEnSistema the numeroTotalEventosFirmaEnSistema to set
     */
    public void setNumeroTotalEventosFirmaEnSistema(int numeroTotalEventosFirmaEnSistema) {
        this.numeroTotalEventosFirmaEnSistema = numeroTotalEventosFirmaEnSistema;
    }

    /**
     * @return the numeroEventosVotacionEnPeticion
     */
    public int getNumeroEventosVotacionEnPeticion() {
        return numeroEventosVotacionEnPeticion;
    }

    /**
     * @param numeroEventosVotacionEnPeticion the numeroEventosVotacionEnPeticion to set
     */
    public void setNumeroEventosVotacionEnPeticion(int numeroEventosVotacionEnPeticion) {
        this.numeroEventosVotacionEnPeticion = numeroEventosVotacionEnPeticion;
    }

    /**
     * @return the numeroTotalEventosVotacionEnSistema
     */
    public int getNumeroTotalEventosVotacionEnSistema() {
        return numeroTotalEventosVotacionEnSistema;
    }

    /**
     * @param numeroTotalEventosVotacionEnSistema the numeroTotalEventosVotacionEnSistema to set
     */
    public void setNumeroTotalEventosVotacionEnSistema(int numeroTotalEventosVotacionEnSistema) {
        this.numeroTotalEventosVotacionEnSistema = numeroTotalEventosVotacionEnSistema;
    }

    /**
     * @return the numeroEventosReclamacionEnPeticion
     */
    public int getNumeroEventosReclamacionEnPeticion() {
        return numeroEventosReclamacionEnPeticion;
    }

    /**
     * @param numeroEventosReclamacionEnPeticion the numeroEventosReclamacionEnPeticion to set
     */
    public void setNumeroEventosReclamacionEnPeticion(int numeroEventosReclamacionEnPeticion) {
        this.numeroEventosReclamacionEnPeticion = numeroEventosReclamacionEnPeticion;
    }

    /**
     * @return the numeroTotalEventosReclamacionEnSistema
     */
    public int getNumeroTotalEventosReclamacionEnSistema() {
        return numeroTotalEventosReclamacionEnSistema;
    }

    /**
     * @param numeroTotalEventosReclamacionEnSistema the numeroTotalEventosReclamacionEnSistema to set
     */
    public void setNumeroTotalEventosReclamacionEnSistema(int numeroTotalEventosReclamacionEnSistema) {
        this.numeroTotalEventosReclamacionEnSistema = numeroTotalEventosReclamacionEnSistema;
    }

    /**
     * @return the numeroEventosEnPeticion
     */
    public int getNumeroEventosEnPeticion() {
        return numeroEventosEnPeticion;
    }

    /**
     * @param numeroEventosEnPeticion the numeroEventosEnPeticion to set
     */
    public void setNumeroEventosEnPeticion(int numeroEventosEnPeticion) {
        this.numeroEventosEnPeticion = numeroEventosEnPeticion;
    }

    /**
     * @return the numeroTotalEventosEnSistema
     */
    public int getNumeroTotalEventosEnSistema() {
        return numeroTotalEventosEnSistema;
    }

    /**
     * @param numeroTotalEventosEnSistema the numeroTotalEventosEnSistema to set
     */
    public void setNumeroTotalEventosEnSistema(int numeroTotalEventosEnSistema) {
        this.numeroTotalEventosEnSistema = numeroTotalEventosEnSistema;
    }

	public List<Evento> getEventos() {
		return eventos;
	}

	public void setEventos(List<Evento> eventos) {
		this.eventos = eventos;
	}
    
}
