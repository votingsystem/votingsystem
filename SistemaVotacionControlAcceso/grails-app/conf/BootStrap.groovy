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
	def filesService
	
    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy/MM/dd' 'HH:mm:ss")
        }
		
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());	
		
		//We call it here because InitializingBean method interface is called before GORM plugin
		firmaService.afterPropertiesSet()
		pdfService.afterPropertiesSet()
		timeStampService.afterPropertiesSet()
		encryptionService.afterPropertiesSet()
		filesService.init()
		
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
