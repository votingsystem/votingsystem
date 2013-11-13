import java.security.Security;
import grails.converters.JSON
import org.votingsystem.model.ContextVS

class BootStrap {

	def simulationService
	
    def init = { servletContext ->
		JSON.registerObjectMarshaller(Date) {
			return it?.format("yyyy/MM/dd' 'HH:mm:ss")
		}
		
		ContextVS.init("org.votingsystem.model.UserVSBase")
    }
	
    def destroy = { }

}
