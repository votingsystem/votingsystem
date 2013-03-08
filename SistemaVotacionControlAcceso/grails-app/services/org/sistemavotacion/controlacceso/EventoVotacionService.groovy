package org.sistemavotacion.controlacceso

import java.io.File;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.util.*;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import javax.mail.Header;
import javax.mail.internet.MimeMessage;
import java.security.cert.X509Certificate;
import java.util.Locale;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class EventoVotacionService {	

    def etiquetaService
    def subscripcionService
    def opcionDeEventoService
    def firmaService
    def eventoService
    def grailsApplication
	def almacenClavesService
	def httpService
	def messageSource

    Respuesta guardarEvento(SMIMEMessageWrapper smimeMessage, Locale locale) {
        log.debug("guardarEvento - mensaje: ${smimeMessage.getSignedContent()}")
        Tipo tipo
        int codigoEstado
        CentroControl centroControl
        boolean isValidSignature = smimeMessage.isValidSignature()
        if (isValidSignature) tipo = Tipo.EVENTO_VOTACION
        else tipo = Tipo.EVENTO_VOTACION_ERROR
        def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
        Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		if(Respuesta.SC_OK != respuestaUsuario.codigoEstado) return respuestaUsuario
		Usuario usuario = respuestaUsuario.usuario
        MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipo,
                usuario:usuario, valido:isValidSignature,
                contenido:smimeMessage.getBytes()) 
        EventoVotacion evento = new EventoVotacion(asunto:mensajeJSON.asunto, 
            contenido:mensajeJSON.contenido, usuario:usuario,
                fechaInicio: new Date().parse(
					"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaInicio),
                fechaFin: new Date().parse(
					"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaFin))
        evento.estado = eventoService.obtenerEstadoEvento(evento)
        if(mensajeJSON.cardinalidad) evento.cardinalidadOpciones = 
				Evento.Cardinalidad.valueOf(mensajeJSON.cardinalidad)
        else evento.cardinalidadOpciones = Evento.Cardinalidad.UNA
		evento.save()
		evento.url = "${grailsApplication.config.grails.serverURL}" +
			"${grailsApplication.config.SistemaVotacion.sufijoURLEventoVotacion}${evento.id}"
        if (mensajeJSON.centroControl) {
            evento.centroControl = subscripcionService.comprobarCentroControl(mensajeJSON.centroControl.serverURL)
			evento.save(flush:true)
			Respuesta respuesta = httpService.obtenerCadenaCertificacion(evento.centroControl.serverURL, locale);
			byte[] cadenaCertificacion;
			if (Respuesta.SC_OK == respuesta.codigoEstado) {
				evento.cadenaCertificacionCentroControl = respuesta.cadenaCertificacion
				log.debug("Obtenida cadena de certificacion del CentroControl para el evento: ${evento.id}")
			} else {
				respuesta.mensaje =  messageSource.getMessage('http.ErrorObteniendoCadenaCertificacion', null, locale) + 
					" - " + evento.centroControl.serverURL + " - " + respuesta.mensaje
				return respuesta
			}				
        } else if (!(mensajeJSON.centroControl) || !centroControl){
			log.debug("solicitud sin centro de control")
            codigoEstado = Respuesta.SC_ERROR_PETICION
            mensajeSMIME.setTipo(Tipo.EVENTO_VOTACION_SIN_CENTRO_CONTROL)
			mensajeSMIME.save();
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage('error.requestWithoutControlCenter', null, locale))
        }
		evento.save(flush:true)
        evento.url = "${grailsApplication.config.grails.serverURL}" + 
			"${grailsApplication.config.SistemaVotacion.sufijoURLEventoVotacionValidado}${evento.id}"
        if (mensajeJSON.opciones) {
            def opciones = opcionDeEventoService.guardarOpciones(evento, mensajeJSON.opciones)
            JSONArray arrayOpciones = new JSONArray()
            opciones.collect { opcion ->
                    arrayOpciones.add([id:opcion.id, contenido:opcion.contenido] as JSONObject  )
            }
            mensajeJSON.opciones = arrayOpciones
        } 
        mensajeJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL, 
			nombre:grailsApplication.config.SistemaVotacion.serverName] as JSONObject
		if (mensajeJSON.etiquetas) {
			Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
			if(etiquetaSet) evento.setEtiquetaSet(etiquetaSet)
		} 
		evento = evento.save()
		log.debug(" ------ Guardando Votación: ${evento.id}")
		mensajeJSON.id = evento.id
		mensajeJSON.fechaCreacion = DateUtils.getStringFromDate(evento.dateCreated)
		mensajeJSON.tipo = tipo
		mensajeJSON.URL = evento.url
		Respuesta respuestaClaves = almacenClavesService.generar(evento)
		if(Respuesta.SC_OK == respuestaClaves.codigoEstado) {
			mensajeJSON.certCAVotacion = new String(
				CertUtil.fromX509CertToPEM (respuestaClaves.certificado))
		}
		File cadenaCertificacion = grailsApplication.mainContext.getResource(
			grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
		mensajeJSON.cadenaCertificacion = new String(cadenaCertificacion.getBytes())
		
		X509Certificate certUsuX509 = smimeMessage.getFirmante().getCertificate()
		mensajeJSON.usuario = new String(CertUtil.fromX509CertToPEM (certUsuX509))
		
        Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
        String mensajeValidado = firmaService.obtenerCadenaFirmada(mensajeJSON.toString(),
			messageSource.getMessage('mime.asunto.EventoVotacionValidado', null, locale), header)
        
        mensajeSMIME.evento = evento
        mensajeSMIME.save(flush:true);
        MensajeSMIME mensajeSMIMEValidado = new MensajeSMIME(tipo:Tipo.EVENTO_VOTACION_VALIDADO,
				evento:evento, smimePadre:mensajeSMIME,
                usuario:usuario, valido:isValidSignature,
                contenido:mensajeValidado.getBytes())
        mensajeSMIMEValidado.save();
		
        return new Respuesta(codigoEstado:Respuesta.SC_OK, fecha:DateUtils.getTodayDate(),
                mensajeSMIME:mensajeSMIME, evento:evento, usuario:usuario, 
                mensajeSMIMEValidado:mensajeSMIMEValidado, smimeMessage:smimeMessage)
    }
    
	public Respuesta generarCopiaRespaldo (EventoVotacion evento, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${evento.id}")
		Respuesta respuesta;
		if (evento) {
			def votosContabilizados = Voto.findAllByEventoVotacionAndEstado(evento, Voto.Estado.OK)
			def solicitudesAcceso =  SolicitudAcceso.findAllByEventoVotacionAndEstado(evento, SolicitudAcceso.Estado.OK)
			def fecha = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
			String zipNamePrefix = messageSource.getMessage('votingBackupFileName', null, locale);
			def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" + 
				"/${fecha}/${zipNamePrefix}_${evento.id}"
			new File(basedir).mkdirs()
			int i = 0
			def metaInformacionMap = [numeroVotos:votosContabilizados.size(),
				solicitudesAcceso:solicitudesAcceso.size(),
				URL:"${grailsApplication.config.grails.serverURL}/evento/obtener?id=${evento.id}",
				tipoEvento:Tipo.EVENTO_VOTACION.toString(), asunto:evento.asunto]
			String metaInformacionJSON = metaInformacionMap as JSON
			File metaInformacionFile = new File("${basedir}/meta.inf")
			metaInformacionFile.write(metaInformacionJSON)
			String votoFileName = messageSource.getMessage('votoFileName', null, locale)
			String solicitudAccesoFileName = messageSource.getMessage('solicitudAccesoFileName', null, locale)
			votosContabilizados.each { voto ->
				MensajeSMIME mensajeSMIME = voto.mensajeSMIME
				ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
				MimeMessage msg = new MimeMessage(null, bais);
				File smimeFile = new File("${basedir}/${votoFileName}_${i}")
				FileOutputStream fos = new FileOutputStream(smimeFile);
				msg.writeTo(fos);
				fos.close();
				i++;
			}
			solicitudesAcceso.each { solicitud ->
				MensajeSMIME mensajeSMIME = solicitud.mensajeSMIME
				ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
				MimeMessage msg = new MimeMessage(null, bais);
				File smimeFile = new File("${basedir}/${solicitudAccesoFileName}_${i}")
				FileOutputStream fos = new FileOutputStream(smimeFile);
				msg.writeTo(fos);
				fos.close();
				i++;
			}
			def ant = new AntBuilder()
			ant.zip(destfile: "${basedir}.zip", basedir: basedir)
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, cantidad:votosContabilizados?.size(), file:new File("${basedir}.zip"))
		} else respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage(
			'eventNotFound', [evento.id].toArray(), locale))
		return respuesta
	}
	
}

