package org.sistemavotacion.controlacceso

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.*;
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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class EventoFirmaService {	
		
    static transactional = true

    def etiquetaService
    def subscripcionService
    def firmaService
    def eventoService
    def grailsApplication
	def messageSource

    public Respuesta guardarEvento(SMIMEMessageWrapper smimeMessage, Locale locale) {
        log.debug("guardarEvento - mensaje: ${smimeMessage.getSignedContent()}")
        Tipo tipoMensaje
        if (smimeMessage.isValidSignature()) tipoMensaje = Tipo.EVENTO_FIRMA
        else tipoMensaje = Tipo.EVENTO_FIRMA_ERROR
        def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
        Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		if(Respuesta.SC_OK != respuestaUsuario.codigoEstado) 
			return respuestaUsuario
		Usuario usuario = respuestaUsuario.usuario
        EventoFirma evento = new EventoFirma(usuario:usuario,
                asunto:mensajeJSON.asunto,
                contenido:mensajeJSON.contenido,
                fechaInicio: new Date().parse(
					"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaInicio),
                fechaFin: new Date().parse(
					"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaFin))
        evento = evento.save()
        evento.url = "${grailsApplication.config.grails.serverURL}" + 
			"${grailsApplication.config.SistemaVotacion.sufijoURLEventoFirmaValidado}${evento.id}"
        mensajeJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL, 
			nombre:grailsApplication.config.SistemaVotacion.serverName] as JSONObject
        if (mensajeJSON.etiquetas) {
			Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
			evento.setEtiquetaSet(etiquetaSet)
			evento.save()
		} 
        mensajeJSON.id = evento.id
        mensajeJSON.fechaCreacion = DateUtils.getStringFromDate(evento.dateCreated) 
        mensajeJSON.tipo = tipoMensaje
        log.debug("guardarEvento - mensajeJSON: ${mensajeJSON.toString()}")
        String mensajeValidado = firmaService.obtenerCadenaFirmada(mensajeJSON.toString(),
                messageSource.getMessage('mime.asunto.EventoFirmaValidado', null, locale))
        MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipoMensaje,
                usuario:usuario, valido:smimeMessage.isValidSignature(),
                contenido:smimeMessage.getBytes(), evento:evento)
                mensajeSMIME.save();
        MensajeSMIME mensajeSMIMEValidado = new MensajeSMIME(tipo:Tipo.EVENTO_FIRMA_VALIDADO,
                smimePadre:mensajeSMIME, evento:evento,
        usuario:usuario, valido:smimeMessage.isValidSignature(),
        contenido:mensajeValidado.getBytes())
        mensajeSMIMEValidado.save();
        evento.estado = eventoService.obtenerEstadoEvento(evento)
        evento = evento.save()
        return new Respuesta(codigoEstado:Respuesta.SC_OK, fecha:DateUtils.getTodayDate(),
                mensajeSMIME:mensajeSMIME, evento:evento, usuario:usuario, 
                mensajeSMIMEValidado:mensajeSMIMEValidado, smimeMessage:smimeMessage)
    }
	
	public Respuesta generarCopiaRespaldo(EventoFirma evento, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${evento.id}")
		Respuesta respuesta;
		if (evento) {
			def firmasRecibidas = Documento.findAllWhere(evento:evento,
				estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
			def fecha = DateUtils.getDirStringFromDate(DateUtils.getTodayDate())
			String zipNamePrefix = messageSource.getMessage('manifestBackupFileName', null, locale);
			def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}/" + 
					"${fecha}/${zipNamePrefix}_${evento.id}"
			new File(basedir).mkdirs()
			int i = 0
			def metaInformacionMap = [numeroFirmas:firmasRecibidas.size(),
				URL:"${grailsApplication.config.grails.serverURL}/evento/obtener?id=${evento.id}",
				tipoEvento:Tipo.EVENTO_FIRMA.toString(), asunto:evento.asunto]
			String metaInformacionJSON = metaInformacionMap as JSON
			File metaInformacionFile = new File("${basedir}/meta.inf")
			metaInformacionFile.write(metaInformacionJSON)
			String fileNamePrefix = messageSource.getMessage('manifestSignatureLbl', null, locale);
			firmasRecibidas.each { firma ->
				File pdfFile = new File("${basedir}/${fileNamePrefix}_${i}.pdf")
				FileOutputStream fos = new FileOutputStream(pdfFile);
				fos.write(firma.pdf);
				fos.close();
				i++;
			}
			def ant = new AntBuilder()
			ant.zip(destfile: "${basedir}.zip", basedir: basedir)
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, 
				cantidad:firmasRecibidas.size(), file:new File("${basedir}.zip"))
		} else respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage(
				'evento.eventoNotFound', [evento.id].toArray(), locale))
		return respuesta
	}

}