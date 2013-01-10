package org.sistemavotacion.controlacceso

import grails.converters.JSON
import java.security.MessageDigest

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.sistemavotacion.controlacceso.modelo.Respuesta
import org.sistemavotacion.controlacceso.modelo.SelloTiempo
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.bouncycastle.util.encoders.Base64;

class TimeStampController {
	
	def timeStampService

    def index() { }
		
	def prueba() {
		TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
		String pueba = "prueba"
		//reqgen.setReqPolicy(m_sPolicyOID);
		
		MessageDigest sha = MessageDigest.getInstance("SHA1");
		byte[] digest =  sha.digest(pueba.getBytes() );
		String digestStr = new String(Base64.encode(digest));
		log.debug("--- digestStr : ${digestStr}")
		
		TimeStampRequest timeStampRequest = reqgen.generate(TSPAlgorithms.SHA256, digest);
		TimeStampRequest timeStampRequestServer = new TimeStampRequest(timeStampRequest.getEncoded())
		//String timeStampRequestStr = new String(Base64.encode(timeStampRequestBytes));
		//log.debug("timeStampRequestStr : ${timeStampRequestStr}")
		render timeStampService.processRequest(timeStampRequest.getEncoded(), null).codigoEstado
		return
	}
	
	def obtener() {
		try {
			Map multipartFileMap = ((MultipartHttpServletRequest) request)?.getFileMap()
			MultipartFile timeStamprequestBytes = multipartFileMap?.values()?.iterator()?.next()
			if(timeStamprequestBytes) {
				Respuesta respuesta = timeStampService.processRequest(
					timeStamprequestBytes.getBytes(), request.getLocale())
				response.status = respuesta.codigoEstado
				if(Respuesta.SC_OK == respuesta.codigoEstado) {
					response.outputStream << respuesta.timeStampToken // Performing a binary stream copy
					response.outputStream.flush()
				} else render respuesta.mensaje
				return
			}	
		} catch(Exception ex) {
			log.debug (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_PETICION
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render(view:"index")
		return
	}
	
	def getBySerialNumber() {
		if(params.long('serialNumber')) {
			SelloTiempo selloDeTiempo
			SelloTiempo.withTransaction {
				selloDeTiempo = SelloTiempo.findBySerialNumber(params.long('serialNumber'))
			}
			if(selloDeTiempo) {
				response.status = Respuesta.SC_OK
				response.outputStream << selloDeTiempo.getTokenBytes() // Performing a binary stream copy
				response.outputStream.flush()
			} else {
				response.status = Respuesta.SC_NOT_FOUND
			}
			return
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render(view:"index")
		return
	}

}
