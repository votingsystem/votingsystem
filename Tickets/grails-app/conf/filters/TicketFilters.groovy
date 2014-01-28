package filters

import grails.converters.JSON
import org.apache.http.HttpResponse
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.signature.smime.ValidationResult
import org.votingsystem.util.FileUtils;

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
 */
class TicketFilters {

    def signatureVSService
    def grailsApplication
    def messageSource

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
                        SMIMEMessageWrapper smimeMessageReq = null
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
                            case ContentTypeVS.SIGNED:
                                try {
                                    smimeMessageReq = new SMIMEMessageWrapper(
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
                ResponseVS responseVS = null
                try {
                    ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
                    log.debug("before - contentType: ${contentTypeVS}")
                    if(!contentTypeVS?.isPKCS7()) return;
                    byte[] requestBytes = FileUtils.getBytesFromInputStream(request.getInputStream())
                    //log.debug "---- pkcs7DocumentsFilter - before  - consulta: ${new String(requestBytes)}"
                    if(!requestBytes) return printOutput(response, new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            messageSource.getMessage('requestWithoutFile', null, request.getLocale())))
                    switch(contentTypeVS) {
                        case ContentTypeVS.TICKET:
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
                            responseVS = signatureVSService.processSMIMERequest(new SMIMEMessageWrapper(
                                    new ByteArrayInputStream(requestBytes)), contentTypeVS, request.getLocale())
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
                        messageSMIMEReq.getSmimeMessage().setMessageID(
                                "${grailsApplication.config.grails.serverURL}/messageSMIME/${messageSMIMEReq.id}")
                        messageSMIMEReq.content = messageSMIMEReq.getSmimeMessage().getBytes()
                        if(responseVS.type) messageSMIMEReq.type = responseVS.type
                        if(ResponseVS.SC_OK != responseVS.statusCode) messageSMIMEReq.reason = responseVS.message
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
                    case ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED:
                    case ContentTypeVS.SIGNED_AND_ENCRYPTED:
                        ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(
                                responseVS.getMessageBytes(), model.receiverCert, request.getLocale())
                        if(ResponseVS.SC_OK == encryptResponse.statusCode) {
                            encryptResponse.setStatusCode(responseVS.getStatusCode())
                            encryptResponse.setContentType(responseVS.getContentType())
                            return printOutputStream(response, encryptResponse)
                        } else {
                            messageSMIME.reason = encryptResponse.message
                            messageSMIME.save()
                            return printOutput(response, encryptResponse)
                        }
                    case ContentTypeVS.JSON_ENCRYPTED:
                        ResponseVS encryptResponse
                        if(model.receiverPublicKey) {
                            encryptResponse =  signatureVSService.encryptToCMS(
                                    responseVS.getMessage().getBytes(), model.receiverPublicKey)
                        } else if(model.receiverCert) {
                            encryptResponse =  signatureVSService.encryptToCMS(
                                    responseVS.getMessage().getBytes(), model.receiverCert)
                        }
                        if(ResponseVS.SC_OK == encryptResponse.statusCode) {
                            encryptResponse.setStatusCode(responseVS.getStatusCode())
                            encryptResponse.setContentType(responseVS.getContentType())
                            return printOutputStream(response, encryptResponse)
                        } else {
                            messageSMIME.reason = encryptResponse.message
                            messageSMIME.save()
                            return printOutput(response, encryptResponse)
                        }
                        break;
                    case ContentTypeVS.JSON_SIGNED:
                    case ContentTypeVS.SIGNED:
                        if(ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response, responseVS)
                        else return printOutput(response, responseVS)
                    case ContentTypeVS.MULTIPART_ENCRYPTED:
                        if(responseVS.messageBytes && (model.receiverCert || model.receiverPublicKey)) {
                            if(model.receiverPublicKey) {
                                responseVS =  signatureVSService.encryptToCMS(
                                        responseVS.messageBytes, model.receiverPublicKey)
                            } else if(model.receiverCert) {
                                responseVS = signatureVSService.encryptToCMS(responseVS.messageBytes,model.receiverCert)
                            }
                            responseVS.setContentType(ContentTypeVS.MULTIPART_ENCRYPTED)
                            if (ResponseVS.SC_OK == responseVS.statusCode) return printOutputStream(response,responseVS)
                        } else log.error("missing params - messageBytes && (receiverCert ||receiverPublicKey)")
                        return printOutput(response, responseVS)
                    case ContentTypeVS.HTML:
                    case ContentTypeVS.TEXT:
                        return printOutput(response, responseVS)
                    case ContentTypeVS.JSON:
                        response.status = responseVS.statusCode
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
        if(!(responseVS?.data instanceof MessageSMIME) && responseVS?.data?.fileName) response.setHeader(
                "Content-Disposition", "inline; filename='${responseVS.data.fileName}'");
        response.contentLength = responseVS.getMessageBytes().length
        response.setContentType(responseVS.getContentType().getName())
        response.outputStream <<  responseVS.getMessageBytes()
        response.outputStream.flush()
        return false
    }

}