import grails.converters.JSON
import org.votingsystem.model.ContextVS

class BootStrap {

    def signatureVSService
    def grailsApplication

    def init = { servletContext ->
        JSON.registerObjectMarshaller(Date) {
            return it?.format("yyyy/MM/dd' 'HH:mm:ss")
        }
        ContextVS.init()
        File polymerPlatform = grailsApplication.mainContext.getResource("bower_components/polymer/polymer.js").getFile()
        if(!polymerPlatform.exists()) {
            log.error "Have you executed 'bower install' from web-app dir ???"
        }
        signatureVSService.init()
    }

    def destroy = { }

}
