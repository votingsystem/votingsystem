import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS

class BootStrap {

    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) { return it?.format("yyyy/MM/dd' 'HH:mm:ss") }
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init()
    }

    def destroy = { }

}
