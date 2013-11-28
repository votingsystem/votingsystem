package org.votingsystem.accesscontrol.service

import org.votingsystem.model.EventVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class FilesService {
	
	static transactional = true

	def grailsApplication
	def messageSource

	public void init() {
        new File("${grailsApplication.config.VotingSystem.errorsBaseDir}").mkdirs()
        new File("${grailsApplication.config.VotingSystem.backupCopyPath}").mkdirs()
	}
	
 	public Map<String, File> getBackupFiles(EventVS event, TypeVS type, Locale locale){
		 String servicePathPart = null
		 Map<String, File> result = new HashMap<String, File>()
		 String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
		 String baseDirPath ="${grailsApplication.config.VotingSystem.backupCopyPath}/${datePathPart}/Event_${event.id}"
		 String filesDirPath = null
		 String zipFilesDirPath = "${baseDirPath}/zip"
		 new File(zipFilesDirPath).mkdirs()
		 result.metaInfFile = new File("${baseDirPath}/meta.inf")
		 switch(type) {
			 case TypeVS.REPRESENTATIVE_DATA:
				 servicePathPart = messageSource.getMessage('repAccreditationsBackupPartPath', null, locale)
				 filesDirPath = "${baseDirPath}/files/${servicePathPart}"
				 new File(filesDirPath).mkdirs()
				 String reportPathPart = messageSource.getMessage('representativeReport', null, locale)
				 //result.representativesReportFile = new File("${filesDirPath}/${reportPathPart}.csv")
				 result.filesDir = new File(filesDirPath)
				 break; 
			 case TypeVS.VOTING_EVENT:
				 servicePathPart = messageSource.getMessage('votingBackupPartPath', [event.id].toArray(), locale)
				 filesDirPath = "${baseDirPath}/files"
				 result.filesDir = new File(filesDirPath)
				 break;
			case TypeVS.MANIFEST_EVENT:
				servicePathPart = messageSource.getMessage('manifestsBackupPartPath', [event.id].toArray(), locale)
				result.filesDir = new File("${baseDirPath}/files") 
				break;
			case TypeVS.CLAIM_EVENT:
				servicePathPart = messageSource.getMessage('claimsBackupPartPath', [event.id].toArray(), locale)
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

}

