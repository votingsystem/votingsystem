package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils

import java.security.cert.X509Certificate
import java.text.DecimalFormat
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventVSManifestService {
		
    static transactional = true

    def signatureVSService
    def eventVSService
    def grailsApplication
	def messageSource
	def filesService
	def sessionFactory
	def timeStampVSService

	public ResponseVS saveManifest(PDFDocumentVS pdfDocument, EventVS eventVS, Locale locale) {
		PDFDocumentVS documento = PDFDocumentVS.findWhere(eventVS:eventVS, state:PDFDocumentVS.State.VALIDATED_MANIFEST)
		String messageValidacionDocumento
		if(documento) {
			messageValidacionDocumento = messageSource.getMessage('pdfManifestRepeated',
				[eventVS.subject, documento.userVS?.nif].toArray(), locale)
			log.debug ("saveManifest - ${messageValidacionDocumento}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageValidacionDocumento)
		} else {
			pdfDocument.state = PDFDocumentVS.State.VALIDATED_MANIFEST
			pdfDocument.eventVS = eventVS
			pdfDocument.save()
			eventVS.state = EventVS.State.ACTIVE
			eventVS.userVS = pdfDocument.userVS
			eventVS.save()
			messageValidacionDocumento = messageSource.getMessage('pdfManifestOK',
				[eventVS.subject, pdfDocument.userVS?.nif].toArray(), locale)
			log.debug ("saveManifest - ${messageValidacionDocumento}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				message:messageValidacionDocumento)
		}

	}

	public synchronized ResponseVS generarCopiaRespaldo(EventVSManifest eventVS, Locale locale) {
		log.debug("generarCopiaRespaldo - eventId: ${eventVS.id}")
		ResponseVS responseVS;
		if(!eventVS) {
			return responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage(
				'eventVSNotFound', [eventVS.id].toArray(), locale))
		}

		int numSignatures = PDFDocumentVS.countByEventVSAndState(eventVS, PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		Map<String, File> mapFiles = filesService.getBackupFiles(
			eventVS, TypeVS.MANIFEST_EVENT, locale)
		File metaInfFile = mapFiles.metaInfFile
		File filesDir = mapFiles.filesDir
		File zipResult   = mapFiles.zipResult
		
		String serviceURLPart = messageSource.getMessage(
			'manifestsBackupPartPath', [eventVS.id].toArray(), locale)
		String datePathPart = DateUtils.getShortStringFromDate(eventVS.getDateFinish())
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		
		if(zipResult.exists()) {
			log.debug("generarCopiaRespaldo - backup file already exists")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL)
		}
		
		Set<X509Certificate> systemTrustedCerts = signatureVSService.getTrustedCerts()
		byte[] systemTrustedCertsPEMBytes = CertUtil.getPEMEncoded(systemTrustedCerts)
		File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
		systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
		
		byte[] timeStampCertPEMBytes = timeStampVSService.getSigningCert()
		File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
		timeStampCertFile.setBytes(timeStampCertPEMBytes)
		
		def backupMetaInfMap = [numSignatures:numSignatures]
		Map eventMetaInfMap = eventVSService.getMetaInfMap(eventVS)
		eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
		event.metaInf = eventMetaInfMap as JSON
		EventVS.withTransaction {
			eventVS.save()
		}

		metaInfFile.write((eventMetaInfMap as JSON).toString())
		
		DecimalFormat formatted = new DecimalFormat("00000000");
		int signaturesBatch = 0;
		String baseDir="${filesDir.absolutePath}/batch_${formatted.format(++signaturesBatch)}"
		new File(baseDir).mkdirs()
		
		String fileNamePrefix = messageSource.getMessage(
			'manifestSignatureLbl', null, locale);
		long begin = System.currentTimeMillis()
		PDFDocumentVS.withTransaction {
			def criteria = PDFDocumentVS.createCriteria()
			def firmasRecibidas = criteria.scroll {
				eq("eventVS", eventVS)
				eq("state", PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
			}
			while (firmasRecibidas.next()) {
				PDFDocumentVS firma = (PDFDocumentVS) firmasRecibidas.get(0);
				File pdfFile = new File("${baseDir}/${fileNamePrefix}_${String.format('%08d', firmasRecibidas.getRowNumber())}.pdf")
				pdfFile.setBytes(firma.pdf)
				if((firmasRecibidas.getRowNumber() % 100) == 0) {
					String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
						System.currentTimeMillis() - begin)
					log.debug(" - accessRequest ${firmasRecibidas.getRowNumber()} of ${numSignatures} - ${elapsedTimeStr}");
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
				}
				if(((firmasRecibidas.getRowNumber() + 1) % 2000) == 0) {
					baseDir="${filesDir.absolutePath}/batch_${formatted.format(++signaturesBatch)}"
					new File(baseDir).mkdirs()
				}
			}
		}
		
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: filesDir) {
			fileset(dir:"${filesDir.absolutePath}/..", includes: "meta.inf")
		}
		ant.copy(file: zipResult, tofile: webappBackupPath)
		
		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			 message:backupURL, type:TypeVS.MANIFEST_EVENT)
	}

}