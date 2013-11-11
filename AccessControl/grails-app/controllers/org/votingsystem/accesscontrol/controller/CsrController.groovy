package org.votingsystem.accesscontrol.controller

import org.votingsystem.util.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.accesscontrol.model.*;

import java.security.KeyStore
import java.security.cert.X509Certificate;
import java.security.cert.Certificate

import grails.converters.JSON;

import org.votingsystem.groovy.util.*

/**
 * @infoController Validación de solicitudes de certificación
 * @descController Servicios relacionados con validación y firma de certificados.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class CsrController {

	def csrService
	
	/**
	 * Servicio que devuelve las solicitudes de certificados firmadas una vez que
	 * se ha validado la identidad del usuario.
	 *
	 * @httpMethod [GET]
	 * @param idSolicitudCSR Identificador de la solicitud de certificación enviada previamente por el usuario.
	 * @return Si el sistema ha validado al usuario devuelve la solicitud de certificación firmada.
	 */
	def index() { 
		SolicitudCSRUsuario solicitudCSR
		SolicitudCSRUsuario.withTransaction {
			solicitudCSR = SolicitudCSRUsuario.findWhere(
				id:params.long('idSolicitudCSR'), estado:SolicitudCSRUsuario.Estado.OK)
		}
		if (solicitudCSR) {
			def certificado = Certificado.findWhere(solicitudCSRUsuario:solicitudCSR)
			if (certificado) {
				response.status = ResponseVS.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				File keyStoreFile = grailsApplication.mainContext.getResource(
					grailsApplication.config.VotingSystem.keyStorePath).getFile()
				
				String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
				String password = grailsApplication.config.VotingSystem.signKeysPassword
				KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
					FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
				Certificate[] certsServer =  keyStore.getCertificateChain(aliasClaves);
				
				List<X509Certificate> certs =new ArrayList<X509Certificate>();
				certs.add(certX509);
				for(Certificate cert : certsServer) {
					certs.add((X509Certificate)cert);
				}
				
				log.debug("certs.size(): " + certs.size())
				
				
				byte[] pemCert = CertUtil.fromX509CertCollectionToPEM(certs)
				
				//byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.setContentType("text/plain")
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: "csr.ErrorGeneracionCert")
			return false
		} 
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: "csr.solicitudNoValidada")
		return false
	}
	
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS)<br/>
	 * Servicio para la creación de certificados de usuario.
	 * 
	 * @httpMethod [POST]
	 * @param csr Solicitud de certificación con los datos de usuario.
	 * @return Si todo es correcto devuelve un código de estado HTTP 200 y el identificador 
	 *         de la solicitud en la base de datos.
	 */
	def solicitar() {
		String consulta = "${request.getInputStream()}"
		if (!consulta) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
		ResponseVS respuesta = csrService.saveUserCSR(consulta.getBytes(), request.getLocale())
		response.status = respuesta.statusCode
		render respuesta.message
		return false;
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
	 *
	 * Servicio que firma solicitudes de certificación de usuario.<br/>
	 *
	 * TODO - Hacer las validaciones sólo sobre solicitudes firmadas electrónicamente
	 * por personal dado de alta en la base de datos.
	 *
	 * @httpMethod [POST]
	 * @requestContentType [application/x-pkcs7-signature] Obligatorio. Documento 
	 * firmadoArchivo firmado en formato SMIME en cuyo contenido 
	 * se encuentran los datos de la solicitud que se desea validar.
	 * <code>{deviceId:"000000000000000", telefono:"15555215554", nif:"1R" }</code>
	 * 
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
	def validacion() { 
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		List<String> administradores = Arrays.asList(
			grailsApplication.config.VotingSystem.adminsDNI.split(","))
		Usuario usuario = messageSMIME.getUsuario()
		def docValidacionJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
		SMIMEMessageWrapper smimeMessageReq = messageSMIME.getSmimeMessage()
		if (administradores.contains(usuario.nif) || usuario.nif.equals(docValidacionJSON.nif)) {
			String msg = message(code: "csr.usuarioAutorizado", args: [usuario.nif])
			Dispositivo dispositivo = Dispositivo.findWhere(deviceId: docValidacionJSON.deviceId)
			if (!dispositivo?.usuario) {
				response.status = ResponseVS.SC_ERROR_PETICION
				render message(code: "csr.solicitudNoEncontrada", args: [smimeMessageReq.getSignedContent()])
				return false
			}
			if(dispositivo.usuario.nif != usuario.nif) {
				log.debug "Usuario con nif:'${usuario.nif}' intentando validar dispositivo:" + 
					"'${dispositivo.id}' con nif:'${dispositivo.usuario.nif}'"
				render message(code: "csr.usuarioNoAutorizado")
				return false
			}
			SolicitudCSRUsuario solicitudCSR = SolicitudCSRUsuario.findWhere(usuario:dispositivo.usuario,
				estado:SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION);
			if (!solicitudCSR) {
				response.status = ResponseVS.SC_ERROR_PETICION
				render message(code: "csr.usuarioSinSolicitud", args: [usuarioMovil.nif])
				return false
			}
			ResponseVS respuestaValidacionCSR = csrService.firmarCertificadoUsuario(
				solicitudCSR, request.getLocale())
			if (ResponseVS.SC_OK == respuestaValidacionCSR.statusCode) {
				response.status = ResponseVS.SC_OK
				render message(code: "csr.generacionCertOK")
			} else {
				response.status = respuestaValidacionCSR.statusCode
				render respuestaValidacionCSR.message
			}
			return false
		} else {
			String msg = message(code: "csr.usuarioNoAutorizado", args: [usuario.nif])
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
	}
	
	/**
     * ==================================================
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	 * ==================================================
	 * 
	 * Servicio que firma solicitudes de certificación de usuario.<br/>
	 * 
	 * @httpMethod [POST]
	 * @requestContentType [application/json] Documento JSON con los datos del usuario 
	 * <code>{deviceId:"000000000000000", telefono:"15555215554", nif:"1R" }</code>
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
	def validar() {
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		String consulta = "${request.getInputStream()}"
		if (!consulta) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render(view:"index")
			return false	
		}
		log.debug ("consulta: ${consulta}")
		def consultaJSON = JSON.parse(consulta)
		Dispositivo dispositivo = Dispositivo.findByDeviceId(consultaJSON?.deviceId?.trim())
		if (!dispositivo) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render message(code: "csr.solicitudNoEncontrada", args: 
				["deviceId: ${consultaJSON?.deviceId}"])
			return false
		}
		Usuario usuario
		String nifValidado = StringUtils.validarNIF(consultaJSON?.nif)
		if(nifValidado) usuario = Usuario.findByNif(nifValidado)
		if (!usuario) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render message(code: "csr.solicitudNoEncontrada", args: ["nif: ${nifValidado}"])
			return false
		}
		SolicitudCSRUsuario solicitudCSR
		SolicitudCSRUsuario.withTransaction{
			solicitudCSR = SolicitudCSRUsuario.findByDispositivoAndUsuarioAndEstado(
				dispositivo, usuario, SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION);
		}
		if (!solicitudCSR) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render message(code: "csr.solicitudNoEncontrada", args: [consulta])
			return false
		}
		ResponseVS respuestaValidacionCSR = csrService.firmarCertificadoUsuario(
			solicitudCSR, request.getLocale())
		if (ResponseVS.SC_OK == respuestaValidacionCSR.statusCode) {
			response.status = ResponseVS.SC_OK
			render message(code: "csr.generacionCertOK")
		} else {
			response.status = respuestaValidacionCSR.statusCode
			render respuestaValidacionCSR.message
		}
		return false
	}
	
}