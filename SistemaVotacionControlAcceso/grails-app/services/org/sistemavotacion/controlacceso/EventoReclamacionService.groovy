package org.sistemavotacion.controlacceso

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.*;
import org.sistemavotacion.controlacceso.modelo.*;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import java.io.File;
import java.io.FileOutputStream;
import javax.mail.internet.MimeMessage;
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class EventoReclamacionService {	
		
    static transactional = true

    def etiquetaService
    def subscripcionService
    def firmaService
    def eventoService
    def grailsApplication
	def messageSource
	

    Respuesta guardarEvento(SMIMEMessageWrapper smimeMessage, Locale locale) {		
        log.debug("guardarEvento - mensaje: ${smimeMessage.getSignedContent()}")
        Tipo tipoMensaje
        if (smimeMessage.isValidSignature()) tipoMensaje = Tipo.EVENTO_RECLAMACION
        else tipoMensaje = Tipo.EVENTO_FIRMA_ERROR
        def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		if(200 != respuestaUsuario.codigoEstado) {
			response.status = respuestaUsuario.codigoEstado
			render respuestaUsuario.mensaje
			return false
		}
		Usuario usuario = respuestaUsuario.usuario
        EventoReclamacion evento = new EventoReclamacion(usuario:usuario,
                asunto:mensajeJSON.asunto,
                contenido:mensajeJSON.contenido,
				copiaSeguridadDisponible:mensajeJSON.copiaSeguridadDisponible,
                fechaFin: new Date().parse(
					"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaFin))
		if(mensajeJSON.fechaInicio) evento.fechaInicio = new Date().parse(
					"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaInicio)
		else evento.fechaInicio = DateUtils.getTodayDate();
        evento.url = "${grailsApplication.config.grails.serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLEventoReclamacionValidado}${evento.id}"
        evento = evento.save()
        if (mensajeJSON.etiquetas) {
			Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
			evento.setEtiquetaSet(etiquetaSet)
			evento.save()
		} 
		mensajeJSON.id = evento.id
        mensajeJSON.fechaCreacion = DateUtils.getStringFromDate(evento.dateCreated) 
        mensajeJSON.tipo = tipoMensaje
        def camposValidados = []
        JSONArray arrayCampos = new JSONArray()
        mensajeJSON.campos?.collect { campoItem ->
            def campo = new CampoDeEvento(evento:evento, contenido:campoItem.contenido)
            campo.save();
            arrayCampos.add(new JSONObject([id:campo.id, contenido:campo.contenido]))
        }
        mensajeJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL, nombre:grailsApplication.config.SistemaVotacion.NombreControlAcceso]  as JSONObject
        mensajeJSON.campos = arrayCampos
        log.debug("guardarEvento - mensajeValidado: ${mensajeJSON.toString()}")
        String mensajeValidado = firmaService.obtenerCadenaFirmada(mensajeJSON.toString(),
			messageSource.getMessage('mime.asunto.EventoReclamacionValidado', null, locale))
		MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipoMensaje,
			usuario:usuario, valido:smimeMessage.isValidSignature(),
			contenido:smimeMessage.getBytes(),evento:evento)
		mensajeSMIME.save();
		MensajeSMIME mensajeSMIMEValidado = new MensajeSMIME(tipo:Tipo.EVENTO_RECLAMACION_VALIDADO,
				smimePadre:mensajeSMIME, evento:evento,
                usuario:usuario, valido:smimeMessage.isValidSignature(),
                contenido:mensajeValidado.getBytes())
        mensajeSMIMEValidado.save();
        evento.estado = eventoService.obtenerEstadoEvento(evento)
        if(mensajeJSON.cardinalidad) evento.cardinalidadRepresentaciones = Evento.Cardinalidad.valueOf(mensajeJSON.cardinalidad)
        else evento.cardinalidadRepresentaciones = Evento.Cardinalidad.UNA
        evento = evento.save()
        return new Respuesta(codigoEstado:200, fecha:DateUtils.getTodayDate(),
                mensajeSMIME:mensajeSMIME, evento:evento, usuario:usuario, 
                mensajeSMIMEValidado:mensajeSMIMEValidado, smimeMessage:smimeMessage)
    }

    public void generarCopiasDeRespaldo () {
        log.debug("generarCopiasDeRespaldo")
        EventoReclamacion.findAll().collect {evento ->
            generarCopiaRespaldo(evento, null)
        }
    }
	
    public Respuesta generarCopiaRespaldo (Long eventoId) {
        log.debug("generarCopiaRespaldo - eventoId: ${eventoId}")
        EventoReclamacion evento = EventoReclamacion.get (eventoId)
        if (!evento) return new Respuesta(codigoEstado:404)
        return generarCopiaRespaldo(evento, null)
    }

    public Respuesta generarCopiaRespaldo (EventoReclamacion evento, Locale locale) {
        log.debug("generarCopiaRespaldo - eventoId: ${evento.id}")
		Respuesta respuesta;
        if (evento) {
            def firmasRecibidas = Firma.findAllWhere(evento:evento)
            def fecha = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
            def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}/${fecha}/CopiaSeguridadEventoReclamacion_${evento.id}"
            new File(basedir).mkdirs()
			int i = 0
			def metaInformacionMap = [numeroFirmas:firmasRecibidas.size(),
				URL:"${grailsApplication.config.grails.serverURL}/evento?id=${evento.id}",
				tipoEvento:Tipo.EVENTO_RECLAMACION.toString(), asunto:evento.asunto]
			String metaInformacionJSON = metaInformacionMap as JSON
			File metaInformacionFile = new File("${basedir}/meta.inf")
			metaInformacionFile.write(metaInformacionJSON)
            firmasRecibidas.each { firma ->
                MensajeSMIME mensajeSMIME = firma.mensajeSMIME
                ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
                MimeMessage msg = new MimeMessage(null, bais);
                File smimeFile = new File("${basedir}/${i}_${msg.getFileName()}")
                FileOutputStream fos = new FileOutputStream(smimeFile);
                msg.writeTo(fos);
                fos.close();
				i++;
            }
            def ant = new AntBuilder()
            ant.zip(destfile: "${basedir}.zip", basedir: basedir)
			respuesta = new Respuesta(codigoEstado:200, cantidad:firmasRecibidas.size(), file:new File("${basedir}.zip"))
        } else respuesta = new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
				'evento.peticionSinEvento', null, locale))
		return respuesta
    }
	
    public File obtenerCopiaRespaldo(Long eventoId) {
        log.debug("obtenerCopiaRespaldo - eventoId: ${eventoId}")
        EventoReclamacion evento = EventoReclamacion.get(eventoId)
        if (!evento) return null
        return obtenerCopiaRespaldo(evento)
    }
	
    public File obtenerCopiaRespaldo(Evento evento) {
        if (!evento) return null
        def fecha = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
        def baseRuta = "${grailsApplication.config.grails.serverURL}${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}"
        def basedir = "${baseRuta}/${fecha}/${eventoFirma.id}"
        if (new File(basedir).mkdirs()) {
            fecha = DateUtils.getShortStringFromDate(DateUtils.getYesterdayDate())
            basedir = "${baseRuta}/${fecha}/${documentoId}"
        }
        def copiaRespaldo = new File("${basedir}.zip")
        return copiaRespaldo
    }
	
	
}