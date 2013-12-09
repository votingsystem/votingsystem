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
                params.messageSMIMEReq = null
                params.receiverCert = null
                params.responseVS = null
            }
        }


        votingSystemFilter(controller:'*', action:'*') {
            before = {
                try {
                    ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
                    log.debug("before - contentType: ${contentTypeVS}")
                    if(!contentTypeVS?.isPKCS7()) return;
                    ResponseVS responseVS = null
                    byte[] requestBytes = getBytesFromInputStream(request.getInputStream())
                    //log.debug "---- pkcs7DocumentsFilter - before  - consulta: ${new String(requestBytes)}"
                    if(!requestBytes) {
                        log.debug "before  - PKCS7 REQUEST WITHOUT FILE ------------"
                        response.status = ResponseVS.SC_ERROR_REQUEST
                        render(messageSource.getMessage('requestWithoutFile', null, request.getLocale()))
                        return false
                    }
                    switch(contentTypeVS) {
                        case ContentTypeVS.PDF_SIGNED_AND_ENCRYPTED:
                            responseVS = signatureVSService.decryptMessage(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.statusCode) {
                                requestBytes = responseVS.messageBytes
                                responseVS = pdfService.checkSignature(requestBytes, request.getLocale())
                                if(ResponseVS.SC_OK == responseVS.statusCode) params.pdfDocument = responseVS?.data
                            }
                            if(ResponseVS.SC_OK != responseVS.statusCode) return printText(response, responseVS)
                            break;
                        case ContentTypeVS.PDF_SIGNED:
                            responseVS = pdfService.checkSignature(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK != responseVS.statusCode) return printText(response, responseVS)
                            params.pdfDocument = responseVS.data
                            break;
                        case ContentTypeVS.PDF:
                            params.plainPDFDocument = requestBytes
                            break;
                        case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                            responseVS =  signatureVSService.decryptSMIMEMessage(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                responseVS = processSMIMERequest(responseVS.smimeMessage, params, request)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) params.messageSMIMEReq = responseVS.data
                            break;
                        case ContentTypeVS.ENCRYPTED:
                            responseVS =  signatureVSService.decryptMessage(requestBytes, request.getLocale())
                            params.requestBytes = responseVS.messageBytes
                            break;
                        case ContentTypeVS.SIGNED:
                            responseVS = processSMIMERequest(new SMIMEMessageWrapper(
                                    new ByteArrayInputStream(requestBytes)), params, request)
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) params.messageSMIMEReq = responseVS.data
                            break;
                        default: return;
                    }
                    if(ResponseVS.SC_OK != responseVS.statusCode) {
                        log.error "before - message: ${responseVS?.message}"
                        response.status = responseVS.statusCode
                        render responseVS.message? responseVS.message: "statusCode: ${responseVS.statusCode}"
                        return false
                    }
                } catch(Exception ex) {
                    log.error(ex.getMessage(), ex)
                    response.status = ResponseVS.SC_ERROR_REQUEST
                    render messageSource.getMessage('signedDocumentErrorMsg', null, request.getLocale())
                    return false
                }
            }

            after = {
                MessageSMIME messageSMIMEReq = params.messageSMIMEReq
                ResponseVS responseVS = params.responseVS
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
                    case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                        ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(
                                messageSMIME.smimeMessage, params.receiverCert, request.getLocale())
                        if(ResponseVS.SC_OK == encryptResponse.statusCode)
                            return printOutputStream(response, encryptResponse)
                        else {
                            messageSMIME.metaInf = encryptResponse.message
                            messageSMIME.save()
                            return printText(response, encryptResponse)
                        }
                    case ContentTypeVS.SIGNED:
                        if(ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response, responseVS)
                        else return printText(response, responseVS)
                    case ContentTypeVS.MULTIPART_ENCRYPTED:
                        if(responseVS.messageBytes && (params.receiverCert || params.receiverPublicKey)) {
                            if(params.receiverPublicKey) {
                                responseVS =  signatureVSService.encryptMessage(
                                        responseVS.messageBytes, params.receiverPublicKey)
                            } else if(params.receiverCert) {
                                responseVS = signatureVSService.encryptToCMS(responseVS.messageBytes,params.receiverCert)
                            }
                            if (ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response,responseVS)
                        }
                        return printText(response, responseVS)
                    case ContentTypeVS.TEXT:
                        return printText(response, responseVS)
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

    private boolean printText(HttpServletResponseWrapper response, ResponseVS responseVS) {
        response.status = responseVS.statusCode
        response.setContentType(ContentTypeVS.TEXT.getName())
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

    private ResponseVS processSMIMERequest(SMIMEMessageWrapper smimeMessageReq,Map params, HttpServletRequest request) {
        if (smimeMessageReq?.isValidSignature()) {
            log.debug "---- processSMIMERequest - signature OK - "
            ResponseVS certValidationResponse = null;
            if("voteVS".equals(params.controller)) {
                certValidationResponse = signatureVSService.validateSMIMEVote(smimeMessageReq, request.getLocale())
            } else certValidationResponse = signatureVSService.validateSMIME(smimeMessageReq, request.getLocale())

            MessageSMIME messageSMIME
            if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
                messageSMIME = new MessageSMIME(metaInf:certValidationResponse.message, type:TypeVS.ERROR,
                        content:smimeMessageReq.getBytes())
                MessageSMIME.withTransaction { messageSMIME.save() }
                log.error "*** Filter - processSMIMERequest - failed document validation - request rejected"
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