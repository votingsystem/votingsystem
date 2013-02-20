import java.security.Security;
import grails.converters.JSON


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class BootStrap {

	
	def firmaService
	def pdfService
	def timeStampService
	
    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy-MM-dd' 'HH:mm:ss")
        }
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		//We call it here because InitializingBean method interface is called before GORM plugin
		firmaService.afterPropertiesSet()
		pdfService.afterPropertiesSet()
		timeStampService.afterPropertiesSet()
    }
	
    def destroy = {}

}
