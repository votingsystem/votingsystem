package org.votingsystem.accesscontrol.service

import org.votingsystem.throwable.ExceptionVS
import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.EventVS
import org.votingsystem.model.TypeVS
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
        new File("${grailsApplication.config.vs.errorsBaseDir}").mkdirs()
        new File("${grailsApplication.config.vs.backupCopyPath}").mkdirs()
        File polymerPlatform = grailsApplication.mainContext.getResource("bower_components/polymer/polymer.js").getFile()
        if(!polymerPlatform.exists()) {
            log.error "Have you executed 'bower install' from web-app dir ???"
        }
	}

 	public Map<String, File> getBackupFiles(EventVS event, TypeVS type) throws ExceptionVS {
		Map<String, File> result = new HashMap<String, File>()
		String datePartPath = DateUtils.getDateStr(event.getDateFinish(), "yyyy_MM_dd")
		String baseDirPath ="${grailsApplication.config.vs.backupCopyPath}/${datePartPath}/EventVS_${event.id}"
		String filesDirPath = "$baseDirPath/${type.toString()}"
		result.baseDir = new File(baseDirPath)
		result.baseDir.mkdirs()
		result.filesDir = new File(filesDirPath)
		result.filesDir.mkdirs()
		switch(type) {
			case TypeVS.REPRESENTATIVE_DATA:
				result.metaInfFile = new File("$filesDirPath/meta.inf")
				result.reportFile = new File("$baseDirPath/${type.toString()}_REPORTS.csv")
				result.zipResult = new File("$baseDirPath/${type.toString()}_EventVS_${event.id}.zip")
				break;
			case TypeVS.VOTING_EVENT:
				result.metaInfFile = new File("${baseDirPath}/meta.inf")
				result.zipResult = new File("$baseDirPath/${type.toString()}_${event.id}.zip")
				break;
			default: throw new ExceptionVS("unprocessed type: ${type}")
		}
		return result
	 }			 

}