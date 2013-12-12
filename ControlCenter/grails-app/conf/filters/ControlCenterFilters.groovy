package filters

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME

import javax.servlet.http.HttpServletResponseWrapper
import java.security.cert.X509Certificate;
import org.votingsystem.model.TypeVS
import org.springframework.web.multipart.MultipartHttpServletRequest
import javax.servlet.http.HttpServletRequest
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 *
 * */
class ControlCenterFilters {


    def grailsApplication
    def messageSource
    def signatureVSService

    def filters = {
        paramsCheck(controller:'*', action:'*') {
            before = {
                log.debug "###########################<${params.controller}> - before ################################"
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
        }


        votingSystemFilter(controller:'*', action:'*') {
            before = {
                ResponseVS responseVS = null
                try {
                    ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
                    log.debug("before - contentType: ${contentTypeVS}")
                    if(!contentTypeVS?.isPKCS7()) return;
                    byte[] requestBytes = getBytesFromInputStream(request.getInputStream())
                    //log.debug "---- pkcs7DocumentsFilter - before  - consulta: ${new String(requestBytes)}"
                    if(!requestBytes) return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('requestWithoutFile', null, request.getLocale())))
                    switch(contentTypeVS) {
                        case ContentTypeVS.VOTE:
                        case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                        case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                            responseVS =  signatureVSService.decryptSMIMEMessage(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                responseVS = processSMIMERequest(responseVS.smimeMessage,contentTypeVS, params, request)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) request.messageSMIMEReq = responseVS.data
                            break;
                        case ContentTypeVS.JSON_ENCRYPTED:
                        case ContentTypeVS.ENCRYPTED:
                            responseVS =  signatureVSService.decryptMessage(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                params.requestBytes = responseVS.messageBytes
                            break;
                        case ContentTypeVS.JSON_SIGNED:
                        case ContentTypeVS.SIGNED:
                            responseVS = processSMIMERequest(new SMIMEMessageWrapper(
                                    new ByteArrayInputStream(requestBytes)), contentTypeVS, params, request)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) request.messageSMIMEReq = responseVS.data
                            break;
                        default: return;
                    }
                } catch(Exception ex) {
                    log.error(ex.getMessage(), ex)
                    return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('signedDocumentErrorMsg', null, request.getLocale())))
                }
                if(responseVS != null && ResponseVS.SC_OK !=responseVS.statusCode)
                    return printOutput(response,responseVS)
            }

            after = { model ->
                MessageSMIME messageSMIMEReq = request.messageSMIMEReq
                ResponseVS responseVS = model?.responseVS
                if(messageSMIMEReq && responseVS){
                    MessageSMIME.withTransaction {
                        messageSMIMEReq = messageSMIMEReq.merge()
                        messageSMIMEReq.eventVS = responseVS.eventVS
                        messageSMIMEReq.metaInf = responseVS.message
                        messageSMIMEReq.type = responseVS.type
                        messageSMIMEReq.save(flush:true)
                    }
                    log.debug "after - saved MessageSMIME - id '${messageSMIMEReq.id}' - type '${messageSMIMEReq.type}'"
                }
                if(!responseVS) return;
                MessageSMIME messageSMIME = null
                if(responseVS?.data instanceof MessageSMIME) {
                    messageSMIME = responseVS?.data
                    responseVS.setMessageBytes(messageSMIME.content)
                }
                log.debug "after - response status: ${responseVS.getStatusCode()} - contentType: ${responseVS.getContentType()}"
                switch(responseVS.getContentType()) {
                    case ContentTypeVS.VOTE:
                    case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                        ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(
                                messageSMIME.content, model.receiverCert, request.getLocale())
                        if(ResponseVS.SC_OK == encryptResponse.statusCode) {
                            encryptResponse.setContentType(responseVS.getContentType())
                            return printOutputStream(response, encryptResponse)
                        } else {
                            messageSMIME.metaInf = encryptResponse.message
                            messageSMIME.save()
                            return printOutput(response, encryptResponse)
                        }
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

    private boolean printOutput(HttpServletResponseWrapper response, ResponseVS responseVS) {
        response.status = responseVS.statusCode
        response.setContentType(responseVS.getContentType()?.getName()+";charset=UTF-8")
        String resultMessage = responseVS.message? responseVS.message: "statusCode: ${responseVS.statusCode}"
        if(ResponseVS.SC_OK != response.status) log.error "after - message: '${resultMessage}'"
        response.outputStream <<  resultMessage
        response.outputStream.flush()
        return false
    }

    private boolean printOutputStream(HttpServletResponseWrapper response, ResponseVS responseVS) {
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

    private ResponseVS processSMIMERequest(SMIMEMessageWrapper smimeMessageReq, ContentTypeVS contenType,
           Map params, HttpServletRequest request) {
        if (smimeMessageReq?.isValidSignature()) {
            log.debug "processSMIMERequest - isValidSignature"
            ResponseVS certValidationResponse = null;
            switch(contenType) {
                case ContentTypeVS.VOTE:
                    certValidationResponse = signatureVSService.validateSMIMEVote(smimeMessageReq, request.getLocale())
                    break;
                default:
                    certValidationResponse = signatureVSService.validateSMIME(smimeMessageReq, request.getLocale());
            }
            MessageSMIME messageSMIME
            if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
                messageSMIME = new MessageSMIME(metaInf:certValidationResponse.message, type:TypeVS.ERROR,
                        content:smimeMessageReq.getBytes())
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
            log.error "**** Filter - processSMIMERequest - signature ERROR - "
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage('signatureErrorMsg', null, request.getLocale()))
        }
    }
}