package org.votingsystem.accesscontrol.service

import org.votingsystem.model.CertificateVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.KeyStoreVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.KeyStoreUtil

import java.security.KeyStore
import java.security.cert.Certificate
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class KeyStoreService {
	
	def grailsApplication
 
    def generateElectionKeysStore(EventVSElection eventVS) {
		ResponseVS responseVS
		KeyStoreVS keyStoreVS = KeyStoreVS.findWhere(valid:Boolean.TRUE, eventVS:eventVS)
		if (keyStoreVS) {
			log.error ("Ya se había generado el almacén de claves de CA del eventVS: '${eventVS.getId()}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST)
		} 
		//String password = (1..7).inject("") { a, b -> a += ('a'..'z')[new Random().nextFloat() * 26 as int] }.toUpperCase()
		// _ TODO _ ====== crypto token
		String password = "${grailsApplication.config.VotingSystem.signKeysPassword}"
		String eventVSUrl = "${grailsApplication.config.grails.serverURL}/eventVS/${eventVS.id}";
		String strSubjectDNRoot = "CN=eventVSUrl:${eventVSUrl}, OU=Elections";
		String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
		KeyStore keyStore = KeyStoreUtil.createRootKeyStore(eventVS.dateBegin, eventVS.dateFinish, password.toCharArray(),
                keyAlias, strSubjectDNRoot);
		Certificate[] chain = keyStore.getCertificateChain(keyAlias);
		Certificate cert = chain[0]
		CertificateVS certificateVS = new CertificateVS(type:CertificateVS.Type.VOTEVS_ROOT,
			content:cert.getEncoded(), state:CertificateVS.State.OK, serialNumber:cert.getSerialNumber().longValue(),
			validFrom:cert.getNotBefore(), validTo:cert.getNotAfter(), eventVSElection:eventVS)
		certificateVS.save();
		keyStoreVS = new KeyStoreVS (isRoot:Boolean.TRUE, valid:Boolean.TRUE, keyAlias:keyAlias,
			eventVS:eventVS, validFrom:eventVS.dateBegin, validTo:eventVS.dateFinish,
			bytes: KeyStoreUtil.getBytes(keyStore, password.toCharArray()))
		keyStoreVS.save()
		responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, data:cert)
		log.debug ("Saved KeyStoreVS '${keyStoreVS.getId()}' for event '${eventVS.getId()}'")
		return responseVS
    }
	
	public ResponseVS cancel (EventVSElection eventVS) {
		KeyStoreVS keyStoreVS = KeyStoreVS.findWhere(valid:Boolean.TRUE, eventVS:eventVS)
		if (!keyStoreVS) {
			log.debug ("El '${eventVS.getId()}' doesn't have KeyStoreVS associated")
			return new ResponseVS(ResponseVS.SC_NOT_FOUND)
		}
		keyStoreVS.valid = false;
		keyStoreVS.save()
        return new ResponseVS(ResponseVS.SC_OK)
	}

}
