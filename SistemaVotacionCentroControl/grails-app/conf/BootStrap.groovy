import java.security.Security;
import grails.converters.JSON
import grails.util.Environment
import org.sistemavotacion.utils.*
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
		 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		 firmaService.inicializar()
		 encryptionService.afterPropertiesSet()
		 filesService.init()
    }
	
    def destroy = { }
}
