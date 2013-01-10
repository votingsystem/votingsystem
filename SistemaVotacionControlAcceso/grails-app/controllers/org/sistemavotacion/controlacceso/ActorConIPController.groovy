package org.sistemavotacion.controlacceso

import org.sistemavotacion.smime.*
import org.sistemavotacion.controlacceso.modelo.*

import grails.converters.JSON
import java.util.Arrays

class ActorConIPController {
    
    def subscripcionService

	def index() { }
	
    def guardarSolicitudDesactivacion = { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
        Respuesta respuesta
        if (params.long('actorConIPId')) {
            ActorConIP actorConIP = ActorConIP.get(mensajeJSON.actorConIPId)
            if (!actorConIP) {
                respuesta = new Respuesta(codigoEstado:404, tipo: Tipo.ERROR, 
                    mensaje:message(code: 'evento.actorConIPNotFound', args:[mensajeJSON.actorConIPId]))
            } else {
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
                if (administradores.contains(usuario.nif)) {
                    log.debug "Usuario en la lista de administradoreas, desactivando actor"
                    actorConIP.estado = ActorConIP.Estado.SUSPENDIDO
                    actorConIP.save()
                    respuesta = new Respuesta(codigoEstado:200, tipo: Tipo.OK)
                } else {
                    log.debug "Usuario no esta en la lista de administradoreas, petición denegada"
                    respuesta = new Respuesta(codigoEstado:400, 
                        mensaje:message(code: 'error.UsuarioNoAdministrador'), tipo: Tipo.ERROR)
                }    
            }  
        } else {
			response.status = 400
            render(view:"index")
			return
        }
        response.status = respuesta.codigoEstado
        render respuesta.getMap() as JSON
        return false
    }
    
    def guardarSolicitudActivacion = { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
        Respuesta respuesta
        if (params.long('actorConIPId')) {
            ActorConIP actorConIP = ActorConIP.get(mensajeJSON.actorConIPId)
            if (!actorConIP) {
                respuesta = new Respuesta(codigoEstado:404, tipo: Tipo.ERROR, 
                    mensaje:message(code: 'evento.actorConIPNotFound', args:[mensajeJSON.actorConIPId]))
            } else {
                List<String> administradores = Arrays.asList(
                    grailsApplication.config.SistemaVotacion.adminsDNI.split(",")) 
				Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(
					smimeMessageReq, request.getLocale())
				if(200 != respuestaUsuario.codigoEstado) {
					response.status = respuestaUsuario.codigoEstado
					render respuestaUsuario.mensaje
					return false
				}
				Usuario usuario = respuestaUsuario.usuario
                if (administradores.contains(usuario.nif)) {
                    actorConIP.estado = ActorConIP.Estado.ACTIVO
                    actorConIP.save()
                    respuesta = new Respuesta(codigoEstado:200, tipo: Tipo.OK)
                } else {
                    respuesta = new Respuesta(codigoEstado:400, 
                        mensaje:message(code: 'error.UsuarioNoAdministrador'), tipo: Tipo.ERROR)
                }    
            } 
        } else {
            respuesta = new Respuesta(codigoEstado:400, 
                mensaje:message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"]), tipo: Tipo.ERROR)
        }
        response.status = respuesta.codigoEstado
        render respuesta.getMap() as JSON
        return false
    }
    
}