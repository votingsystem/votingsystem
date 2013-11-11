package org.votingsystem.accesscontrol.controller

import java.util.Date;
import java.util.Locale;import org.votingsystem.util.DateUtils;

import org.votingsystem.accesscontrol.model.Usuario;
import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSSignedData
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Property
import org.votingsystem.accesscontrol.model.*

import grails.converters.JSON

import org.votingsystem.util.FileUtils;
import org.votingsystem.groovy.util.*
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.DateUtils;

import java.io.IOException;
import java.io.InputStream;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*
import org.votingsystem.accesscontrol.model.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mail.SimpleMailMessage

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;


import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @infoController Prueba
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class TestingController {

	def representativeService
	def grailsApplication
	def httpService
	def timeStampService
	def firmaService
	def accessControlBackgroundService
	def applicationContext
	def publishService
	def eventoService
	def pdfService
	def mailSenderService
	def hibernateProperties
	
	
	def index() {
		render(view: "prueba")
	}
	
	def pruebaGsp() {
		if(params.pageName) render(view:params.pageName, model:params)
		else {
			response.status = ResponseVS.SC_ERROR
			render "page name null"
			return false
		}
	}

	def pruMail(){
		for(int i =0; i < 20; i++) {
			log.debug("sent mail ${i}")
			mailSenderService.sendMail("jgzornoza@sistemavotacion.org", "Cuerpo message", "Hola desde grails")
		}
		
	}
	
	def zip(){
		def zipPath = "/home/jgzornoza/Descargas/bootstrap.zip"
		String link = "${grailsApplication.mainContext.getResource('.')?.getFile()}/backup/VotosEvento_5.zip"
		//ln -s /destino /enlaceSimbolico
		
		/*
		def ant = new AntBuilder()
		//File zipResult = new File()
		ant.copy(file: zipPath, tofile: link)*/
		
		
		/*def command = """cp ${zipPath} ${link}"""// Create the String
		def proc = command.execute()                 // Call *execute* on the string
		proc.waitFor()                               // Wait for the command to finish

		// Obtain status and output
		render "return code: ${ proc.exitValue()}<br/>"
		render "stderr: ${proc.err.text}<br/>"
		render "stdout: ${proc.in.text}<br/>"*/
		
		
		/**/
		String url = "/backup/2013-07-02/VotosEvento_8.zip"
		log.debug("redirect to -> ${url}")
		redirect(uri: url)
	}
	
	

	
	def pruEventMap1(){
		Evento event
		Evento.withTransaction {
			event = Evento.get(16)
			Map eventMap = eventoService.getMetaInfMap(event) 
			//Map eventMap = JSON.parse(event?.metaInf)
			//ventMap.metaInf = "Buenas"
			event.metaInf = eventMap as JSON
			log.debug(" === eventMap.metaInf: ${eventMap.metaInf}")
			render event.metaInf
			event.save()
		}
		
		
	}
	
	def pruEventMap(){
		Evento event
		Evento.withTransaction {
			event = Evento.get(16)
		}
		Map eventMap = JSON.parse(event?.metaInf)
		render eventMap as JSON	
	}
	
	
	def pruRevoke(){
		Usuario usuario = Usuario.get(71)	
		Date dateFrom = DateUtils.getDateFromString("2012-01-31 00:00:00")
		Date dateTo = DateUtils.getDateFromString("2014-01-31 00:00:00")
		ResponseVS respuesta = representativeService.getVotingHistoryBackup (
			usuario, dateFrom, dateTo, request.locale)
		render respuesta.statusCode
	}
	
	def votes() {
		def votes = null
		Evento event
		Evento.withTransaction {
			event = Evento.get(6)
		}
		
		Voto.withTransaction {
			//votes = Voto.findAllWhere(estado:Voto.Estado.OK, eventoVotacion:event)
			def criteria = Voto.createCriteria()
			votes = criteria.scroll {
				//maxResults(50)
				eq("estado", Voto.Estado.OK)
				eq("eventoVotacion", event)
			}
			log.debug("--- votes.getClass(): ${votes.getClass()}")
			int iteration = 0;
			while (votes.next() ) {
				def vote = (Voto) votes.get(0);
				log.debug("iteration: ${++iteration} - vote: ${vote.id}")
			}
		}	
	}
	
	
	def asyncMeta() {
		EventoVotacion evento
		EventoVotacion.withTransaction {
			evento = Evento.get(params.id)
		}
		ResponseVS respuesta = representativeService.getAccreditationsMapForEvent(
			evento, request.locale)
		
		render respuesta.data as JSON
	}

	
	def processTokenRequest() {
		//Vote OK -> String timeStampRequestStr = "MFYCAQEwUTANBglghkgBZQMEAgMFAARANunGRBds3c+Md5MBWNwVFGHklPwRn4mjvXIbZefKhSrG7c6crUViButcrCxhEERAoVxpF2hTfJ8PK2j5u5t16w=="
		//reclamacion OK android -> String timeStampRequestStr = "MDkCAQEwMTANBglghkgBZQMEAgEFAAQg5CXdYT/wQKfj57LE0VOUIkQxwtHGUCo4Xb/nDYRKYHsBAf8="
		 String timeStampRequestStr = "MDkCAQEwMTANBglghkgBZQMEAgMFAAQgrbiEUT6OZejyQSkNg1PqR48b+s9pudY512Dm3Oow4H0BAf8="                                                       
		
		
		ResponseVS respuesta = timeStampService.processRequest(Base64.decode(timeStampRequestStr), request.getLocale())
		byte[] encodedTimestamp = respuesta.messageBytes
		String encodedTimestampStr =  new String(Base64.encode(encodedTimestamp));
		render encodedTimestampStr
		return false
	}
	
	def validateToken() {
		//String tokenStr = "MIAGCSqGSIb3DQEHAqCAMIACAQMxDzANBglghkgBZQMEAgEFADCABgsqhkiG9w0BCRABBKCAJIAETTBLAgEBBgIqAzAhMAkGBSsOAwIaBQAEFNAdneKV6YgJ43QzgmtShRJ9olrMAgECGA8yMDEzMDUyNjA5NTUxNFowCwIBAYACAfSBAgH0AAAAAAAAMYIBnTCCAZkCAQEwWDBOMTUwMwYDVQQDDCxBdXRvcmlkYWQgQ2VydGlmaWNhZG9yYSBTaXN0ZW1hIGRlIFZvdGFjacOzbjEVMBMGA1UECwwMQ2VydGlmaWNhZG9zAgYBPqKDu7YwDQYJYIZIAWUDBAIBBQCggZgwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xMzA1MjYwOTU1MTRaMCsGCyqGSIb3DQEJEAIMMRwwGjAYMBYEFB03+2TSvT+K6Uwsq4pkC3CYQ0twMC8GCSqGSIb3DQEJBDEiBCB9c/VH18gSlsUTjDK4ObvcSeeUpJ7iMtDE/xi7LlB/xTANBgkqhkiG9w0BAQEFAASBgEI1QSu1nM0czLOL2bfBog4byqTrcpxSsILPkLo0+IPMBT6Me6CMsQRLszXYWxuwaOvQisDZlEHJXI+T3e6+h6oTBOYeA4PaLKazNtGi+o3OcEmv0jjLvKUMsnG+ngvIVPK+OdkOtLXP6bFgLJvGPHgPHG/t0/CsDjEDv7jh5RRAAAAAAAAA"
		String clientTokenStr = "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"
		CMSSignedData cmsSignedData = new CMSSignedData(Base64.decode(clientTokenStr))
		TimeStampToken timeStampToken = new TimeStampToken(cmsSignedData)
		
		ResponseVS respuesta = timeStampService.validateToken(timeStampToken, request.getLocale());
		
		render "- statusCode: ${respuesta.statusCode} - -message: ${respuesta.message}"
		
	}
	
	
	def testAsyncResponse() {
		def ctx = startAsync()
		//ctx.setTimeout(10000);
		
		log.debug("=========== testAsyncResponse")
		def future = callAsync {
			 Thread.sleep(6000)
			 return "Hello from async controller"
		}
		String respuesta = future.get()
		render respuesta
		ctx.complete();
	}

	def testRunAsync() {
		log.debug("Normal log")
		runAsync {
			log.debug("---------- Async log")
			laprueba()
			log.debug("Async log")
		}
		render "Todo ok"
		return false
		Thread.sleep(1000)
	}	

	
	/**
	 * @httpMethod [GET]
	 * @return Parámetros de configuración de Hibernate
	 */
	def hibernate () {
		render hibernateProperties as JSON
		return false
	}
}