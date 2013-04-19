package org.sistemavotacion.centrocontrol

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.utils.StringUtils;
import grails.converters.JSON
import java.io.File;
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.*;
import java.io.File;
import org.apache.http.conn.HttpHostConnectException
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import java.nio.charset.Charset
import groovyx.net.http.ContentType
import net.sf.json.JSONSerializer
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import java.security.cert.CertificateFactory
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* */
class VotoService {

    static transactional = true

	def messageSource
	def firmaService
    def grailsApplication  
    def httpService
	def encryptionService
	
	Respuesta validarFirmaUsuario(SMIMEMessageWrapper smimeMessageUsu, Locale locale) {
		MensajeSMIME mensajeSMIME = new MensajeSMIME(
			tipo:Tipo.VOTO,	contenido:smimeMessageUsu.getBytes())
		InformacionVoto infoVoto = smimeMessageUsu.informacionVoto
		String certServerURL = StringUtils.checkURL(infoVoto.controlAccesoURL)
		ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:certServerURL)
		log.debug ("validarFirmaUsuario - certServerURL: ${certServerURL}")
		if (!controlAcceso) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
				messageSource.getMessage('validacionVoto.errorEmisorDesconocido', null, locale)) 
		EventoVotacion evento = EventoVotacion.findWhere(controlAcceso:controlAcceso,
			eventoVotacionId:infoVoto.getEventoId())
		if (!evento) {
			log.debug ("No se ha encontrado evento con id '${infoVoto?.getEventoId()}' para el control de acceso '${certServerURL}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage(
				'validacionVoto.convocatoriaDesconocida', null, locale))
		}
		if(evento.estado != EventoVotacion.Estado.ACTIVO) {
			log.debug ("Recibido voto para evento cerrado - evento id: '${evento.id}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage(
				'validacionVoto.eventClosed', [evento.asunto].toArray(), locale))
		}
		Certificado certificado = Certificado.findWhere(
			hashCertificadoVotoBase64:infoVoto.hashCertificadoVotoBase64,
			estado:Certificado.Estado.UTILIZADO)
		if (certificado) {
			log.error("Voto repetido - hashCertificadoVotoBase64:${infoVoto.hashCertificadoVotoBase64}")
			Voto voto = Voto.findWhere(certificado:certificado)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_VOTO_REPETIDO, voto:voto)
		}
		X509Certificate certificadoFirma = 
			smimeMessageUsu.getFirmante()?.getCertificate()
		Respuesta respuestaValidacionCert = firmaService.
				validarCertificacionCertificadoVoto(certificadoFirma, evento, locale)
		if(Respuesta.SC_OK != respuestaValidacionCert.codigoEstado) {
			log.error("Error validando el certificado del voto")
			return respuestaValidacionCert
		} 
		log.debug ("certificadoFirma.getSubjectDN(): ${certificadoFirma.getSubjectDN()}")
        certificado = new Certificado(esRaiz:false, estado: Certificado.Estado.OK, 
			tipo:Certificado.Tipo.VOTO, contenido:certificadoFirma.getEncoded(), 
			numeroSerie:certificadoFirma.getSerialNumber().longValue(), eventoVotacion:evento,
			validoDesde:certificadoFirma.getNotBefore(), validoHasta:certificadoFirma.getNotAfter())
		certificado.setSigningCert(certificadoFirma)
		certificado.save()
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento,
			certificado:certificadoFirma)
	}
	
	Respuesta validarFirmaControlAcceso(SMIMEMessageWrapper smimeMessageCA) {
		X509Certificate certificadoFirma = smimeMessageCA.getFirmante().getCertificate()
		log.debug ("certificadoFirma.getSubjectDN(): ${certificadoFirma.getSubjectDN()}")
		//TODO
		return new Respuesta()
	}
    
    def obtenerAsuntoEvento(byte[] signedDataBytes) {
        log.debug "obtenerAsuntoEvento"
        ByteArrayInputStream bais = new ByteArrayInputStream(signedDataBytes);
        MimeMessage email = new MimeMessage(null, bais);
        return email.getSubject()
    }
	
	
    synchronized Respuesta enviarVoto_A_ControlAcceso (MimeMessage smimeMessage,
            EventoVotacion eventoVotacion, Locale locale) {
		String urlVotosControlAcceso = "${eventoVotacion.controlAcceso.serverURL}" + 
			"${grailsApplication.config.SistemaVotacion.sufijoURLNotificacionVotoControlAcceso}"
        log.debug ("enviarVoto_A_ControlAcceso - urlVotosControlAcceso: ${urlVotosControlAcceso}")
		String localServerURL = grailsApplication.config.grails.serverURL
		
		MensajeSMIME multifirmaCentroControl = new MensajeSMIME(
			tipo:Tipo.VOTO_VALIDADO_CENTRO_CONTROL, valido:true)
		multifirmaCentroControl.save()
		
		smimeMessage.setMessageID("${localServerURL}/mensajeSMIME/obtener?id=${multifirmaCentroControl.id}")
		smimeMessage.setFrom(new InternetAddress(
			grailsApplication.config.SistemaVotacion.serverName.replaceAll(" ", "")))
		smimeMessage.setTo("${eventoVotacion.controlAcceso.serverURL}")
				
        MimeMessage multiSignedMessage = firmaService.generarMultifirma(
            smimeMessage, messageSource.getMessage('validacionVoto.smimeMessageSubject', null, locale))
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        multiSignedMessage.writeTo(out);
		byte[] multiSignedMessageBytes = out.toByteArray()

		multifirmaCentroControl.contenido = multiSignedMessageBytes
		multifirmaCentroControl.save();
		
		Respuesta respuesta = encryptionService.encryptSMIMEMessage(
			multiSignedMessageBytes, eventoVotacion.getControlAccesoCert(), locale);
		if (Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
		
		byte[] encryptedMultiSignedMessageBytes = respuesta.messageBytes

        respuesta = httpService.enviarMensaje(
			urlVotosControlAcceso, encryptedMultiSignedMessageBytes)
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			SMIMEMessageWrapper smimeMessageCA = SMIMEMessageWrapper.build(
				new ByteArrayInputStream(respuesta.mensaje.getBytes()),
				null, SMIMEMessageWrapper.Tipo.VOTO);
			return validarFirmas(smimeMessageCA, multifirmaCentroControl, eventoVotacion)
		} else return respuesta
    }
     
	synchronized Respuesta validarFirmas(SMIMEMessageWrapper smimeMessageCA,
		MensajeSMIME multifirmaCentroControl, EventoVotacion eventoVotacion) {
		log.debug ("validarFirmas")
		Respuesta respuesta = firmaService.
				validarCertificacionFirmantesVoto(smimeMessageCA, eventoVotacion)
		if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
		String localServerURL = grailsApplication.config.grails.serverURL
		InformacionVoto infoVoto = smimeMessageCA.informacionVoto
		MensajeSMIME mensajeSMIME = new MensajeSMIME(valido:true,
			contenido:smimeMessageCA.getBytes(), 
			smimePadre:multifirmaCentroControl,
			tipo:Tipo.VOTO_VALIDADO_CONTROL_ACCESO)
		String mensaje
		EventoVotacion evento = EventoVotacion.findWhere(
			eventoVotacionId:String.valueOf(infoVoto.getEventoId()))
		if (!evento) mensaje = 
			"El evento con id '${infoVoto.getEventoId()}' no esta dado de alta en este servidor"
		mensajeSMIME.eventoVotacion = evento
		Certificado certificado = Certificado.findWhere(
			hashCertificadoVotoBase64:infoVoto.hashCertificadoVotoBase64,
			estado:Certificado.Estado.OK)
		if (!certificado)
			mensaje = "El hash '${infoVoto.hashCertificadoVotoBase64}' no es válido"
		def votoJSON = JSON.parse(smimeMessageCA.getSignedContent())
		OpcionDeEvento opcionSeleccionada = OpcionDeEvento.findWhere(
			opcionDeEventoId:String.valueOf(votoJSON.opcionSeleccionadaId))
		if (!opcionSeleccionada)
			mensaje = "No existe ninguna opción con identificador '${String.valueOf(votoJSON.opcionSeleccionadaId)}'"
		if (mensaje) {
			log.error(mensaje)
			return new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:mensaje)
		}
		mensajeSMIME.save()
		Voto voto = new Voto(opcionDeEvento:opcionSeleccionada,
			eventoVotacion:evento, estado:Voto.Estado.OK,
			certificado:certificado, mensajeSMIME:mensajeSMIME)
		voto.save()
		certificado.estado = Certificado.Estado.UTILIZADO;
		certificado.save();
		return new Respuesta(codigoEstado:Respuesta.SC_OK, voto:voto)
	}
			
			
    EventoVotacion obtenerEventoAsociado(String asunto) {
        log.debug "obtenerEventoAsociado - asunto del Token: ${asunto}"
        EventoVotacion eventoVotacion
        //[Token de Acceso]-serverURL-eventoVotacionId
        String[] camposAsunto = asunto.split("-") 
        String serverURL = camposAsunto[1].toString()
        ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:serverURL)
        if (controlAcceso) {
            eventoVotacion = EventoVotacion.findWhere(controlAcceso:controlAcceso,
            eventoVotacionId:camposAsunto[2])
        }
        return eventoVotacion
    }
	public synchronized Respuesta validarAnulacion (SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug ("validarAnulacion")
		def anulacionJSON = JSON.parse(smimeMessage.getSignedContent())
		def origenHashCertificadoVoto = anulacionJSON.origenHashCertificadoVoto
		def hashCertificadoVotoBase64 = anulacionJSON.hashCertificadoVotoBase64
		def hashCertificadoVoto = CMSUtils.obtenerHashBase64(origenHashCertificadoVoto, 
			"${grailsApplication.config.SistemaVotacion.votingHashAlgorithm}")
		if (!hashCertificadoVotoBase64.equals(hashCertificadoVoto))
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:messageSource.getMessage(
					'anulacionVoto.errorEnHashCertificado', null, locale))
		def certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
		if (!certificado)
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage(
				'anulacionVoto.errorCertificadoNoEncontrado', null, locale))
		def voto = Voto.findWhere(certificado:certificado)
		if(!voto) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage(
			'anulacionVoto.errorVotoNoEncontrado', null, locale))
		MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:Tipo.ANULADOR_VOTO, 
			contenido:smimeMessage.getBytes(), valido:true, eventoVotacion:voto.eventoVotacion)
		mensajeSMIME.save()
		log.debug("mensajeSMIME.id: ${mensajeSMIME.id}")
		voto.estado = Voto.Estado.ANULADO
		voto.save()
		certificado.estado = Certificado.Estado.ANULADO
		certificado.save()
		AnuladorVoto anuladorVoto = new AnuladorVoto(voto:voto, 
			certificado:certificado, eventoVotacion:voto.eventoVotacion,
			origenHashCertificadoVotoBase64:origenHashCertificadoVoto,
			hashCertificadoVotoBase64:hashCertificadoVotoBase64,
			mensajeSMIME:mensajeSMIME)
		anuladorVoto.save();
		log.debug("anuladorVoto.id: ${anuladorVoto.id}")
		return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}
 
}