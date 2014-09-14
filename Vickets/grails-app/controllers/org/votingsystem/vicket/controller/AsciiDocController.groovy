package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.asciidoctor.ast.ContentPart
import org.asciidoctor.ast.Document
import org.asciidoctor.ast.DocumentHeader
import org.asciidoctor.ast.StructuredDocument
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.asciidoctor.Asciidoctor;

/**
 * @infoController AsciiDoc
 * @descController Servicios relacionados con documentos en formato AsciiDoc
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class AsciiDocController {

	def grailsApplication;
    private String testFile = "./asciiDocksVicketsTest.temp"

    def index() {
        File asciiFile = new File(testFile)
        FileReader reader = new FileReader(asciiFile);
        StringWriter writer = new StringWriter();
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.convert(reader, writer, new HashMap<String, Object>());
        StringBuffer htmlBuffer = writer.getBuffer();
        render htmlBuffer.toString()
    }

    def test() {
        if("POST".equals(request.method)) {
            if(request.contentTypeVS && ContentTypeVS.JSON_SIGNED == request.contentTypeVS) {
                MessageSMIME messageSMIMEReq = request.messageSMIMEReq
                if(!messageSMIMEReq) {
                    return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
                }
                String asciiDocStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
                render loadAsciiDoc(asciiDocStr)
                return false
            } else {
                log.debug(" ${request.JSON.toString()}")
                def requestJSON = request.JSON
                render loadAsciiDoc(requestJSON.asciiDoc)
                return false
            }
        }
    }

    private String loadAsciiDoc(String asciiDocStr) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        String html = asciidoctor.convert(asciiDocStr, new HashMap<String, Object>());
        //StructuredDocument document = asciidoctor.readDocumentStructure(asciiDocStr, new HashMap<String, Object>());
        DocumentHeader header = asciidoctor.readDocumentHeader(asciiDocStr)
        String metaInf = header.getAttributes().get("metaInfVS")
        def metaInfJSON = JSON.parse(metaInf)
        log.debug("loadAsciiDoc - metaInfJSON: ${metaInfJSON}")
        return html
    }

}