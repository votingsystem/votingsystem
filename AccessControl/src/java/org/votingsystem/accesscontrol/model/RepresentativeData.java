package org.votingsystem.accesscontrol.model;

public class RepresentativeData {
	
	private Long optionSelectedId        = null;
	private Long id                       = null;
	private Long numRepresentedWithVote  = 0L;
	private Long numTotalRepresentations = 0L;
	private String nif                   = null;
	
	RepresentativeData(Long id, String nif, Long optionSelectedId) {
		this.id = id;
		this.nif = nif;
		this.optionSelectedId = optionSelectedId;
	}
	
	RepresentativeData(String nif) { 
		this.nif = nif;
	}
	
	RepresentativeData() { }

	public Long getOptionSelectedId() {
		return optionSelectedId;
	}

	public void setOptionSelectedId(Long optionSelectedId) {
		this.optionSelectedId = optionSelectedId;
	}

	public Long getNumRepresentedWithVote() {
		return numRepresentedWithVote;
	}

	public void setNumRepresentedWithVote(Long numRepresentedWithVote) {
		this.numRepresentedWithVote = numRepresentedWithVote;
	}

	public Long getNumTotalRepresentations() {
		return numTotalRepresentations;
	}

	public void setNumTotalRepresentations(Long numTotalRepresentations) {
		this.numTotalRepresentations = numTotalRepresentations;
	}

	public String getNif() {
		return nif;
	}

	public void setNif(String nif) {
		this.nif = nif;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


}
