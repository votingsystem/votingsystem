package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identification document type
 *
 * Blanco (Sin código asociado) -> EMPTY
 * A (Tarjetas de residencias) -> RESIDENCE
 * C (CIF) -> CIF
 * D (DNI) -> DNI
 * N (NIF) -> NIF
 * O (Otros documentos) -> OTHER
 * P (Pasaportes) -> PASSPORT
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 */
public enum IdDocument {

	@JsonProperty("DNI")
	DNI("D", "ESP"),
	@JsonProperty("NIF")
	NIF("N", "ESP"),
	@JsonProperty("OTHER")
	OTHER("O", "ESP"),
	@JsonProperty("RESIDENCE")
	RESIDENCE("A", "ESP"),
	@JsonProperty("CIF")
	CIF("C", "ESP"),
	@JsonProperty("PASSPORT")
	PASSPORT("p", "ESP"),
	@JsonProperty("EMPTY")
	EMPTY("", "ESP");

	private final String code;
	private final String country;

	IdDocument(String code, String country) {
		this.code = code;
		this.country = country;
	}

	/**
	 * code
	 * 
	 * @return code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * country
	 * 
	 * @return country
	 */
	public String getCountry() {
		return country;
	}

	public static IdDocument getByCode(String code) {
		if(code == null || code.trim().isEmpty()) return EMPTY;
		for(IdDocument idDocument : IdDocument.values()) {
			if(idDocument.getCode().toLowerCase().equals(code.toLowerCase())) return idDocument;
		}
		throw new IllegalArgumentException("unknown identification document: " + code);
	}

}
