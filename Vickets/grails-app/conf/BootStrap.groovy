import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.util.ApplicationContextHolder

class BootStrap {

    def systemService
    def timeStampService
    def signatureVSService
    def filesService
    def grailsApplication

    def init = { servletContext ->
        DateUtils.getDate_Es()
        JSON.registerObjectMarshaller(Date) {
            Date lastYear = DateUtils.addDays(Calendar.getInstance().getTime(), -365)
            if(it.before(lastYear)) return it?.format("dd MMM yyyy' 'HH:mm")
            else return it?.format("EEE dd MMM' 'HH:mm")
        }
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init(ApplicationContextHolder.getInstance())
        signatureVSService.init();
        systemService.init()
        filesService.init()
        timeStampService.init();
    }

    def destroy = { }
}
