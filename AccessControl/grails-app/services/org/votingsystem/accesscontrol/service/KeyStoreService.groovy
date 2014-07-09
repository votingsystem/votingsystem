package org.votingsystem.accesscontrol.service

import org.votingsystem.model.*
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.FileUtils

import javax.security.auth.x500.X500PrivateCredential
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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

    def ResponseVS generateUserTestKeysStore(String givenName, String surname, String nif, String userPassword) {
        log.debug (" generateUserTestKeysStore - givenName: ${givenName} - surname: ${surname} - nif: ${nif}")
        if(EnvironmentVS.DEVELOPMENT  !=  ApplicationContextHolder.getEnvironment()) {
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                    message:"ERROR Service available only for DEVELOPMENT environment- Application environment: " +
                            "${ApplicationContextHolder.getEnvironment()}")
        } else {
            Date validFrom = Calendar.getInstance().getTime();
            Calendar today_plus_year = Calendar.getInstance();
            today_plus_year.add(Calendar.YEAR, 1);
            today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
            today_plus_year.set(Calendar.MINUTE, 0);
            today_plus_year.set(Calendar.SECOND, 0);
            Date validTo = today_plus_year.getTime()
            File keyStoreFile = grailsApplication.mainContext.getResource(
                    grailsApplication.config.VotingSystem.keyStorePath).getFile()
            String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
            String password = grailsApplication.config.VotingSystem.signKeysPassword
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
                    FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
            PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
            X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
            X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKeySigner,  keyAlias);
;
            String testUserDN = "GIVENNAME=${givenName}, SURNAME=${surname} , SERIALNUMBER=${nif}"
            //String strSubjectDN = "CN=Voting System Cert Authority , OU=VotingSystem"
            //KeyStore rootCAKeyStore = KeyStoreUtil.createRootKeyStore (validFrom.getTime(), (validTo.getTime() - validFrom.getTime()),
            //        userPassword.toCharArray(), keyAlias, strSubjectDN);
            //X509Certificate certSigner = (X509Certificate)rootCAKeyStore.getCertificate(keyAlias);
            //PrivateKey privateKeySigner = (PrivateKey)rootCAKeyStore.getKey(keyAlias, userPassword.toCharArray());
            //X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKeySigner,  keyAlias);

            KeyStore userKeyStore = KeyStoreUtil.createUserKeyStore(validFrom.getTime(),
                    (validTo.getTime() - validFrom.getTime()), userPassword.toCharArray(), ContextVS.KEYSTORE_USER_CERT_ALIAS,
                    rootCAPrivateCredential, testUserDN);
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:userKeyStore)
        }
    }

	public ResponseVS cancel (EventVSElection eventVS) {
		KeyStoreVS keyStoreVS = KeyStoreVS.findWhere(valid:Boolean.TRUE, eventVS:eventVS)
		if (!keyStoreVS) {
			log.debug ("Event '${eventVS.getId()}' doesn't have KeyStoreVS associated")
			return new ResponseVS(ResponseVS.SC_NOT_FOUND)
		}
		keyStoreVS.valid = false;
		keyStoreVS.save()
        return new ResponseVS(ResponseVS.SC_OK)
	}

}
