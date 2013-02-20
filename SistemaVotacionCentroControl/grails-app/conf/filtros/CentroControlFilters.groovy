package filtros

import org.sistemavotacion.utils.StringUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.smime.*
import javax.mail.internet.MimeMessage
import org.springframework.web.context.request.RequestContextHolder
import java.security.cert.PKIXParameters;
import org.sistemavotacion.smime.SignedMailValidator.ValidationResult;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* 
* */
class CentroControlFilters {

    def grailsApplication    

    def filters = {
        String nombreEntidadFirmada = grailsApplication.config.SistemaVotacion.nombreEntidadFirmada;
        
        paramsCheck(controller:'*', action:'*') {
            before = {
                log.debug "###########################<${params.controller}>###################################"
                //log.debug "Method: " + request.method + " -Params: " + params 
				log.debug "Method: " + request.method
                log.debug "getRemoteHost: " + request.getRemoteHost()
                log.debug "Request: " + request.getRequestURI()  + " - RemoteAddr: " + request.getRemoteAddr()
                log.debug "User agent: " + request.getHeader("User-Agent")
                log.debug "-----------------------------------------------------------------------------------"
				if(!params.int("max")) params.max = 20
                if(!params.int("offset")) params.offset = 0
                if(!params.sort) params.sort = "dateCreated"
                if(!params.order) params.order = "desc"
                response.setHeader("Cache-Control", "no-store")
                params.ids = StringUtils.checkIds(params.id)
				log.debug " -Params: " + params 
            }
        }
		
		
		guardarFilter(action:'guardar', find:true) {
			before = {
				log.debug "----------------- filter - guardar - before ------------------------------------"
				if (!(request instanceof MultipartHttpServletRequest)) {
					log.debug "------------- La petición no es instancia de MultipartHttpServletRequest ------------"
					flash.respuesta = new Respuesta(codigoEstado:400,
						tipo: Tipo.PETICION_SIN_ARCHIVO)
					forward controller: "error400", action: "procesar"   
					return false
				}
				try {
					MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(nombreEntidadFirmada);
					if (multipartFile?.getBytes() != null || params.archivoFirmado) {
						SMIMEMessageWrapper smimeMessageReq
						try {
							if (params.archivoFirmado) smimeMessageReq = SMIMEMessageWrapper.build(
									new ByteArrayInputStream(params.archivoFirmado.getBytes()), null);
							else smimeMessageReq = SMIMEMessageWrapper.build(
									new ByteArrayInputStream(multipartFile?.getBytes()), null)
						} catch (Exception ex) {
							log.error (ex.getMessage(), ex)
							flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
								codigoEstado:500, tipo: Tipo.PETICION_CON_ERRORES)
							forward controller: "error500", action: "procesar"   
							return false
						}
						if (smimeMessageReq.isValidSignature()) {
							log.debug "firma valida"
							params.smimeMessageReq = smimeMessageReq
							return
						} else {
							log.debug "firma erronea"
							flash.respuesta = new Respuesta(codigoEstado:400,
								tipo: Tipo.FIRMA_EVENTO_CON_ERRORES)
							forward controller: "error400", action: "procesar"   
							return false
						}
					} else {
						log.debug "Peticion sin archivo"
						flash.respuesta = new Respuesta(codigoEstado:400,
							tipo: Tipo.PETICION_SIN_ARCHIVO)
						forward controller: "error400", action: "procesar"   
						return false
					}
				} catch (Exception ex) {
					log.error (ex.getMessage(), ex)
					flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
					codigoEstado:500, tipo: Tipo.PETICION_CON_ERRORES)
					forward controller: "error500", action: "procesar"   
					return false
				}
			}

		}

    }

}