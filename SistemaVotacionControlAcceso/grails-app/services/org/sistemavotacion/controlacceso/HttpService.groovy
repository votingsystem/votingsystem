package org.sistemavotacion.controlacceso

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Set;
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.seguridad.*;
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.MultipartEntity
import org.codehaus.groovy.grails.web.json.*
import java.util.Locale;

import javax.persistence.Transient;

class HttpService {
	
	def grailsApplication
	def messageSource

    public Respuesta obtenerCadenaCertificacion (String serverURL, Locale locale) {
        String urlCadenaCertificacion = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLCadenaCertificacion}"
        log.debug "obtenerCadenaCertificacion - urlCadenaCertificacion: ${urlCadenaCertificacion}"
        def infoActorHTTPBuilder = new HTTPBuilder(urlCadenaCertificacion);
		Respuesta respuesta
		try {
			infoActorHTTPBuilder.request(Method.GET) { req ->
				response.'200' = { resp, reader ->
					log.debug "***** OK: ${resp.statusLine}"
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.cadenaCertificacion = reader.getBytes()
				}
				response.failure = { resp, reader ->
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.mensaje = new String("${reader}")
				}
			}
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:500, mensaje:
				messageSource.getMessage('http.errorObteniendoCadenaCertificacion', 
					[serverURL].toArray(), locale))
		}
        return respuesta;
    }
	
	public Respuesta obtenerCertificado (String urlCertificado) {
		log.debug "obtenerCertificado - urlCertificado: ${urlCertificado}"
		Respuesta respuesta
		def certificadoHTTPBuilder = new HTTPBuilder(urlCertificado.trim());
		certificadoHTTPBuilder.request(Method.GET) { req ->
			response.'200' = { resp, reader ->
				log.debug "---- OK: ${resp.statusLine}"
				respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
				respuesta.certificado = CertUtil.fromPEMToX509Cert(reader.text.getBytes())
			}
			response.failure = { resp, reader ->
				log.error "***** ERROR: ${resp.statusLine}"
				respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
				respuesta.mensaje = new String("${reader}")
			}
		}
		return respuesta
	}
	
	public Respuesta obtenerInfoActorConIP (String urlInfo) {
		log.debug "obtenerInfoActorConIP - urlInfo: ${urlInfo}"
		Respuesta respuesta
		try {
			def infoActorHTTPBuilder = new HTTPBuilder(urlInfo);
			infoActorHTTPBuilder.request(Method.GET) { req ->
				response.'200' = { resp, reader ->
					log.debug "***** OK: ${resp.statusLine}"
					ActorConIP actorConIP = new ActorConIP(nombre:reader.nombre,
						serverURL:reader.serverURL, 
						cadenaCertificacion:reader.cadenaCertificacionPEM?.getBytes(),
						estado:ActorConIP.Estado.valueOf(reader.estado),
						tipoServidor:Tipo.valueOf(reader.tipoServidor))
					respuesta = new Respuesta(actorConIP:actorConIP,
						codigoEstado:resp.statusLine.statusCode)
				}
				response.failure = { resp ->
					log.error "***** ERROR: ${resp.statusLine}"
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.mensaje = new String("${reader}")
				}
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:ex.getMessage())
		}
		return respuesta
	}
	
	Respuesta sendMessage(byte[] message, String contentType, String serverURL) {
		log.debug(" - sendMessage:${serverURL} - contentType: ${contentType}")
		def httpBuilder = new HTTPBuilder(serverURL);
		def respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR)
		try {
			httpBuilder.request(POST) {request ->
				ByteArrayEntity byteArrayEntity = new ByteArrayEntity(message)
				byteArrayEntity.setContentType(contentType)
				request.entity = byteArrayEntity
				request.getParams().setParameter("http.connection.timeout", new Integer(10000));
				request.getParams().setParameter("http.socket.timeout", new Integer(10000));
				response.'200' = { resp, reader ->
					log.debug "***** OK: ${resp.statusLine}"
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.mensaje = reader.text
				}
				response.failure = { resp, reader ->
					log.error "***** Error: ${resp.statusLine}"
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.mensaje = new String("${reader}")
				}
			}
		} catch(SocketTimeoutException ste) {
			log.error(ste.getMessage(), ste)
			respuesta.mensaje = ste.getMessage()
		}
		return respuesta;
	}
	
	ConcurrentHashMap notificarActoresInicializacionDeEvento (byte[] mensaje,
			EventoVotacion eventoVotacion, Set<ActorConIP> actores) {
		log.debug("notificarActoresInicializacionDeEvento - eventoVotacion: ${eventoVotacion.asunto} - numero actores: ${actores.size()}")
		Executor executor = Executors.newFixedThreadPool(actores.size())
		final ConcurrentHashMap<ActorConIP, Respuesta> mapaRespuestas =
			new ConcurrentHashMap<ActorConIP, Respuesta>(actores.size())
		try {
			for (final ActorConIP actor : actores) {
				executor.execute(new Runnable(){
					public void run () {//No añadir en estos hilos lógica de acceso a la BD!!!
						def respuesta = notificarActorInicializacionDeEvento(actor, eventoVotacion)
						mapaRespuestas.put(actor, respuesta)
					}
				})
			}
		} finally {
				executor.shutdown()
				//executor.awaitTermination(Long.valueOf(grailsApplication.config.SistemaVotacion.timeOutConsulta), TimeUnit.MILLISECONDS)
				executor.awaitTermination(2000, TimeUnit.MILLISECONDS)
		}
		//comprobarRespuestasActores(mapaRespuestas, eventoVotacion)
		return mapaRespuestas
	}
			
	
}
