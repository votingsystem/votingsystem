package org.votingsystem.controlcenter.service

import java.util.Date;
import java.util.Locale;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.controlcenter.model.*;
import org.votingsystem.util.DateUtils;
import java.security.cert.X509Certificate;
import org.votingsystem.signature.util.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class FilesService {
	
	static transactional = true

	def grailsApplication
	def messageSource

	public void init() {
		File eventsMetaInfDir = new File("${grailsApplication.config.VotingSystem.eventsMetaInfBaseDir}")
		eventsMetaInfDir.mkdirs()
		File errorsDir = new File("${grailsApplication.config.VotingSystem.errorsBaseDir}")
		errorsDir.mkdirs()
	}	 

	 public File getEventMetaInf(EventoVotacion event) {
		 String datePathPart = DateUtils.getShortStringFromDate(event.fechaInicio)
		 String eventsMetaInfBaseDirPath = "${grailsApplication.config.VotingSystem.eventsMetaInfBaseDir}" +
			 "/${datePathPart}"
		 File metaInfFile = new File("${eventsMetaInfBaseDirPath}/meta_event_${event.id}.inf")
		 return metaInfFile
	 }

}
