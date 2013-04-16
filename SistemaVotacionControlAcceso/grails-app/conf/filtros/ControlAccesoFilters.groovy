package filtros

import org.sistemavotacion.utils.StringUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.smime.*
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* 
*/
class ControlAccesoFilters {

    def firmaService
	def encryptionService
    def grailsApplication
	def messageSource

    def filters = {
        String nombreEntidadFirmada = grailsApplication.config.SistemaVotacion.nombreEntidadFirmada;
        paramsCheck(controller:'*', action:'*') {
            before = {
                log.debug "###########################<${params.controller}> - before ###################################"
                log.debug "Method: " + request.method +"Params: " + params
                if(!params.archivoFirmado) log.debug "Params: " + params
                log.debug "getRemoteHost: " + request.getRemoteHost()
                log.debug "Request: " + request.getRequestURI()  + " - RemoteAddr: " + request.getRemoteAddr()
                log.debug "User agent: " + request.getHeader("User-Agent")
                log.debug "-----------------------------------------------------------------------------------"
				if(!params.int("max")) params.max = 20
                if(!params.int("offset")) params.offset = 0
                if(!params.sort) params.sort = "dateCreated"
                if(!params.order) params.order = "desc"
                response.setHeader("Cache-Control", "no-store")
                if(params.id)params.ids = StringUtils.checkIds(params.id)
            }
        }

        guardarFilter(action:'guardar', find:true) {
            before = {
                log.debug "----------------- filter - guardar - before ------------------------------------"
                if (!(request instanceof MultipartHttpServletRequest)) {
                    log.debug "------------- La petición no es instancia de MultipartHttpServletRequest ------------"
					response.status = Respuesta.SC_ERROR_PETICION
					render(messageSource.getMessage(
						'evento.peticionSinArchivo', null, request.getLocale()))
					return false
                }
                try {
                    MultipartFile multipartFile = ((MultipartHttpServletRequest) 
						request)?.getFile(nombreEntidadFirmada);
                    if (multipartFile?.getBytes() != null || params.archivoFirmado) {
						Respuesta respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION)
						if(multipartFile?.getBytes() != null) {
								respuesta = encryptionService.decryptSMIMEMessage(
									multipartFile?.getBytes(), request.getLocale())
						} else if(params.archivoFirmado)
							respuesta = encryptionService.decryptSMIMEMessage(
								params.archivoFirmado.getBytes(), request.getLocale())
						if(Respuesta.SC_OK != respuesta.codigoEstado) {
							response.status = respuesta.codigoEstado
							render respuesta.mensaje
							return false
						}
						SMIMEMessageWrapper smimeMessageReq = respuesta.smimeMessage
                        if (smimeMessageReq && smimeMessageReq.isValidSignature()) {
                            log.debug " - signature OK - "
							Respuesta respuestaValidacionCa = firmaService.
									validarCertificacionFirmantes(smimeMessageReq, request.getLocale())
							if(Respuesta.SC_OK != respuestaValidacionCa.codigoEstado) {
								response.status = respuestaValidacionCa.codigoEstado
								render respuestaValidacionCa.mensaje
								return false
							}  
							params.smimeMessageReq = smimeMessageReq
                            return
                        } else {
                            log.debug " - signature ERROR - "
							response.status = Respuesta.SC_ERROR_PETICION
							render messageSource.getMessage(
								'signatureErrorMsg', null, request.getLocale())
							return false
                        }
                    } else {
						String msg = messageSource.getMessage(
							'evento.peticionSinArchivo', null, request.getLocale())
                        log.debug msg
						response.status = Respuesta.SC_ERROR_PETICION
						render msg
						return false
                    }
                } catch (Exception ex) {
                    log.error (ex.getMessage(), ex)
					response.status = Respuesta.SC_ERROR_PETICION
					render ex.getMessage()
					return false                   
                }
            }

        }
		
        adjuntandoValidacionFilter(action:'AdjuntandoValidacion', find:true) {
            after = {
                def codigoEstado = flash.respuesta?.codigoEstado
                log.debug "-----------------  filter - AdjuntandoValidacion - after - status: ${codigoEstado}-----------------------------------"
                response.status = codigoEstado
                if (Respuesta.SC_OK == flash.respuesta.codigoEstado) {
					MensajeSMIME mensajeSMIMEValidado = flash.respuesta.mensajeSMIMEValidado
					if (mensajeSMIMEValidado) {
						response.contentLength = mensajeSMIMEValidado.contenido.length
						response.setContentType("application/octet-stream")
						response.outputStream << mensajeSMIMEValidado.contenido
						response.outputStream.flush()
						return false
					} else {
						if (flash?.respuesta?.mensaje) render flash.respuesta.mensaje
						else render "ERROR"
						return false
					}
                } else {
					response.setContentType("text/plain")
					if (flash?.respuesta?.mensaje) render flash.respuesta.mensaje
					else render "ERROR"
					return false
                }
            }

        }	

    }

}