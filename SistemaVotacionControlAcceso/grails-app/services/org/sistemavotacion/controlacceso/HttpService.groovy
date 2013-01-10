package org.sistemavotacion.controlacceso

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import java.security.cert.X509Certificate;
import java.util.Set;
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.seguridad.*;
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.MultipartEntity
import org.codehaus.groovy.grails.web.json.*
import java.util.Locale;

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
	
	public Respuesta obtenerInfoActorConIP (String urlInfo, ActorConIP actorConIP) {
		log.debug "obtenerInfoActorConIP - urlInfo: ${urlInfo}"
		Respuesta respuesta
		try {
			def infoActorHTTPBuilder = new HTTPBuilder(urlInfo);
			infoActorHTTPBuilder.request(Method.GET) { req ->
				response.'200' = { resp, reader ->
					log.debug "***** OK: ${resp.statusLine}"
					actorConIP.nombre = reader.nombre
					actorConIP.serverURL = reader.serverURL
					actorConIP.estado = ActorConIP.Estado.valueOf(reader.estado)
					actorConIP.tipoServidor = Tipo.valueOf(reader.tipoServidor)
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.actorConIP = actorConIP
				}
				response.failure = { resp ->
					log.error "***** ERROR: ${resp.statusLine}"
					respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
					respuesta.mensaje = new String("${reader}")
				}
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:500, mensaje:ex.getMessage())
		}
		return respuesta
	}

	Respuesta notificarInicializacionDeEvento (ActorConIP actorConIP, byte[] mensaje) {
		def urlInicializacion = "${actorConIP.serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLInicializacionEvento}"
		log.debug("notificarInicializacionDeEvento - urlInicializacion:${urlInicializacion}")
		String nombreEntidadFirmada = grailsApplication.config.SistemaVotacion.nombreEntidadFirmada
		def httpBuilder = new HTTPBuilder(urlInicializacion);
		def respuesta = new Respuesta()
		try {
			httpBuilder.request(POST) {request ->
				requestContentType = ContentType.URLENC
				MultipartEntity entity = new MultipartEntity();
				ByteArrayBody  fileBody = new ByteArrayBody(mensaje, nombreEntidadFirmada);
				entity.addPart(nombreEntidadFirmada, fileBody);
				request.entity = entity
				request.getParams().setParameter("http.connection.timeout", new Integer(10000));
				request.getParams().setParameter("http.socket.timeout", new Integer(10000));
				response.'200' = { resp, reader ->
						log.debug "***** OK: ${resp.statusLine}"
						respuesta.codigoEstado = resp.statusLine.statusCode
				}
				response.failure = { resp, reader ->
						String mensajeRespuesta = "${reader}"
						log.error "***** Error: ${resp.statusLine}"
						log.error "***** mensajeRespuesta: ${mensajeRespuesta}"
						respuesta = new Respuesta(mensaje:mensajeRespuesta,
							codigoEstado:resp.statusLine.statusCode)
				}
			}
		} catch(SocketTimeoutException ste) {
			log.error(ste.getMessage(), ste)
			return new Respuesta(mensaje:ste.getMessage(),
				codigoEstado:400)
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
			
	public Respuesta enviarMensaje (String destURL, byte[] bytesMensaje) {
		def httpBuilder = new HTTPBuilder(destURL);
		log.debug "enviarMensaje - destURL: '${destURL}'"
		Respuesta respuesta
		httpBuilder.request(POST) {request ->
			requestContentType = ContentType.URLENC
			MultipartEntity entity = new MultipartEntity();
			ByteArrayBody  fileBody = new ByteArrayBody(bytesMensaje,
				grailsApplication.config.SistemaVotacion.nombreEntidadFirmada);
			entity.addPart(grailsApplication.config.SistemaVotacion.nombreEntidadFirmada, fileBody);
			request.entity = entity
			log.debug "***** Lanzada solicitudFirmada: "
			response.'200' = { resp, reader ->
				log.debug "***** OK: ${resp.statusLine}"
				respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
				respuesta.mensaje = new String("${reader}")
			}
			response.failure = { resp, reader ->
				log.error "***** Error: ${resp.statusLine}"
				respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
				respuesta.mensaje = new String("${reader}")
			}
		}
		return respuesta;
	}
	
}
