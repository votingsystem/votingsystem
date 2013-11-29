import grails.converters.JSON
import grails.util.Environment
import org.votingsystem.model.ContextVS
import grails.util.Metadata

class BootStrap {

	def grailsApplication
	def filesService
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy/MM/dd' 'HH:mm:ss")
		 }

        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init()
		filesService.init()
    }
	
    def destroy = { }
}