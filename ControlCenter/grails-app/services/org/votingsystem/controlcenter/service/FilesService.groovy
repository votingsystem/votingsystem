package org.votingsystem.controlcenter.service

import org.votingsystem.model.EventVS
import org.votingsystem.util.DateUtils
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class FilesService {
	
	static transactional = true

	def grailsApplication
	def messageSource

	public void init() {
		new File("${grailsApplication.config.VotingSystem.eventsMetaInfBaseDir}").mkdirs()
		new File("${grailsApplication.config.VotingSystem.errorsBaseDir}").mkdirs()
        File polymerPlatform = grailsApplication.mainContext.getResource("bower_components/polymer/polymer.js").getFile()
        if(!polymerPlatform.exists()) {
            log.error "Have you executed 'bower install' from web-app dir ???"
        }
	}	 

	 public File getEventMetaInf(EventVS event) {
		 String datePathPart = DateUtils.getShortStringFromDate(event.dateBegin)
		 String eventsMetaInfBaseDirPath = "${grailsApplication.config.VotingSystem.eventsMetaInfBaseDir}" +
			 "/${datePathPart}"
		 File metaInfFile = new File("${eventsMetaInfBaseDirPath}/meta_event_${event.id}.inf")
		 return metaInfFile
	 }

}
