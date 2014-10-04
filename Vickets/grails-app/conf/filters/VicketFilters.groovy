package filters

import grails.converters.JSON
import org.apache.http.HttpResponse
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.signature.smime.ValidationResult
import org.votingsystem.util.FileUtils;

import javax.servlet.http.HttpServletResponse
import java.security.cert.X509Certificate;
import org.votingsystem.model.TypeVS
import org.springframework.web.multipart.MultipartHttpServletRequest
import javax.servlet.http.HttpServletRequest
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*
import org.springframework.web.servlet.support.RequestContextUtils

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class VicketFilters {

    def signatureVSService
    def grailsApplication
    def messageSource

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
                    log.debug "User agent: " + request.getHeader("User-Agent") + " - Locale: " + request.locale
                    log.debug "-----------------------------------------------------------------------------------"
                }
                if(!params.int("max")) params.max = 20
                if(!params.int("offset")) params.offset = 0
                if(!params.sort) params.sort = "dateCreated"
                if(!params.order) params.order = "desc"
                response.setHeader("Cache-Control", "no-store")
            }
        }

        filemapFilter(action:'FileMap', find:true) {
            before = {
                log.debug "filemapFilter - before "
                if (!(request instanceof MultipartHttpServletRequest)) {
                    return printOutputStream(response,  new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('requestWithoutFile', null, request.getLocale())))
                }
                Map fileMap = ((MultipartHttpServletRequest)request)?.getFileMap();
                Set<String> fileNames = fileMap.keySet()
                for(String key : fileNames) {
                    //String key = fileMap.keySet().iterator().next()
                    if(key.contains(":")) {
                        String[] keySplitted = key.split(":")
                        String fileName = keySplitted[0]
                        ContentTypeVS contentTypeVS = ContentTypeVS.getByName(keySplitted[1])
                        log.debug "---- filemapFilter - file: ${fileName} - contentType: ${contentTypeVS}"
                        if(contentTypeVS == null) {
                            return printOutput(response,new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                                    messageSource.getMessage('unknownContentType', [keySplitted[1]].toArray(),
                                            request.getLocale())))
                        }
                        ResponseVS responseVS = null
                        SMIMEMessage smimeMessageReq = null
                        switch(contentTypeVS) {
                            case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                            case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                                responseVS = signatureVSService.decryptSMIMEMessage(
                                        fileMap.get(key)?.getBytes(), request.getLocale())
                                if(ResponseVS.SC_OK == responseVS.statusCode) smimeMessageReq = responseVS.smimeMessage
                                break;
                            case ContentTypeVS.ENCRYPTED:
                                responseVS = signatureVSService.decryptMessage(
                                        fileMap.get(key)?.getBytes(), request.getLocale())
                                if(ResponseVS.SC_OK == responseVS.statusCode) params[fileName] = responseVS.messageBytes
                                break;
                            case ContentTypeVS.JSON_SIGNED:
                            case ContentTypeVS.SIGNED:
                                try {
                                    smimeMessageReq = new SMIMEMessage(
                                            new ByteArrayInputStream(fileMap.get(key)?.getBytes()));
                                } catch(Exception ex) {
                                    log.error(ex.getMessage(), ex)
                                    return printOutputStream(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                                        messageSource.getMessage('signedDocumentErrorMsg', null, request.getLocale())))
                                }
                                break;
                        }
                        if(smimeMessageReq) {
                            responseVS = signatureVSService.processSMIMERequest(smimeMessageReq, contentTypeVS, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.statusCode) params[fileName] = responseVS.data
                            else params[fileName] = null
                        }
                        if(responseVS != null && ResponseVS.SC_OK != responseVS.getStatusCode())
                            return printOutput(response, responseVS)
                    } else {
                        params[key] = fileMap.get(key)?.getBytes()
                        log.debug "---- filemapFilter - before - file: '${key}' -> without ContentTypeVS"
                    }
                }
            }
        }


        votingSystemFilter(controller:'*', action:'*') {
            before = {
                if("assets".equals(params.controller) || params.isEmpty() || "element".equals(params.controller)) return
                ResponseVS responseVS = null
                try {
                    ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
                    request.contentTypeVS = contentTypeVS
                    log.debug("before - request.contentTypeVS: ${request.contentTypeVS}")
                    if(!contentTypeVS?.isPKCS7()) return;
                    byte[] requestBytes = org.apache.commons.io.IOUtils.toByteArray(request.getInputStream())
                    //log.debug "---- pkcs7DocumentsFilter - before  - consulta: ${new String(requestBytes)}"
                    if(!requestBytes) return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('requestWithoutFile', null, request.getLocale())))
                    switch(contentTypeVS) {
                        case ContentTypeVS.VICKET:
                        case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                        case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                            responseVS =  signatureVSService.decryptSMIMEMessage(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                responseVS = signatureVSService.processSMIMERequest(
                                        responseVS.smimeMessage,contentTypeVS, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) request.messageSMIMEReq = responseVS.data
                            break;
                        case ContentTypeVS.JSON_ENCRYPTED:
                        case ContentTypeVS.ENCRYPTED:
                            responseVS =  signatureVSService.decryptCMS(requestBytes, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                                params.requestBytes = responseVS.messageBytes
                            break;
                        case ContentTypeVS.JSON_SIGNED:
                        case ContentTypeVS.SIGNED:
                            responseVS = signatureVSService.processSMIMERequest(new SMIMEMessage(
                                    new ByteArrayInputStream(requestBytes)), contentTypeVS, request.getLocale())
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) request.messageSMIMEReq = responseVS.data
                            break;
                        case ContentTypeVS.MESSAGEVS:
                            responseVS = signatureVSService.processMessageVS(requestBytes, contentTypeVS, request.getLocale())
                            request.messageVS = responseVS.data?.messageVS
                            request.messageSMIMEReq = responseVS.data?.messageSMIMEReq
                            break;
                        default: return;
                    }
                } catch(Exception ex) {
                    return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('signedDocumentErrorMsg', [ex.getMessage()].toArray(),
                            request.getLocale())))
                }
                if(responseVS != null && ResponseVS.SC_OK !=responseVS.statusCode)
                    return printOutput(response,responseVS)
            }

            after = { model ->
                try {
                    MessageSMIME messageSMIMEReq = request.messageSMIMEReq
                    ResponseVS responseVS = model?.responseVS
                    if(messageSMIMEReq && responseVS){
                        MessageSMIME.withTransaction {
                            messageSMIMEReq = messageSMIMEReq.merge()
                            messageSMIMEReq.getSmimeMessage().setMessageID(
                                    "${grailsApplication.config.grails.serverURL}/messageSMIME/${messageSMIMEReq.id}")
                            messageSMIMEReq.content = messageSMIMEReq.getSmimeMessage().getBytes()
                            if(responseVS.type) messageSMIMEReq.type = responseVS.type
                            if(responseVS.reason) messageSMIMEReq.setReason(responseVS.getReason())
                            if(responseVS.metaInf) messageSMIMEReq.setMetaInf(responseVS.getMetaInf())
                            messageSMIMEReq.save(flush:true)
                        }
                        log.debug "after - saved MessageSMIME - id '${messageSMIMEReq.id}' - type '${messageSMIMEReq.type}'"
                    }
                    if(!responseVS) return;
                    log.debug "after - response status: ${responseVS.getStatusCode()} - contentType: ${responseVS.getContentType()}"
                    switch(responseVS.getContentType()) {
                        case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                        case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                            ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(
                                    responseVS.getMessageBytes(), model.receiverCert, request.getLocale())
                            if(ResponseVS.SC_OK == encryptResponse.statusCode) {
                                encryptResponse.setStatusCode(responseVS.getStatusCode())
                                encryptResponse.setContentType(responseVS.getContentType())
                                return printOutputStream(response, encryptResponse)
                            }
                        case ContentTypeVS.JSON_SIGNED:
                        case ContentTypeVS.SIGNED:
                            if(ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response, responseVS)
                            else return printOutput(response, responseVS)
                        case ContentTypeVS.JSON_ENCRYPTED:
                        case ContentTypeVS.ENCRYPTED:
                        case ContentTypeVS.MULTIPART_ENCRYPTED:
                            ResponseVS encryptResponse
                            if(responseVS.messageBytes && (model.receiverCert || model.receiverPublicKey)) {
                                if(model.receiverPublicKey) {
                                    encryptResponse =  signatureVSService.encryptToCMS(
                                            responseVS.messageBytes, model.receiverPublicKey)
                                } else if(model.receiverCert) {
                                    encryptResponse = signatureVSService.encryptToCMS(
                                            responseVS.messageBytes,model.receiverCert)
                                }
                                if(ResponseVS.SC_OK == encryptResponse.getStatusCode()) {
                                    encryptResponse.setStatusCode(responseVS.getStatusCode())
                                    encryptResponse.setContentType(responseVS.getContentType())
                                }
                                return printOutputStream(response, encryptResponse)
                            } else log.error("missing params - messageBytes && (receiverCert ||receiverPublicKey)")
                            return printOutput(response, responseVS)
                        case ContentTypeVS.HTML:
                        case ContentTypeVS.TEXT:
                            return printOutput(response, responseVS)
                        case ContentTypeVS.JSON:
                            response.status = responseVS.statusCode
                            response.setContentType(ContentTypeVS.JSON.name)
                            render responseVS.getData() as JSON
                            return false
                        case ContentTypeVS.ZIP:
                            response.setHeader("Content-Disposition", "inline; filename='${responseVS.message}'");
                            return printOutputStream(response, responseVS)
                        case ContentTypeVS.PDF:
                            //response.setHeader("Content-disposition", "attachment; filename='${responseVS.message}'")
                            return printOutputStream(response, responseVS)
                        case ContentTypeVS.PEM:
                        case ContentTypeVS.CMS_SIGNED:
                        case ContentTypeVS.TIMESTAMP_RESPONSE:
                        case ContentTypeVS.IMAGE:
                        case ContentTypeVS.TEXT_STREAM:
                            return printOutputStream(response, responseVS)
                    }
                    log.debug("### responseVS not processed ###");
                } catch(Exception ex) {
                    log.error(ex.getMessage(), ex)
                    return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST, ex.getMessage()))
                }
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
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}