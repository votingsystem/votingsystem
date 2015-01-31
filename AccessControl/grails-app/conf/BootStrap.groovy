import grails.util.Metadata
import org.votingsystem.model.RepresentativeVS
import grails.converters.JSON
import org.votingsystem.model.ContextVS
import org.votingsystem.util.HttpHelper

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class BootStrap {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    def grailsApplication
    def filesService
    def signatureVSService
    def systemService
    def timeStampService

    def init = { servletContext ->
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init()
        signatureVSService.init();
        systemService.init()
        filesService.init()
        timeStampService.init()
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        JSON.registerObjectMarshaller(Date) { return dateFormat.format(it); }
        JSON.registerObjectMarshaller(RepresentativeVS) {
            def returnMap = [:]
            returnMap['optionSelectedId'] = it.optionSelectedId
            returnMap['numRepresentedWithVote'] = it.numRepresentedWithVote
            returnMap['numTotalRepresentations'] = it.numTotalRepresentations
            return returnMap
        }
    }

    def destroy = {}
}
