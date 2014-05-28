package filters

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS

import javax.servlet.http.HttpServletResponse
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import grails.converters.JSON

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
*/
class VotingSystemTestFilters {


    def filters = {
        paramsCheck(controller:'*', action:'*') {
            before = {
                if("assets".equals(params.controller) || params.isEmpty()) return
                log.debug "###########################<${params.controller}> - before ###################################"
                log.debug "Method: " + request.method
                log.debug "Params: " + params
				log.debug "request.contentType: " + request.contentType
                log.debug "getRemoteHost: " + request.getRemoteHost()
                log.debug "Request: " + request.getRequestURI()  + " - RemoteAddr: " + request.getRemoteAddr()
                log.debug "User agent: " + request.getHeader("User-Agent")
                log.debug "-----------------------------------------------------------------------------------"
				if(!params.int("max")) params.max = 20
                if(!params.int("offset")) params.offset = 0
                if(!params.sort) params.sort = "dateCreated"
                if(!params.order) params.order = "desc"
                response.setHeader("Cache-Control", "no-store")
            }

            after = { model ->
                if(model?.responseVS == null) return;
                ResponseVS responseVS = model.responseVS
                switch(responseVS.getContentType()) {
                    case ContentTypeVS.TEXT:
                        return printText(response, responseVS);
                    case ContentTypeVS.JSON:
                        render responseVS.getData() as JSON
                        return false
                }
            }

        }


    }

    private boolean printText(HttpServletResponse response, ResponseVS responseVS) {
        response.status = responseVS.statusCode
        response.setContentType(ContentTypeVS.TEXT.getName())
        String resultMessage = responseVS.message? responseVS.message: "statusCode: ${responseVS.statusCode}"
        if(ResponseVS.SC_OK != response.status) log.error "after - message: '${resultMessage}'"
        response.outputStream <<  resultMessage
        response.outputStream.flush()
        return false
    }
	
}