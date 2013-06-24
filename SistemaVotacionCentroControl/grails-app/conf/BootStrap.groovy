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
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
			return it?.format("yyyy-MM-dd' 'HH:mm:ss")
		 }
		 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		 
		 
		 MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
		 
		 mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		 mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		 mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		 mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
		 mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
 
		 CommandMap.setDefaultCommandMap(mc);
				 
		 
		 
		 firmaService.inicializar()
		 encryptionService.afterPropertiesSet()
		 
    }
	
    def destroy = { }
}
