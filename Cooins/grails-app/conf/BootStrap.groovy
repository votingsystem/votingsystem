import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS
import org.votingsystem.util.DateUtils
import org.votingsystem.cooin.util.ApplicationContextHolder

class BootStrap {

    def systemService
    def timeStampService
    def signatureVSService
    def filesService
    def grailsApplication

    def init = { servletContext ->
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        JSON.registerObjectMarshaller(Date) { return DateUtils.getDayWeekDateStr(it) }
        JSON.registerObjectMarshaller(BigDecimal) { return it.toPlainString() }
        ContextVS.init(ApplicationContextHolder.getInstance())
        signatureVSService.init();
        systemService.init()
        filesService.init()
        timeStampService.init();
    }

    def destroy = { }
}
