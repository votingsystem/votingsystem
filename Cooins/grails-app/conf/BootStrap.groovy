import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS
import org.votingsystem.util.DateUtils
import org.votingsystem.cooin.util.ApplicationContextHolder

import java.text.DateFormat
import java.text.SimpleDateFormat

class BootStrap {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    def systemService
    def timeStampService
    def signatureVSService
    def filesService
    def grailsApplication

    def init = { servletContext ->
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        JSON.registerObjectMarshaller(Date) { return dateFormat.format(it); }
        JSON.registerObjectMarshaller(BigDecimal) { return it.toPlainString() }
        ContextVS.init(ApplicationContextHolder.getInstance())
        signatureVSService.init();
        systemService.init()
        filesService.init()
        timeStampService.init();
    }

    def destroy = { }
}
