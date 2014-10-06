import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS
import org.votingsystem.vicket.util.ApplicationContextHolder

class BootStrap {

    def systemService
    def timeStampService
    def signatureVSService
    def filesService
    def grailsApplication

    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) { return it?.format("dd MMM yyyy' 'HH:mm") }
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init(ApplicationContextHolder.getInstance())
        signatureVSService.init();
        systemService.init()
        filesService.init()
        timeStampService.init();
    }

    def destroy = { }
}
