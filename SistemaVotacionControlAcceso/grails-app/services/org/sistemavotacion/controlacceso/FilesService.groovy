package org.sistemavotacion.controlacceso

import java.util.Date;
import java.util.Locale;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.DateUtils;
import java.security.cert.X509Certificate;
import org.sistemavotacion.seguridad.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class FilesService {
	
	static transactional = true

	def grailsApplication
	def messageSource

	public void init() {
		File errorsDir = new File("${grailsApplication.config.SistemaVotacion.errorsBaseDir}")
		errorsDir.mkdirs()
	}
	
 	public Map<String, File> getBackupFiles(Evento event, Tipo type, 
		 	Locale locale){
		 String servicePathPart = null
		 Map<String, File> result = new HashMap<String, File>()
		 String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
		 String baseDirPath = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" +
		 	"/${datePathPart}/Event_${event.id}"
		 String filesDirPath = null
		 String zipFilesDirPath = "${baseDirPath}/zip"
		 new File(zipFilesDirPath).mkdirs()
		 result.metaInfFile = new File("${baseDirPath}/meta.inf")
		 switch(type) {
			 case Tipo.REPRESENTATIVE_DATA:
				 servicePathPart = messageSource.getMessage(
					 'repAccreditationsBackupPartPath', null, locale)
				 filesDirPath = "${baseDirPath}/files/${servicePathPart}"
				 new File(filesDirPath).mkdirs()
				 String reportPathPart = messageSource.getMessage(
					 'representativeReport', null, locale) 
				 //result.representativesReportFile = new File("${filesDirPath}/${reportPathPart}.csv")
				 result.filesDir = new File(filesDirPath)
				 break; 
			 case Tipo.EVENTO_VOTACION:
				 servicePathPart = messageSource.getMessage(
					 'votingBackupPartPath', [event.id].toArray(), locale)
				 filesDirPath = "${baseDirPath}/files"
				 result.filesDir = new File(filesDirPath)
				 break;
			case Tipo.EVENTO_FIRMA:
				servicePathPart = messageSource.getMessage(
					'manifestsBackupPartPath', [event.id].toArray(), locale)
				result.filesDir = new File("${baseDirPath}/files") 
				break;
			case Tipo.EVENTO_RECLAMACION:
				servicePathPart = messageSource.getMessage(
					'claimsBackupPartPath', [event.id].toArray(), locale)
				result.filesDir = new File("${baseDirPath}/files")

				break;
		     default: 
			 	log.error("getBackupZipPath - map files not found for type ${type}")
				return;
		 }
		 if(result.filesDir) result.filesDir.mkdirs();
		 result.zipResult = new File("${zipFilesDirPath}/${servicePathPart}.zip")
		 return result
	 }			 
			 
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
	}

}

