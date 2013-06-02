package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import grails.converters.JSON
import java.security.cert.X509Certificate;
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.tsp.TimeStampToken
import org.codehaus.groovy.grails.web.json.JSONElement

class TimeStampService {
	
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	
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
				respuesta = httpService.obtenerCertificado(timeStampCertURL)
				if(Respuesta.SC_OK != respuesta.codigoEstado) {
					msg = messageSource.getMessage('timeStampCertErrorMsg', [timeStampCertURL].toArray(), locale)
					log.error("validateToken - ${msg}")
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
				} else {
					timeStampVerifier = new JcaSimpleSignerInfoVerifierBuilder().
						setProvider(BC).build(respuesta.certificado)
					timeStampVerifiers.put(accessControl.id, timeStampVerifier)
				}
			}
			timeStampToken.validate(timeStampVerifier)
			Date timestampDate = timeStampToken.getTimeStampInfo().getGenTime()
			if(!timestampDate.after(evento.fechaInicio) &&
				!timestampDate.before(evento.fechaFin)) {
				String dateRangeStr = "[${eventoVotacion.fechaInicio} - ${eventoVotacion.fechaFin}]"
				msg = messageSource.getMessage('timestampDateErrorMsg',
					[timestampDate, dateRangeStr].toArray(), locale)
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
