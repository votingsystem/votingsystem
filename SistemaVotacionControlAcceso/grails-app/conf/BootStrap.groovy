import java.security.Security;
import grails.converters.JSON


/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class BootStrap {

    def firmaService
	def timeStampService
	def pdfService

    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy-MM-dd' 'HH:mm:ss")
        }
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        firmaService.inicializar()
		pdfService.inicializar()
    }
	
    def destroy = {}

}
