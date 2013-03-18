import java.security.Security;
import grails.converters.JSON
import grails.util.Environment

class BootStrap {

	def firmaService
	
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
			return it?.format("yyyy-MM-dd' 'HH:mm:ss")
		 }
		 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		 firmaService.inicializar()
    }
	
    def destroy = { }
}
