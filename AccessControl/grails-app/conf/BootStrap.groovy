import grails.util.Metadata
import org.votingsystem.model.RepresentativeVS
import grails.converters.JSON
import org.votingsystem.model.ContextVS

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class BootStrap {

	def grailsApplication
	def filesService
    def signatureVSService
    def timeStampService
	
    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) { return it?.format("yyyy/MM/dd' 'HH:mm:ss") }
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
		ContextVS.init()
		filesService.init()
        signatureVSService.initService();
        timeStampService.initService();
		JSON.registerObjectMarshaller(RepresentativeVS) {
			def returnMap = [:]
			returnMap['optionSelectedId'] = it.optionSelectedId
			returnMap['numRepresentedWithVote'] = it.numRepresentedWithVote
			returnMap['numTotalRepresentations'] = it.numTotalRepresentations
			return returnMap
		}
    }
	
    def destroy = {}
}
