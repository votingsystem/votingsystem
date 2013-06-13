package org.sistemavotacion.controlacceso.modelo;

import org.sistemavotacion.controlacceso.modelo.Evento;
import org.sistemavotacion.controlacceso.modelo.OpcionDeEvento;
import org.sistemavotacion.controlacceso.modelo.Usuario;

public enum VotingEvent {

	ACCESS_REQUEST, ACCESS_REQUEST_REPRESENTATIVE, 
	ACCESS_REQUEST_USER_WITH_REPRESENTATIVE, VOTE,
	VOTE_CANCEL, REPRESENTATIVE_VOTE, REPRESENTATIVE_DELEGATION;
	
	private Usuario user;
	private Evento event;
	private OpcionDeEvento optionSelected;
	private Long numRepresentations;
	
	public VotingEvent setData(Evento event, 
			OpcionDeEvento optionSelected) {
		this.event = event;
		this.optionSelected = optionSelected;
		return this;
	}
	
	public VotingEvent setData(Usuario user, Evento event) {
		this.setUser(user);
		this.setEvent(event);
		return this;
	}
	
	public VotingEvent setData(Usuario user, Evento event,
			OpcionDeEvento optionSelected,  Long numRepresentations) {
		this.setUser(user);
		this.setEvent(event);
		this.numRepresentations = numRepresentations;
		this.setOptionSelected(optionSelected);
		return this;
	}


	public Long getNumRepresentations() {
		return numRepresentations;
	}

	public void setNumRepresentations(Long numRepresentations) {
		this.numRepresentations = numRepresentations;
	}

	public Usuario getUser() {
		return user;
	}

	public void setUser(Usuario user) {
		this.user = user;
	}

	public Evento getEvent() {
		return event;
	}

	public void setEvent(Evento event) {
		this.event = event;
	}

	public OpcionDeEvento getOptionSelected() {
		return optionSelected;
	}

	public void setOptionSelected(OpcionDeEvento optionSelected) {
		this.optionSelected = optionSelected;
	}

}
