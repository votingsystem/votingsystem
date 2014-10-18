package filters

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.util.MetaInfMsg

import javax.servlet.http.HttpServletResponse

import java.security.cert.X509Certificate;
import org.votingsystem.model.TypeVS
import org.springframework.web.multipart.MultipartHttpServletRequest
import javax.servlet.http.HttpServletRequest
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * */
class ControlCenterFilters {


    def grailsApplication
    def messageSource
    def signatureVSService

    def filters = {
        paramsCheck(controller:'*', action:'*') {
            before = {
                if("assets".equals(params.controller) || params.isEmpty()) return
                if(!"element".equals(params.controller)) {
                    log.debug "###########################<${params.controller}> - before ################################"
                    log.debug "Method: " + request.method
                    log.debug "Params: " + params
                    log.debug "request.contentType: " + request.contentType
                    log.debug "getRemoteHost: " + request.getRemoteHost()
                    log.debug "Request: " + request.getRequestURI()  + " - RemoteAddr: " + request.getRemoteAddr()
                    log.debug "User agent: " + request.getHeader("User-Agent")
                    log.debug "-----------------------------------------------------------------------------------"
                }
                if(!params.int("max")) params.max = 20
                if(!params.int("offset")) params.offset = 0
                if(!params.sort) params.sort = "dateCreated"
                if(!params.order) params.order = "desc"
                response.setHeader("Cache-Control", "no-store")
            }
        }


        votingSystemFilter(controller:'*', action:'*') {
            before = {
                if("assets".equals(params.controller) || params.isEmpty() || "element".equals(params.controller)) return
                ResponseVS responseVS = null
                byte[] requestBytes = null
                try {
                    request.contentTypeVS = ContentTypeVS.getByName(request?.contentType)
                    if(!request.contentTypeVS?.isPKCS7()) return;
                    requestBytes = getBytesFromInputStream(request.getInputStream())
                    //log.debug "before  - requestBytes: ${new String(requestBytes)}"
                    if(!requestBytes) return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('requestWithoutFile', null, request.getLocale())))
                    switch(request.contentTypeVS) {
                        case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                        case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                            responseVS =  signatureVSService.decryptSMIMEMessage(requestBytes)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                responseVS = processSMIMERequest(responseVS.smimeMessage,request.contentTypeVS, params, request)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) request.messageSMIMEReq = responseVS.data
                            break;
                        case ContentTypeVS.JSON_ENCRYPTED:
                        case ContentTypeVS.ENCRYPTED:
                            responseVS =  signatureVSService.decryptMessage(requestBytes)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                params.requestBytes = responseVS.messageBytes
                            break;
                        case ContentTypeVS.VOTE:
                        case ContentTypeVS.JSON_SIGNED:
                        case ContentTypeVS.SIGNED:
                            responseVS = processSMIMERequest(new SMIMEMessage(
                                    new ByteArrayInputStream(requestBytes)), request.contentTypeVS, params, request)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) request.messageSMIMEReq = responseVS.data
                            break;
                        default: return;
                    }
                    if(responseVS != null && ResponseVS.SC_OK !=responseVS.statusCode)
                        return printOutput(response,responseVS)
                } catch(Exception ex) {
                    return printOutput(response, ResponseVS.getExceptionResponse(params.controller, params.action,
                            ex, StackTraceUtils.extractRootCause(ex)).save())
                }

            }

            after = { model ->
                ResponseVS responseVS = model?.responseVS
                if(!responseVS) return;
                if(responseVS.messageSMIME){
                    MessageSMIME.withTransaction { responseVS.refreshMessageSMIME().save() }
                    log.debug "after - MessageSMIME - id '${responseVS.messageSMIME.id}' - type '${responseVS.messageSMIME.type}'"
                }
                log.debug "after - response status: ${responseVS.getStatusCode()} - contentType: ${responseVS.getContentType()}"
                switch(responseVS.getContentType()) {
                    case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                    case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                        ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(
                                responseVS.getMessageBytes(), model.receiverCert)
                        if(ResponseVS.SC_OK == encryptResponse.statusCode) {
                            encryptResponse.setContentType(responseVS.getContentType())
                            return printOutputStream(response, encryptResponse)
                        } else {
                            messageSMIME.metaInf = encryptResponse.message
                            messageSMIME.save()
                            return printOutput(response, encryptResponse)
                        }
                    case ContentTypeVS.VOTE:
                    case ContentTypeVS.JSON_SIGNED:
                    case ContentTypeVS.SIGNED:
                        if(ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response, responseVS)
                        else return printOutput(response, responseVS)
                    case ContentTypeVS.MULTIPART_ENCRYPTED:
                        if(responseVS.messageBytes && (model.receiverCert || model.receiverPublicKey)) {
                            if(model.receiverPublicKey) {
                                responseVS =  signatureVSService.encryptMessage(responseVS.messageBytes,
                                        model.receiverPublicKey)
                            } else if(model.receiverCert) {
                                responseVS = signatureVSService.encryptToCMS(responseVS.messageBytes,model.receiverCert)
                            }
                            if (ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response,responseVS)
                        }
                        return printOutput(response, responseVS)
                    case ContentTypeVS.HTML:
                    case ContentTypeVS.TEXT:
                        return printOutput(response, responseVS)
                    case ContentTypeVS.JSON:
                        render responseVS.getData() as JSON
                        return false
                    case ContentTypeVS.ZIP:
                        response.setHeader("Content-Disposition", "inline; filename='${responseVS.message}'");
                        return printOutputStream(response, responseVS)
                    case ContentTypeVS.PDF:
                        response.setHeader("Content-disposition", "attachment; filename='${responseVS.message}'")
                        return printOutputStream(response, responseVS)
                    case ContentTypeVS.PEM:
                    case ContentTypeVS.CMS_SIGNED:
                    case ContentTypeVS.TIMESTAMP_RESPONSE:
                    case ContentTypeVS.IMAGE:
                    case ContentTypeVS.TEXT_STREAM:
                        return printOutputStream(response, responseVS)
                }
                log.debug("### responseVS not processed ###");
            }
        }
    }

    private boolean printOutput(HttpServletResponse response, ResponseVS responseVS) {
        response.status = responseVS.statusCode
        response.setContentType(responseVS.getContentType()?.getName()+";charset=UTF-8")
        String resultMessage = responseVS.message? responseVS.message: "statusCode: ${responseVS.statusCode}"
        if(ResponseVS.SC_OK != response.status) log.error "after - message: '${resultMessage}'"
        response.outputStream <<  resultMessage
        response.outputStream.flush()
        return false
    }

    private boolean printOutputStream(HttpServletResponse response, ResponseVS responseVS) {
        response.status = responseVS.getStatusCode()
        response.contentLength = responseVS.getMessageBytes().length
        response.setContentType(responseVS.getContentType().getName())
        response.outputStream <<  responseVS.getMessageBytes()
        response.outputStream.flush()
        return false
    }

    /**
     * requestBytes = "${request.getInputStream()}".getBytes() -> problems working with pdf
     */
    public byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf =new byte[4096];
        int len;
        while((len = inputStream.read(buf)) > 0){ outputStream.write(buf,0,len);}
        outputStream.close();
        inputStream.close();
        return outputStream.toByteArray();
    }

    private ResponseVS processSMIMERequest(SMIMEMessage smimeMessageReq, ContentTypeVS contenType,
           Map params, HttpServletRequest request) {
        if (smimeMessageReq?.isValidSignature()) {
            log.debug "processSMIMERequest - isValidSignature"
            ResponseVS certValidationResponse = null;
            switch(contenType) {
                case ContentTypeVS.VOTE:
                    certValidationResponse = signatureVSService.validateSMIMEVote(smimeMessageReq)
                    break;
                default:
                    certValidationResponse = signatureVSService.validateSMIME(smimeMessageReq);
            }
            MessageSMIME messageSMIME
            if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
                messageSMIME = new MessageSMIME(reason:certValidationResponse.message, content:smimeMessageReq.getBytes(),
                        metaInf:MetaInfMsg.getErrorMsg("processSMIMERequest", "${params.controller}Controller_${params.action}Action"),
                        smimeMessage:smimeMessageReq)
                MessageSMIME.withTransaction { messageSMIME.save() }
                log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
                        " - message: ${certValidationResponse.message}"
                return certValidationResponse
            } else {
                messageSMIME = new MessageSMIME(signers:certValidationResponse.data?.checkedSigners,
                        userVS:certValidationResponse.data?.checkedSigner, smimeMessage:smimeMessageReq,
                        eventVS:certValidationResponse.eventVS, type:TypeVS.OK,
                        content:smimeMessageReq.getBytes(), base64ContentDigest:smimeMessageReq.getContentDigestStr())
                MessageSMIME.withTransaction {messageSMIME.save()}
            }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME)
        } else if(smimeMessageReq) {
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage('signatureErrorMsg', null, request.getLocale()))
        }
    }
}