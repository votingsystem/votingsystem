import grails.converters.JSON
import grails.util.Metadata
import org.votingsystem.model.ContextVS
import java.text.DateFormat
import java.text.SimpleDateFormat

class BootStrap {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    def systemService

    def init = { servletContext ->
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        JSON.registerObjectMarshaller(Date) { return dateFormat.format(it); }
        log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
        ContextVS.init()
        systemService.init()
    }

    def destroy = { }

}
