package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
/**
 * @infoController Mensajes firmados
 * @descController Servicios relacionados con los messages firmados manejados por la
 *                 aplicación.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class MessageSMIMEController {
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/messageSMIME/$id] 
	 * @param [id]	Obligatorio. Identificador del message en la base de datos
	 * @return El message solicitado.
	 */
	def index() { 
        def messageSMIME
		MessageSMIME.withTransaction{
			messageSMIME = MessageSMIME.get(params.long('id'))
		}
        if (messageSMIME) {
            if(ContentTypeVS.TEXT != request.contentTypeVS) {
                String receiptPageTitle = null;
                String receipt = new String(messageSMIME.content, "UTF-8")
                String signedContent = messageSMIME.getSmimeMessage()?.getSignedContent()

                params.messageSMIME = messageSMIME
                forward(controller:"receipt")

                //render(view:"receiptViewer" , model:[receiptPageTitle:receiptPageTitle, receipt:receipt, signedContent:signedContent])
            } else {
                return [responseVS : new ResponseVS(statusCode:ResponseVS.SC_OK, contentType:ContentTypeVS.TEXT_STREAM,
                        messageBytes:messageSMIME.content)]
            }
        } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'messageSMIMENotFound', args:[params.id]))]
	}
	
	
	/**
     * Servicio que devuelve el recibo con el que el servidor respondió un message
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/messageSMIME/receipt/$requestMessageId]
	 * @param [requestMessageId] Obligatorio. Identificador del message origen del recibo en la base de datos
	 * @return El recibo asociado al message pasado como parámetro.
	 */
	def receipt() {
		def messageSMIMEOri = MessageSMIME.get(params.long('requestMessageId'))
		if (messageSMIMEOri) {
			def messageSMIME = MessageSMIME.findWhere(smimeParent:messageSMIMEOri, type: TypeVS.RECEIPT)
			if (messageSMIME) {
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT_STREAM,
                        messageBytes: messageSMIME.content)]
			}
		}
        return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'messageSMIMENotFound', args:[params.smimeParentId]))]
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