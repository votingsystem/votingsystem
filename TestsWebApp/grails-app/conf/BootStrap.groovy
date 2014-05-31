import grails.converters.JSON
import org.votingsystem.model.ContextVS

class BootStrap {

    def simulationService
    def signatureVSService

    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy/MM/dd' 'HH:mm:ss")
        }
        ContextVS.init()
        signatureVSService.init()
    }

    def destroy = { }

}
