import grails.converters.JSON
import grails.util.Environment
import org.votingsystem.model.ContextVS
import grails.util.Metadata
import org.votingsystem.util.HttpHelper

import java.text.DateFormat
import java.text.SimpleDateFormat

class BootStrap {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    def grailsApplication
    def filesService
    def signatureVSService
    def timeStampService

    def init = { servletContext ->
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        JSON.registerObjectMarshaller(Date) {
            return dateFormat.format(it);
            //return it?.format("yyyy/MM/dd' 'HH:mm:ss")
        }
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.initModeServer()
        filesService.init()
        signatureVSService.init();
        timeStampService.init()
    }

    def destroy = { }
}
