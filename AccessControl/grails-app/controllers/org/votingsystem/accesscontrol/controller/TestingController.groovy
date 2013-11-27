package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.SignatureVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.DateUtils
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
	def timeStampVSService
	def signatureVSService
	def accessControlBackgroundService
	def applicationContext
	def publishService
	def eventVSService
	def pdfService
	def mailSenderService
	def hibernateProperties


    def index() {
        CertificateVS certificate = null
        CertificateVS.withTransaction { certificate = CertificateVS.findBySerialNumber("6156340016997887657")}
        render "certificate.id: ${certificate.id}"
    }

	def count() {
        /*int numTestCerts = CertificateVS.countByType(CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST)
		render "numTestCerts: ${numTestCerts}"*/

        EventVS eventVS = EventVS.get(0)
        log.debug(" ======= eventVS: ${eventVS.id}")
        SignatureVS.withTransaction {
            def numSignatures = SignatureVS.countByEventVS(eventVS)
            log.debug(" ======= numSignatures: ${numSignatures}")
        }

        return false
	}
	

	
	def votes() {
		def votes = null
		EventVS event
		EventVS.withTransaction {
			event = EventVS.get(6)
		}
		
		VoteVS.withTransaction {
			//votes = VoteVS.findAllWhere(state:VoteVS.State.OK, eventVSElection:event)
			def criteria = VoteVS.createCriteria()
			votes = criteria.scroll {
				//maxResults(50)
				eq("state", VoteVS.State.OK)
				eq("eventVSElection", event)
			}
			log.debug("--- votes.getClass(): ${votes.getClass()}")
			int iteration = 0;
			while (votes.next() ) {
				def vote = (VoteVS) votes.get(0);
				log.debug("iteration: ${++iteration} - vote: ${vote.id}")
			}
		}	
	}
	
	
	def asyncMeta() {
		EventVSElection eventVS
		EventVSElection.withTransaction {
			eventVS = EventVS.get(params.id)
		}
		ResponseVS responseVS = representativeService.getAccreditationsMapForEvent(eventVS, request.locale)
		render responseVS.data as JSON
	}

	
	def processTokenRequest() {
		//Vote OK -> String timeStampRequestStr = "MFYCAQEwUTANBglghkgBZQMEAgMFAARANunGRBds3c+Md5MBWNwVFGHklPwRn4mjvXIbZefKhSrG7c6crUViButcrCxhEERAoVxpF2hTfJ8PK2j5u5t16w=="
		//reclamacion OK android -> String timeStampRequestStr = "MDkCAQEwMTANBglghkgBZQMEAgEFAAQg5CXdYT/wQKfj57LE0VOUIkQxwtHGUCo4Xb/nDYRKYHsBAf8="
		 String timeStampRequestStr = "MDkCAQEwMTANBglghkgBZQMEAgMFAAQgrbiEUT6OZejyQSkNg1PqR48b+s9pudY512Dm3Oow4H0BAf8="                                                       
		
		
		ResponseVS responseVS = timeStampVSService.processRequest(Base64.decode(timeStampRequestStr), request.getLocale())
		byte[] encodedTimestamp = responseVS.messageBytes
		String encodedTimestampStr =  new String(Base64.encode(encodedTimestamp));
		render encodedTimestampStr
		return false
	}
	
	def validateToken() {
		//String tokenStr = "MIAGCSqGSIb3DQEHAqCAMIACAQMxDzANBglghkgBZQMEAgEFADCABgsqhkiG9w0BCRABBKCAJIAETTBLAgEBBgIqAzAhMAkGBSsOAwIaBQAEFNAdneKV6YgJ43QzgmtShRJ9olrMAgECGA8yMDEzMDUyNjA5NTUxNFowCwIBAYACAfSBAgH0AAAAAAAAMYIBnTCCAZkCAQEwWDBOMTUwMwYDVQQDDCxBdXRvcmlkYWQgQ2VydGlmaWNhZG9yYSBTaXN0ZW1hIGRlIFZvdGFjacOzbjEVMBMGA1UECwwMQ2VydGlmaWNhZG9zAgYBPqKDu7YwDQYJYIZIAWUDBAIBBQCggZgwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xMzA1MjYwOTU1MTRaMCsGCyqGSIb3DQEJEAIMMRwwGjAYMBYEFB03+2TSvT+K6Uwsq4pkC3CYQ0twMC8GCSqGSIb3DQEJBDEiBCB9c/VH18gSlsUTjDK4ObvcSeeUpJ7iMtDE/xi7LlB/xTANBgkqhkiG9w0BAQEFAASBgEI1QSu1nM0czLOL2bfBog4byqTrcpxSsILPkLo0+IPMBT6Me6CMsQRLszXYWxuwaOvQisDZlEHJXI+T3e6+h6oTBOYeA4PaLKazNtGi+o3OcEmv0jjLvKUMsnG+ngvIVPK+OdkOtLXP6bFgLJvGPHgPHG/t0/CsDjEDv7jh5RRAAAAAAAAA"
		String clientTokenStr = "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"
		CMSSignedData cmsSignedData = new CMSSignedData(Base64.decode(clientTokenStr))
		TimeStampToken timeStampToken = new TimeStampToken(cmsSignedData)
		
		ResponseVS responseVS = timeStampVSService.validateToken(timeStampToken, request.getLocale());
		
		render "- statusCode: ${responseVS.statusCode} - -message: ${responseVS.message}"
		
	}
	
	
	def testAsyncResponse() {
		def ctx = startAsync()
		//ctx.setTimeout(10000);
		
		log.debug("=========== testAsyncResponse")
		def future = callAsync {
			 Thread.sleep(6000)
			 return "Hello from async controller"
		}
		/*
		 * 				ctx.response.status = 200
				ctx.response.setContentType(ContentTypeVS.TEXT)
				ctx.response.contentLength = voteVSCanceller.messageSMIME.content.length
				ctx.response.outputStream <<  voteVSCanceller.messageSMIME.content
				ctx.response.outputStream.flush()
		 */
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