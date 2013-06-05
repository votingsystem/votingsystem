package org.sistemavotacion.centrocontrol

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import java.security.cert.X509Certificate;
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.seguridad.*;
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.mime.content.*
import org.apache.http.entity.mime.MultipartEntity

class HttpService {
	
    def grailsApplication;

    public Respuesta obtenerCadenaCertificacion (String serverURL) {
        String urlCadenaCertificacion = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLCadenaCertificacion}"
        log.debug "obtenerCadenaCertificacion - urlCadenaCertificacion: ${urlCadenaCertificacion}"
        def infoActorHTTPBuilder = new HTTPBuilder(urlCadenaCertificacion);
		Respuesta respuesta
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
			response.failure = { resp, reader ->
				log.error "***** ERROR: ${resp.statusLine}"
				respuesta = new Respuesta(codigoEstado:resp.statusLine.statusCode)
				respuesta.mensaje = new String("${reader}")
			}
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
						respuesta.mensaje = new String("${reader}")
				}
				response.failure = { resp, reader ->
						String mensajeRespuesta = new String("${reader}")
						log.error "***** Error: ${resp.statusLine}"
						log.error "***** mensajeRespuesta: ${mensajeRespuesta}"
						respuesta.mensaje = mensajeRespuesta
						respuesta.codigoEstado = resp.statusLine.statusCode
				}
			}
		} catch(SocketTimeoutException ste) {
			log.error(ste.getMessage(), ste)
			respuesta.mensaje = ste.getMessage()
		}
		return respuesta;
	}
	
}