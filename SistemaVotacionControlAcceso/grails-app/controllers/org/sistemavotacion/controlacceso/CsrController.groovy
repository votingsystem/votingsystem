package org.sistemavotacion.controlacceso

import org.sistemavotacion.util.*;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.controlacceso.modelo.*;
import java.security.KeyStore
import java.security.cert.X509Certificate;
import java.security.cert.Certificate
import grails.converters.JSON;

/**
 * @infoController Validación de solicitudes de certificación
 * @descController Servicios relacionados con validación y firma de certificados.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class CsrController {

	def csrService
	def firmaService
	
	/**
	 * @httpMethod [GET]
	 * @return Información sobre los servicios que tienen como url base '/csr'.
	 */
	def index() { 
		redirect action: "restDoc"
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
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
		Respuesta respuesta = csrService.validarCSRUsuario(consulta.getBytes(), request.getLocale())
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false;
	}
	
	/**
	 * Servicio que devuelve las solicitudes de certificados firmadas una vez que
	 * se ha validado la identidad del usuario.
	 *
	 * @httpMethod [GET]
	 * @param idSolicitudCSR Identificador de la solicitud de certificación enviada previamente por el usuario.
	 * @return Si el sistema ha validado al usuario devuelve la solicitud de certificación firmada.
	 */
	def obtener() { 
		SolicitudCSRUsuario solicitudCSR
		SolicitudCSRUsuario.withTransaction {
			solicitudCSR = SolicitudCSRUsuario.findWhere(
				id:params.long('idSolicitudCSR'), estado:SolicitudCSRUsuario.Estado.OK)
		}
		if (solicitudCSR) {
			def certificado = Certificado.findWhere(solicitudCSRUsuario:solicitudCSR)
			if (certificado) {
				response.status = Respuesta.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				
				
				def rutaAlmacenClaves = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
				File keyStoreFile = new File(rutaAlmacenClaves);
				String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
				String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
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
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: "csr.ErrorGeneracionCert")
			return false
		} 
		response.status = Respuesta.SC_NOT_FOUND
		render message(code: "csr.solicitudNoValidada")
		return false
	}
	
	private String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
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
		MensajeSMIME mensajeSMIME = params.mensajeSMIMEReq
		if(!mensajeSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		List<String> administradores = Arrays.asList(
			grailsApplication.config.SistemaVotacion.adminsDNI.split(","))
		Usuario usuario = mensajeSMIME.getUsuario()
		def docValidacionJSON = JSON.parse(mensajeSMIME.getSmimeMessage().getSignedContent())
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIME.getSmimeMessage()
		if (administradores.contains(usuario.nif) || usuario.nif.equals(docValidacionJSON.nif)) {
			String msg = message(code: "csr.usuarioAutorizado", args: [usuario.nif])
			Dispositivo dispositivo = Dispositivo.findWhere(deviceId: docValidacionJSON.deviceId)
			if (!dispositivo?.usuario) {
				response.status = Respuesta.SC_ERROR_PETICION
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
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: "csr.usuarioSinSolicitud", args: [usuarioMovil.nif])
				return false
			}
			Respuesta respuestaValidacionCSR = firmaService.firmarCertificadoUsuario(
				solicitudCSR, request.getLocale())
			if (Respuesta.SC_OK == respuestaValidacionCSR.codigoEstado) {
				response.status = Respuesta.SC_OK
				render message(code: "csr.generacionCertOK")
			} else {
				response.status = respuestaValidacionCSR.codigoEstado
				render respuestaValidacionCSR.mensaje
			}
			return false
		} else {
			String msg = message(code: "csr.usuarioNoAutorizado", args: [usuario.nif])
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
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
		String consulta = "${request.getInputStream()}"
		if (!consulta) {
			response.status = Respuesta.SC_ERROR_PETICION
			render(view:"index")
			return false	
		}
		log.debug ("consulta: ${consulta}")
		def consultaJSON = JSON.parse(consulta)
		Dispositivo dispositivo = Dispositivo.findByDeviceId(consultaJSON?.deviceId?.trim())
		if (!dispositivo) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: "csr.solicitudNoEncontrada", args: 
				["deviceId: ${consultaJSON?.deviceId}"])
			return false
		}
		Usuario usuario
		String nifValidado = StringUtils.validarNIF(consultaJSON?.nif)
		if(nifValidado) usuario = Usuario.findByNif(nifValidado)
		if (!usuario) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: "csr.solicitudNoEncontrada", args: ["nif: ${nifValidado}"])
			return false
		}
		SolicitudCSRUsuario solicitudCSR
		SolicitudCSRUsuario.withTransaction{
			solicitudCSR = SolicitudCSRUsuario.findByDispositivoAndUsuarioAndEstado(
				dispositivo, usuario, SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION);
		}
		if (!solicitudCSR) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: "csr.solicitudNoEncontrada", args: [consulta])
			return false
		}
		Respuesta respuestaValidacionCSR = firmaService.firmarCertificadoUsuario(
			solicitudCSR, request.getLocale())
		if (Respuesta.SC_OK == respuestaValidacionCSR.codigoEstado) {
			response.status = Respuesta.SC_OK
			render message(code: "csr.generacionCertOK")
		} else {
			response.status = respuestaValidacionCSR.codigoEstado
			render respuestaValidacionCSR.mensaje
		}
		return false
	}
	
}