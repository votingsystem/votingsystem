package org.votingsystem.simulation.model

import org.codehaus.groovy.grails.web.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS

import java.security.KeyStore
import java.sql.Clob
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestBackup {
    
	
	private static Logger logger = LoggerFactory.getLogger(AccessRequestBackup.class);

	private Long id;
	private TypeVS tipo;
	private Clob content;
	private String hashCertVoteBase64;
	private String hashAccessRequestBase64;
	private String originHashAccessRequest;
	private String originHashCertVote;
	private String userNif;
	private String eventURL;
	private String eventId;
	private boolean firmaCorrecta;
	private UserVS userVS;
	private byte[] hashEncabezado;
	private Date dateCreated;
	private File archivo;
	private KeyStore userKeyStore;

	
	public AccessRequestBackup() { }
		
	public AccessRequestBackup(boolean firmaCorrecta) {
		this.firmaCorrecta = firmaCorrecta;
	}
	
	public TypeVS getType() {
		return tipo;
	}

	public void setType(TypeVS tipo) {
		this.tipo = tipo;
	}

	public Clob getContent () {
		return content;
	}

	public void setContent (Clob content) {
		this.content = content;
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

	public void setUserVS(UserVS userVS) {
		this.userVS = userVS;
	}

	public UserVS getUserVS() {
		return userVS;
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
	 * @return the hashCertVoteBase64
	 */
	public String getHashCertVoteBase64() {
		return hashCertVoteBase64;
	}

	/**
	 * @param hashCertVoteBase64 the hashCertVoteBase64 to set
	 */
	public void setHashCertVoteBase64(String hashCertVoteBase64) {
		this.hashCertVoteBase64 = hashCertVoteBase64;
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
	 * @return the hashAccessRequestBase64
	 */
	public String getAccessRequestHashBase64() {
		return hashAccessRequestBase64;
	}

	/**
	 * @param hashAccessRequestBase64 the hashAccessRequestBase64 to set
	 */
	public void setAccessRequestHashBase64(String hashAccessRequestBase64) {
		this.hashAccessRequestBase64 = hashAccessRequestBase64;
	}

	/**
	 * @return the eventURL
	 */
	public String getEventVSURL() {
		return eventURL;
	}

	/**
	 * @param eventURL the eventURL to set
	 */
	public void setEventURL(String eventURL) {
		this.eventURL = eventURL;
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
	 * @return the originHashCertVote
	 */
	public String getoriginHashCertVote() {
		return originHashCertVote;
	}

	/**
	 * @param originHashCertVote the originHashCertVote to set
	 */
	public void setoriginHashCertVote(String originHashCertVote) {
		this.originHashCertVote = originHashCertVote;
	}

	/**
	 * @return the originHashAccessRequest
	 */
	public String getoriginHashAccessRequest() {
		return originHashAccessRequest;
	}

	/**
	 * @param originHashAccessRequest the originHashAccessRequest to set
	 */
	public void setoriginHashAccessRequest(String originHashAccessRequest) {
		this.originHashAccessRequest = originHashAccessRequest;
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
	 * @return the eventId
	 */
	public String getEventVSId() {
		return eventId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventVSId(String eventId) {
		this.eventId = eventId;
	}
	
	public JSONObject toJSON() {
		logger.debug("toJSON");
		Map map = new HashMap();
		map.put("originHashCertVote", originHashCertVote);
		map.put("hashCertVoteBase64", hashCertVoteBase64);
		map.put("originHashAccessRequest", originHashAccessRequest);
		map.put("hashAccessRequestBase64", hashAccessRequestBase64);
		map.put("UUID", UUID.randomUUID().toString());
		JSONObject jsonObject = new JSONObject(map);
		return jsonObject;
	}
	
	public static AccessRequestBackup parse(String strSolicitud) throws Exception {
		logger.debug("parse ");
		AccessRequestBackup solicitud = new AccessRequestBackup();
		JSONObject requestJSON = new JSONObject(strSolicitud);
		if(requestJSON.containsKey("hashAccessRequestBase64"))
			solicitud.setAccessRequestHashBase64(requestJSON.getString("hashAccessRequestBase64"));
		if(requestJSON.containsKey("originHashAccessRequest"))
			solicitud.setoriginHashAccessRequest(
					requestJSON.getString("originHashAccessRequest"));
		if(requestJSON.containsKey("hashCertVoteBase64"))
			solicitud.setHashCertVoteBase64(
					requestJSON.getString("hashCertVoteBase64"));
		if(requestJSON.containsKey("originHashCertVote"))
			solicitud.setoriginHashCertVote(
					requestJSON.getString("originHashCertVote"));
		return solicitud;
	}
}