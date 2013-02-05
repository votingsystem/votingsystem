package org.sistemavotacion.controlacceso

import org.sistemavotacion.util.*;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.controlacceso.modelo.*;
import java.security.cert.X509Certificate;
import grails.converters.JSON;

class CsrController {

	def csrService
	def subscripcionService
	def firmaService
	
    def index() { }
	
	def solicitar() {
		String consulta = StringUtils.getStringFromInputStream(request.getInputStream())
		if (!consulta) {
			render(view:"index")
			return false
		}
		Respuesta respuesta = csrService.validarCSRUsuario(consulta.getBytes(), request.getLocale())
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false;
	}
	
	def obtener() { 
		SolicitudCSRUsuario solicitudCSR
		SolicitudCSRUsuario.withTransaction {
			solicitudCSR = SolicitudCSRUsuario.findWhere(
				id:params.long('idSolicitudCSR'), estado:SolicitudCSRUsuario.Estado.OK)
		}
		if (solicitudCSR) {
			def certificado = Certificado.findWhere(solicitudCSRUsuario:solicitudCSR)
			if (certificado) {
				response.status = 200
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.setContentType("text/plain")
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = 404
			render message(code: "csr.ErrorGeneracionCert")
			return false
		} 
		response.status = 404
		render message(code: "csr.solicitudNoValidada")
		return false
	}
	
	def guardarValidacion() { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
		List<String> administradores = Arrays.asList(
			grailsApplication.config.SistemaVotacion.adminsDNI.split(","))
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(
			params.smimeMessageReq, request.getLocale())
		if(200 != respuestaUsuario.codigoEstado) {
			response.status = respuestaUsuario.codigoEstado
			render respuestaUsuario.mensaje
			return false
		}
		Usuario usuario = respuestaUsuario.usuario
		def docValidacionJSON = JSON.parse(smimeMessageReq.getSignedContent())
		if (administradores.contains(usuario.nif) || usuario.nif.equals(docValidacionJSON.nif)) {
			String msg = message(code: "csr.usuarioAutorizado", args: [usuario.nif])
			Dispositivo dispositivo = Dispositivo.findWhere(deviceId: docValidacionJSON.deviceId)
			if (!dispositivo?.usuario) {
				response.status = 400
				render message(code: "csr.solicitudNoEncontrada", args: [smimeMessageReq.getSignedContent()])
				return false
			}
			if(dispositivo.usuario.nif != usuario.nif) {
				log.debug "Usuario con nif:'${usuario.nif}' intentando validar dispositivo:'${dispositivo.id}' con nif:'${dispositivo.usuario.nif}'"
				render message(code: "csr.usuarioNoAutorizado")
				return false
			}
			SolicitudCSRUsuario solicitudCSR = SolicitudCSRUsuario.findWhere(usuario:dispositivo.usuario,
				estado:SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION);
			if (!solicitudCSR) {
				response.status = 400
				render message(code: "csr.usuarioSinSolicitud", args: [usuarioMovil.nif])
				return false
			}
			Respuesta respuestaValidacionCSR = firmaService.firmarCertificadoUsuario(
				solicitudCSR, request.getLocale())
			if (200 == respuestaValidacionCSR.codigoEstado) {
				response.status = 200
				render message(code: "csr.generacionCertOK")
			} else {
				response.status = respuestaValidacionCSR.codigoEstado
				render respuestaValidacionCSR.mensaje
			}
			return false
		} else {
			String msg = message(code: "csr.usuarioNoAutorizado", args: [usuario.nif])
			log.debug msg
			response.status = 400
			render msg
			return false
		}
	}
	
	//def guardarSolicitudInformacion() {
	def solicitudInformacion() {
		/*SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(
			params.smimeMessageReq, request.getLocale())
		if(200 != respuestaUsuario.codigoEstado) {
			response.status = respuestaUsuario.codigoEstado
			render respuestaUsuario.mensaje
			return false
		}
		Usuario usuario = respuestaUsuario.usuario*/
		Usuario usuario = Usuario.get(params.id)
		def informacionMap = [usuarioId:usuario?.id, nif:usuario?.nif]
		informacionMap.solicitudesCSR = []
		informacionMap.certificados = []
		//TODO test
		def solicitudesCSRList = SolicitudCSRVoto.
			findAllByUsuarioAndHashCertificadoVotoBase64IsNull(usuario)
		solicitudesCSRList.collect {solicitudItem ->
			def solicitudesCSRMap = [id:solicitudItem.id, estado:solicitudItem.estado?.toString(),
				fechaSolicitud:solicitudItem.dateCreated]
			if (solicitudItem.dispositivo) {
				def dispositivo = solicitudItem.dispositivo
				solicitudesCSRMap.deviceIdDispositivo = dispositivo.deviceId
				if(dispositivo.email) solicitudesCSRMap.emailDispositivo = dispositivo.email
				if(dispositivo.telefono) solicitudesCSRMap.telefonoDispositivo = dispositivo.telefono
			}
			if (SolicitudCSRVoto.Estado.OK.equals(solicitudItem.estado)) {
				Certificado certificado = Certificado.findWhere(solicitudCSR:solicitudItem)
				solicitudesCSRMap.numeroSerieCertificado = certificado.numeroSerie
				solicitudesCSRMap.estadoCertificado = certificado.estado.toString()
				solicitudesCSRMap.fechaEmisionCertificado = certificado.dateCreated
			}
			informacionMap.solicitudesCSR.add(solicitudesCSRMap)
		}
		def certificadosList = Certificado.findAllWhere(usuario:usuario, solicitudCSR:null)
		certificadosList.collect {certificadoItem ->
			def certificadoMap = [id:certificadoItem.id, 
				numeroSerie: certificadoItem.numeroSerie, estado:certificadoItem.estado.toString(), 
				fechaEmision: certificadoItem.dateCreated]
			informacionMap.certificados.add(certificadoMap)
		}
		response.setContentType("application/json")
		render informacionMap as JSON
	}
	
	//'{deviceId:"000000000000000", telefono:"15555215554", nif:"30" }'
	//TODO: este servicio sólo existe para hacer pruebas, no debe estar en un entorno de producción real!!!
	def validar() {
		String consulta = StringUtils.getStringFromInputStream(request.getInputStream())
		if (!consulta) {
			response.status = 400
			render(view:"index")
			return false	
		}
		log.debug ("consulta: ${consulta}")
		def consultaJSON = JSON.parse(consulta)
		Dispositivo dispositivo = Dispositivo.findByDeviceId(consultaJSON?.deviceId?.trim())
		if (!dispositivo) {
			response.status = 400
			render message(code: "csr.solicitudNoEncontrada", args: ["deviceId: ${consultaJSON?.deviceId}"])
			return false
		}
		Usuario usuario
		String nifValidado = StringUtils.validarNIF(consultaJSON?.nif)
		if(nifValidado) usuario = Usuario.findByNif(nifValidado)
		if (!usuario) {
			response.status = 400
			render message(code: "csr.solicitudNoEncontrada", args: ["nif: ${nifValidado}"])
			return false
		}
		SolicitudCSRUsuario solicitudCSR
		SolicitudCSRUsuario.withTransaction{
			solicitudCSR = SolicitudCSRUsuario.findByDispositivoAndUsuarioAndEstado(
				dispositivo, usuario, SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION);
		}
		if (!solicitudCSR) {
			response.status = 400
			render message(code: "csr.solicitudNoEncontrada", args: [consulta])
			return false
		}
		Respuesta respuestaValidacionCSR = firmaService.firmarCertificadoUsuario(
			solicitudCSR, request.getLocale())
		if (200 == respuestaValidacionCSR.codigoEstado) {
			response.status = 200
			render message(code: "csr.generacionCertOK")
		} else {
			response.status = respuestaValidacionCSR.codigoEstado
			render respuestaValidacionCSR.mensaje
		}
		return false
	}
	
	//TODO servicio para anular certificados de usuario
	def anular() {
		String consulta = StringUtils.getStringFromInputStream(request.getInputStream())
		if (!consulta) {
			render(view:"index")
			return false
		}
		
	}
	
}