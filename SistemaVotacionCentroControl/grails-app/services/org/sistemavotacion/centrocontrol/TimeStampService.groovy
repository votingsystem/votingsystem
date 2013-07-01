package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.seguridad.*;
import grails.converters.JSON
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.tsp.TimeStampToken
import org.codehaus.groovy.grails.web.json.JSONElement
import java.security.cert.X509Certificate;

class TimeStampService {
	
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	
	private static final int numMaxAttempts = 3;
	
	def grailsApplication
	def messageSource
	def httpService

	private static final HashMap<Long, SignerInformationVerifier> timeStampVerifiers =
			new HashMap<Long, Set<X509Certificate>>();
			
	public void afterPropertiesSet() throws Exception {}
	
	public Respuesta validateToken(TimeStampToken timeStampToken, 
		EventoVotacion evento, Locale locale) throws Exception {
		log.debug("validateToken - event:${evento.id}")
		String msg = null;
		Respuesta respuesta
		try {
			if(!timeStampToken) {
				msg = messageSource.getMessage('timeStampNullErrorMsg', null, locale)
				log.error("ERROR - validateToken - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_NULL_REQUEST, mensaje:msg)
			}
			ControlAcceso accessControl = evento.controlAcceso
			SignerInformationVerifier timeStampVerifier = timeStampVerifiers.get(accessControl.id)
			if(!timeStampVerifier) {
				String accessControlURL = accessControl.serverURL
				while(accessControlURL.endsWith("/")) {
					accessControlURL = accessControlURL.substring(0, accessControlURL.length() - 1)
				}
				String timeStampCertURL = "${accessControlURL}/timeStamp/cert"
				respuesta = httpService.getInfo(timeStampCertURL, null)
				if(Respuesta.SC_OK != respuesta.codigoEstado) {
					msg = messageSource.getMessage('timeStampCertErrorMsg', [timeStampCertURL].toArray(), locale)
					log.error("validateToken - ${msg}")
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
				} else {
					X509Certificate timeStampCert = CertUtil.fromPEMToX509Cert(respuesta.messageBytes)
					timeStampVerifier = new JcaSimpleSignerInfoVerifierBuilder().
						setProvider(BC).build(timeStampCert)
					timeStampVerifiers.put(accessControl.id, timeStampVerifier)
				}
			}
			
			AtomicBoolean done = new AtomicBoolean(false);
			int numAttemp = 0;
			while(!done.get()) {
				try {
					timeStampToken.validate(timeStampVerifier)
					done.set(true)
				} catch(Exception ex) {
					if(numAttemp < numMaxAttempts) {
						++numAttemp;
					} else {
						log.error(" ------ Exceeded max num attemps");
						throw ex;
					}
				}
			}
			
			Date timestampDate = timeStampToken.getTimeStampInfo().getGenTime()
			if(!timestampDate.after(evento.fechaInicio) ||
				!timestampDate.before(evento.getDateFinish())) {
				msg = messageSource.getMessage('timestampDateErrorMsg',
					[timestampDate, evento.fechaInicio, evento.getDateFinish()].toArray(), locale)
				log.debug("validateToken - ERROR TIMESTAMP DATE -  - Event '${evento.id}' - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, evento:evento)
			} else return new Respuesta(codigoEstado:Respuesta.SC_OK);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('timeStampErrorMsg', null, locale)
			log.error ("validateToken - msg:{msg} - Event '${evento.id}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
		}
	}

	
}
