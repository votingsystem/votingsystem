package org.sistemavotacion.controlacceso

import java.util.Date;
import java.util.Locale;

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

 	public Map<String, File> getBackupFiles(Evento evento, Tipo type, 
		 	Locale locale){
		 String servicePathPart = null
		 Map<String, File> result = new HashMap<String, File>()
		 switch(type) {
			 case Tipo.REPRESENTATIVE_ACCREDITATIONS:
				 servicePathPart = messageSource.getMessage(
					 'repAccreditationsBackupPartPath', null, locale)
				 break;
		     default: 
			 	log.error("getBackupZipPath - servicePathPart not found for type ${type}")
				return;
		 }
		 
		 Date selectedDate
		 if(evento.dateCanceled) selectedDate = evento.dateCanceled
		 else selectedDate = evento.fechaFin
		 
		 String datePathPart = DateUtils.getShortStringFromDate(selectedDate)
		 
		 String baseDirPath = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" +
		 	"/${datePathPart}/Event_${evento.id}"
		 String filesDirPath = "${baseDirPath}/files/${servicePathPart}"
		 new File(filesDirPath).mkdirs()
		 String zipFilesDirPath = "${baseDirPath}/zip"
		 new File(zipFilesDirPath).mkdirs()
		 
		  
		 
		 result.zipResult = new File("${zipFilesDirPath}/${servicePathPart}Event_${evento.id}.zip")
		 result.metaInfFile = new File("${filesDirPath}/meta.inf")
		 result.filesDir = new File(filesDirPath)
		 result.representativesReportFile = new File("${filesDirPath}/representativeReport")
		 return result

	 }
		 
}

