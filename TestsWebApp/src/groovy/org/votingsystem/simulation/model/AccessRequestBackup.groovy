package org.votingsystem.simulation.model;

import java.io.File;
import java.security.KeyStore;
import java.sql.Clob;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.votingsystem.model.*;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestBackup {
    
	
	private static Logger log = LoggerFactory.getLogger(AccessRequestBackup.class);

	private Long id;
	private TypeVS tipo;
	private Clob contenido;
	private String hashCertificadoVotoBase64;
	private String hashSolicitudAccesoBase64;
	private String origenHashSolicitudAcceso;
	private String origenHashCertificadoVoto;
	private String userNif;
	private String eventoURL;
	private String eventoId;
	private boolean firmaCorrecta;
	private UserVS usuario;
	private byte[] hashEncabezado;
	private Date dateCreated;
	private File archivo;
	private KeyStore userKeyStore;

	
	public AccessRequestBackup() { }
		
	public AccessRequestBackup(boolean firmaCorrecta) {
		this.firmaCorrecta = firmaCorrecta;
	}
	
	public TypeVS getTipo() {
		return tipo;
	}

	public void setTipo(TypeVS tipo) {
		this.tipo = tipo;
	}

	public Clob getContenido () {
		return contenido;
	}

	public void setContenido (Clob contenido) {
		this.contenido = contenido;
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

	public void setUsuario(UserVS usuario) {
		this.usuario = usuario;
	}

	public UserVS getUsuario() {
		return usuario;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setHashEncabezado(byte[] hashEncabezado) {
		this.hashEncabezado = hashEncabezado;
	}

	public byte[] getHashEncabezado() {
		return hashEncabezado;
	}

	/**
	 * @return the hashCertificadoVotoBase64
	 */
	public String getHashCertificadoVotoBase64() {
		return hashCertificadoVotoBase64;
	}

	/**
	 * @param hashCertificadoVotoBase64 the hashCertificadoVotoBase64 to set
	 */
	public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
		this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
	}

	/**
	 * @return the firmaCorrecta
	 */
	public boolean isFirmaCorrecta() {
		return firmaCorrecta;
	}

	/**
	 * @param firmaCorrecta the firmaCorrecta to set
	 */
	public void setFirmaCorrecta(boolean firmaCorrecta) {
		this.firmaCorrecta = firmaCorrecta;
	}

	/**
	 * @return the hashSolicitudAccesoBase64
	 */
	public String getHashSolicitudAccesoBase64() {
		return hashSolicitudAccesoBase64;
	}

	/**
	 * @param hashSolicitudAccesoBase64 the hashSolicitudAccesoBase64 to set
	 */
	public void setHashSolicitudAccesoBase64(String hashSolicitudAccesoBase64) {
		this.hashSolicitudAccesoBase64 = hashSolicitudAccesoBase64;
	}

	/**
	 * @return the eventoURL
	 */
	public String getEventoURL() {
		return eventoURL;
	}

	/**
	 * @param eventoURL the eventoURL to set
	 */
	public void setEventoURL(String eventoURL) {
		this.eventoURL = eventoURL;
	}

	/**
	 * @return the userKeyStore
	 */
	public KeyStore getUserKeyStore() {
		return userKeyStore;
	}

	/**
	 * @param userKeyStore the userKeyStore to set
	 */
	public void setUserKeyStore(KeyStore userKeyStore) {
		this.userKeyStore = userKeyStore;
	}

	/**
	 * @return the origenHashCertificadoVoto
	 */
	public String getorigenHashCertificadoVoto() {
		return origenHashCertificadoVoto;
	}

	/**
	 * @param origenHashCertificadoVoto the origenHashCertificadoVoto to set
	 */
	public void setorigenHashCertificadoVoto(String origenHashCertificadoVoto) {
		this.origenHashCertificadoVoto = origenHashCertificadoVoto;
	}

	/**
	 * @return the origenHashSolicitudAcceso
	 */
	public String getorigenHashSolicitudAcceso() {
		return origenHashSolicitudAcceso;
	}

	/**
	 * @param origenHashSolicitudAcceso the origenHashSolicitudAcceso to set
	 */
	public void setorigenHashSolicitudAcceso(String origenHashSolicitudAcceso) {
		this.origenHashSolicitudAcceso = origenHashSolicitudAcceso;
	}

	/**
	 * @return the userNif
	 */
	public String getUserNif() {
		return userNif;
	}

	/**
	 * @param userNif the userNif to set
	 */
	public void setUserNif(String userNif) {
		this.userNif = userNif;
	}

	/**
	 * @return the eventoId
	 */
	public String getEventoId() {
		return eventoId;
	}

	/**
	 * @param eventoId the eventoId to set
	 */
	public void setEventoId(String eventoId) {
		this.eventoId = eventoId;
	}

	/**
	 * @return the archivo
	 */
	public File getArchivo() {
		return archivo;
	}

	/**
	 * @param archivo the archivo to set
	 */
	public void setArchivo(File archivo) {
		this.archivo = archivo;
	}
	
	public JSONObject toJSON() {
		log.debug("toJSON");
		Map map = new HashMap();
		map.put("origenHashCertificadoVoto", origenHashCertificadoVoto);
		map.put("hashCertificadoVotoBase64", hashCertificadoVotoBase64);
		map.put("origenHashSolicitudAcceso", origenHashSolicitudAcceso);
		map.put("hashSolicitudAccesoBase64", hashSolicitudAccesoBase64);
		map.put("UUID", UUID.randomUUID().toString());
		JSONObject jsonObject = new JSONObject(map);
		return jsonObject;
	}
	
	public static AccessRequestBackup parse(String strSolicitud) throws Exception {
		log.debug("parse ");
		AccessRequestBackup solicitud = new AccessRequestBackup();
		JSONObject requestJSON = new JSONObject(strSolicitud);
		if(requestJSON.containsKey("hashSolicitudAccesoBase64")) 
			solicitud.setHashSolicitudAccesoBase64(requestJSON.getString("hashSolicitudAccesoBase64"));
		if(requestJSON.containsKey("origenHashSolicitudAcceso")) 
			solicitud.setorigenHashSolicitudAcceso(
					requestJSON.getString("origenHashSolicitudAcceso"));
		if(requestJSON.containsKey("hashCertificadoVotoBase64")) 
			solicitud.setHashCertificadoVotoBase64(
					requestJSON.getString("hashCertificadoVotoBase64"));
		if(requestJSON.containsKey("origenHashCertificadoVoto")) 
			solicitud.setorigenHashCertificadoVoto(
					requestJSON.getString("origenHashCertificadoVoto"));
		return solicitud;
	}
}