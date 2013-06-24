import java.security.Security;
import grails.converters.JSON
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.utils.*
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class BootStrap {

	
	def firmaService
	def pdfService
	def timeStampService
	def encryptionService
	def grailsApplication
	
    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy-MM-dd' 'HH:mm:ss")
        }
		
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
		
		mc.addMailcap("text/plain;;x-java-content-handler=gnu.mail.handler.TextPlain");
		mc.addMailcap("text/html;;x-java-content-handler=gnu.mail.handler.TextHtml");
		mc.addMailcap("text/xml;;x-java-content-handler=gnu.mail.handler.TextXml");
		mc.addMailcap("multipart/*;;x-java-content-handler=gnu.mail.handler.Multipart");
		mc.addMailcap("message/rfc822;;x-java-content-handler=gnu.mail.handler.MessageRFC822");

		CommandMap.setDefaultCommandMap(mc);
		
		
		//We call it here because InitializingBean method interface is called before GORM plugin
		firmaService.afterPropertiesSet()
		pdfService.afterPropertiesSet()
		timeStampService.afterPropertiesSet()
		encryptionService.afterPropertiesSet()
		
		JSON.registerObjectMarshaller(RepresentativeData) {
			def returnMap = [:]
			returnMap['optionSelectedId'] = it.optionSelectedId
			returnMap['numRepresentedWithVote'] = it.numRepresentedWithVote
			returnMap['numTotalRepresentations'] = it.numTotalRepresentations
			return returnMap
		}
		
    }
	
    def destroy = {}

}
