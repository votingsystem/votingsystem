import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS

import java.text.NumberFormat

class BootStrap {

    def systemService
    def timeStampService
    def signatureVSService
    def filesService
    def grailsApplication

    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) { return it?.format("dd MMM yyyy' 'HH:mm") }
        JSON.registerObjectMarshaller(BigDecimal) { return NumberFormat.getCurrencyInstance().format(it); }



        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init()
        systemService.init()
        filesService.init()
        signatureVSService.init();
        timeStampService.init();
    }

    def destroy = { }
}
