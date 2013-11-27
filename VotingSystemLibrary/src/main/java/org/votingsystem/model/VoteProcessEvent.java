package org.votingsystem.model;

public enum VoteProcessEvent {

	ACCESS_REQUEST, ACCESS_REQUEST_REPRESENTATIVE, 
	ACCESS_REQUEST_USER_WITH_REPRESENTATIVE, VOTE,
	VOTE_CANCEL, REPRESENTATIVE_VOTE, REPRESENTATIVE_DELEGATION;
	
	private UserVS user;
	private EventVS event;
	private FieldEventVS optionSelected;
	
	public VoteProcessEvent setData(EventVS event,
			FieldEventVS optionSelected) {
		this.event = event;
		this.optionSelected = optionSelected;
		return this;
	}
	
	public VoteProcessEvent setData(UserVS user, EventVS event) {
		this.setUser(user);
		this.setEvent(event);
		return this;
	}
	
	public VoteProcessEvent setData(UserVS user,
			EventVS event, FieldEventVS optionSelected) {
		this.setUser(user);
		this.setEvent(event);
		this.setOptionSelected(optionSelected);
		return this;
	}
	
	public UserVS getUser() {
		return user;
	}

	public void setUser(UserVS user) {
		this.user = user;
	}

	public EventVS getEvent() {
		return event;
	}

	public void setEvent(EventVS event) {
		this.event = event;
	}

	public FieldEventVS getOptionSelected() {
		return optionSelected;
	}

	public void setOptionSelected(FieldEventVS optionSelected) {
		this.optionSelected = optionSelected;
	}

}
