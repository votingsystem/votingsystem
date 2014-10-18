package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils

import java.security.cert.X509Certificate
import java.text.DecimalFormat

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class EventVSManifestService {
		
    static transactional = true

    def signatureVSService
    def eventVSService
    def grailsApplication
	def messageSource
	def filesService
	def sessionFactory
	def timeStampService

	public ResponseVS saveManifest(PDFDocumentVS pdfDocument, EventVS eventVS) {
		PDFDocumentVS document = PDFDocumentVS.findWhere(eventVS:eventVS, state:PDFDocumentVS.State.VALIDATED_MANIFEST)
		String msg
		if(document) {
			msg = messageSource.getMessage('pdfManifestRepeated',
				[eventVS.subject, document.userVS?.nif].toArray(), locale)
			log.debug ("saveManifest - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,  message:msg)
		} else {
			pdfDocument.state = PDFDocumentVS.State.VALIDATED_MANIFEST
			pdfDocument.eventVS = eventVS
			pdfDocument.save()
			eventVS.state = EventVS.State.ACTIVE
			eventVS.userVS = pdfDocument.userVS
			eventVS.save()
			msg = messageSource.getMessage('pdfManifestOK', [eventVS.subject, pdfDocument.userVS?.nif].toArray(),
                    locale)
			log.debug ("saveManifest - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg)
		}

	}

	public synchronized ResponseVS generateBackup(EventVSManifest eventVS) {
		log.debug("generateBackup - eventId: ${eventVS.id}")
		if(!eventVS) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('eventVSNotFound', [eventVS.id].toArray(), locale))
        ResponseVS responseVS;
		int numSignatures = PDFDocumentVS.countByEventVSAndState(eventVS, PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		Map<String, File> mapFiles = filesService.getBackupFiles(eventVS, TypeVS.MANIFEST_EVENT)
		File metaInfFile = mapFiles.metaInfFile
		File filesDir = mapFiles.filesDir
		File zipResult   = mapFiles.zipResult
		String serviceURLPart = messageSource.getMessage('manifestsBackupPartPath', [eventVS.id].toArray(),
                locale)
		String datePathPart = DateUtils.getDateStr(eventVS.getDateFinish(), "yyyy/MM/dd")
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		
		if(zipResult.exists()) {
			log.debug("generateBackup - backup file already exists")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.MANIFEST_EVENT, message:backupURL)
		}
		
		Set<X509Certificate> systemTrustedCerts = signatureVSService.getTrustedCerts()
		byte[] systemTrustedCertsPEMBytes = CertUtils.getPEMEncoded(systemTrustedCerts)
		File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
		systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
		
		byte[] timeStampCertPEMBytes = timeStampService.getSigningCertPEMBytes()
		File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
		timeStampCertFile.setBytes(timeStampCertPEMBytes)
		
		def backupMetaInfMap = [numSignatures:numSignatures]
		Map eventMetaInfMap = eventVSService.getMetaInfMap(eventVS)
		eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
        eventVS.metaInf = "${eventMetaInfMap as JSON}"
		EventVS.withTransaction { eventVS.save() }
		metaInfFile.write(eventVS.metaInf)
		
		DecimalFormat formatted = new DecimalFormat("00000000");
		int signaturesBatch = 0;
		String baseDir="${filesDir.absolutePath}/batch_${formatted.format(++signaturesBatch)}"
		new File(baseDir).mkdirs()
		
		String fileNamePrefix = messageSource.getMessage('manifestSignatureLbl', null, locale);
		long begin = System.currentTimeMillis()
		PDFDocumentVS.withTransaction {
			def criteria = PDFDocumentVS.createCriteria()
			def signaturesCollected = criteria.scroll {
				eq("eventVS", eventVS)
				eq("state", PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
			}
			while (signaturesCollected.next()) {
				PDFDocumentVS firma = (PDFDocumentVS) signaturesCollected.get(0);
				File pdfFile = new File("${baseDir}/${fileNamePrefix}_${String.format('%08d', signaturesCollected.getRowNumber())}.pdf")
				pdfFile.setBytes(firma.pdf)
				if((signaturesCollected.getRowNumber() % 100) == 0) {
					String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
						System.currentTimeMillis() - begin)
					log.debug(" - accessRequest ${signaturesCollected.getRowNumber()} of ${numSignatures} - ${elapsedTimeStr}");
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
				}
				if(((signaturesCollected.getRowNumber() + 1) % 2000) == 0) {
					baseDir="${filesDir.absolutePath}/batch_${formatted.format(++signaturesBatch)}"
					new File(baseDir).mkdirs()
				}
			}
		}
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: filesDir) {
			fileset(dir:"${filesDir.absolutePath}/..", includes: "meta.inf")
		}
        //The file is copied and available to download but triggers a null pointer exception with ResourcesPlugin
		ant.copy(file: zipResult, tofile: webappBackupPath)
		return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL, type:TypeVS.MANIFEST_EVENT)
	}

}