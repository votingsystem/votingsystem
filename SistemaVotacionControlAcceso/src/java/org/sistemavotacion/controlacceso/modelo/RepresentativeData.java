package org.sistemavotacion.controlacceso.modelo;

public class RepresentativeData {
	
	private Long optionSelectedId = null;
	private Long numRepresentations = 0L;
	
	RepresentativeData(Long optionSelectedId, 
		Long numRepresentations) {
		this.optionSelectedId = optionSelectedId;
		this.numRepresentations = numRepresentations;
	}

	public Long getOptionSelectedId() {
		return optionSelectedId;
	}

	public void setOptionSelectedId(Long optionSelectedId) {
		this.optionSelectedId = optionSelectedId;
	}

	public Long getNumRepresentations() {
		return numRepresentations;
	}

	public void setNumRepresentations(Long numRepresentations) {
		this.numRepresentations = numRepresentations;
	}

}
