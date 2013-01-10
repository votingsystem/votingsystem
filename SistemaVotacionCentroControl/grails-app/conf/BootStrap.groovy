import java.security.Security;
import grails.converters.JSON

class BootStrap {

	def firmaService
	def hibernateProperties
	
	
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
			return it?.format("yyyy-MM-dd' 'HH:mm:ss")
		 }
		 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
	
    def destroy = { }
}
