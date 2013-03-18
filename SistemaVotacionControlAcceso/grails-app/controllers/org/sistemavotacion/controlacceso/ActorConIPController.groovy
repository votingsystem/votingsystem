package org.sistemavotacion.controlacceso

import org.sistemavotacion.smime.*
import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import java.util.Arrays

/**
 * @infoController Actores de la aplicación
 * @descController Servicios relacionados con la gestión de los actores que 
 * intervienen en la aplicación.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class ActorConIPController {
    
    def subscripcionService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/actorConIP'.
	 */
	def index() { }
	
	/**
	 * Servicio que da de baja un actor.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos del
	 * 		  actor que se desea dar de baja.
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
    def guardarSolicitudDesactivacion () { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
        Respuesta respuesta
        if (params.long('actorConIPId')) {
            ActorConIP actorConIP = ActorConIP.get(mensajeJSON.actorConIPId)
            if (!actorConIP) {
                respuesta = new Respuesta(codigoEstado:Respuesta.SC_NOT_FOUND, tipo: Tipo.ERROR, 
                    mensaje:message(code: 'evento.actorConIPNotFound', args:[mensajeJSON.actorConIPId]))
            } else {
                List<String> administradores = Arrays.asList(
                    grailsApplication.config.SistemaVotacion.adminsDNI.split(",")) 
				Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(
					params.smimeMessageReq, request.getLocale())
				if(Respuesta.SC_OK != respuestaUsuario.codigoEstado) {
					response.status = respuestaUsuario.codigoEstado
					render respuestaUsuario.mensaje
					return false
				}
                Usuario usuario = respuestaUsuario.usuario           
                if (administradores.contains(usuario.nif)) {
                    log.debug "Usuario en la lista de administradoreas, desactivando actor"
                    actorConIP.estado = ActorConIP.Estado.SUSPENDIDO
                    actorConIP.save()
                    respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, tipo: Tipo.OK)
                } else {
                    log.debug "Usuario no esta en la lista de administradoreas, petición denegada"
                    respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
                        mensaje:message(code: 'error.UsuarioNoAdministrador'), tipo: Tipo.ERROR)
                }    
            }  
        } else {
			response.status = Respuesta.SC_ERROR_PETICION
            render(view:"index")
			return
        }
        response.status = respuesta.codigoEstado
        render respuesta.mensaje
        return false
    }
    
	/**
	 * Servicio que da de alta un actor.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos del
	 * 		  actor que se desea dar de baja.
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
    def guardarSolicitudActivacion () { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
        Respuesta respuesta
        if (params.long('actorConIPId')) {
            ActorConIP actorConIP = ActorConIP.get(mensajeJSON.actorConIPId)
            if (!actorConIP) {
                respuesta = new Respuesta(codigoEstado:Respuesta.SC_NOT_FOUND, tipo: Tipo.ERROR, 
                    mensaje:message(code: 'evento.actorConIPNotFound', args:[mensajeJSON.actorConIPId]))
            } else {
                List<String> administradores = Arrays.asList(
                    grailsApplication.config.SistemaVotacion.adminsDNI.split(",")) 
				Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(
					smimeMessageReq, request.getLocale())
				if(Respuesta.SC_OK != respuestaUsuario.codigoEstado) {
					response.status = respuestaUsuario.codigoEstado
					render respuestaUsuario.mensaje
					return false
				}
				Usuario usuario = respuestaUsuario.usuario
                if (administradores.contains(usuario.nif)) {
                    actorConIP.estado = ActorConIP.Estado.ACTIVO
                    actorConIP.save()
                    respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, tipo: Tipo.OK)
                } else {
                    respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
                        mensaje:message(code: 'error.UsuarioNoAdministrador'), tipo: Tipo.ERROR)
                }    
            } 
        } else {
            respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
                mensaje:message(code: 'error.PeticionIncorrectaHTML', args:[
					"${grailsApplication.config.grails.serverURL}/${params.controller}"]), tipo: Tipo.ERROR)
        }
        response.status = respuesta.codigoEstado
		render respuesta.mensaje
        return false
    }
    
}