import grails.converters.JSON
import grails.util.Environment
import org.votingsystem.model.ContextVS
import grails.util.Metadata

class BootStrap {

	def signatureVSService
	def encryptionService
	def grailsApplication
	def filesService
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy/MM/dd' 'HH:mm:ss")
		 }

        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init()

        signatureVSService.init()
		encryptionService.afterPropertiesSet()
		filesService.init()
    }
	
    def destroy = { }
}
