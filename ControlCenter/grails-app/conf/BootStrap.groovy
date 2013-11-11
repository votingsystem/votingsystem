import java.security.Security;
import grails.converters.JSON
import grails.util.Environment
import org.votingsystem.groovy.util.*
import org.votingsystem.model.ContextVS
import grails.util.Metadata
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

class BootStrap {

	def firmaService
	def encryptionService
	def grailsApplication
	def filesService
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy/MM/dd' 'HH:mm:ss")
		 }
		
		ContextVS.init("org.votingsystem.accesscontrol.model.Usuario")
		 
		 log.debug("isWarDeployed: ${Metadata.current.isWarDeployed()}")
		 firmaService.inicializar()
		 encryptionService.afterPropertiesSet()
		 filesService.init()
    }
	
    def destroy = { }
}
