package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.DocumentHeader;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/asciiDoc")
public class AsciiDocResource {

    private static final Logger log = Logger.getLogger(AsciiDocResource.class.getSimpleName());

    private String testFile = "./asciiDocksCurrencyTest.temp";
    @Inject SignatureBean signatureBean;


    @GET@Path("/")
    public Response index(@Context ServletContext context, @Context HttpServletRequest req,
              @Context HttpServletResponse resp) throws ServletException, IOException {
        File asciiFile = new File(testFile);
        FileReader reader = new FileReader(asciiFile);
        StringWriter writer = new StringWriter();
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.convert(reader, writer, new HashMap<String, Object>());
        StringBuffer htmlBuffer = writer.getBuffer();
        return Response.ok().entity(htmlBuffer.toString()).build();
    }

    @Path("/test")
    @POST @Consumes(MediaTypeVS.JSON_SIGNED)
    public Response testSMIME(MessageSMIME messageSMIME, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String asciiDocStr = messageSMIME.getSMIME().getSignedContent();
        return Response.ok().entity(loadAsciiDoc(asciiDocStr)).build();
    }

    @Path("/test")
    @POST @Consumes(MediaType.APPLICATION_JSON)
    public Response test(Map<String, Object> dataMap, @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        return Response.ok().entity(loadAsciiDoc((String) dataMap.get("asciiDoc"))).build();
    }

    private String loadAsciiDoc(String asciiDocStr) throws IOException {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        String html = asciidoctor.convert(asciiDocStr, new HashMap<String, Object>());
        //StructuredDocument document = asciidoctor.readDocumentStructure(asciiDocStr, new HashMap<String, Object>());
        DocumentHeader header = asciidoctor.readDocumentHeader(asciiDocStr);
        String metaInf = (String) header.getAttributes().get("metaInfVS");
        JsonNode actualObj = new ObjectMapper().readTree("{\"k1\":\"v1\"}");
        log.info("loadAsciiDoc - metaInfJSON: " + metaInf);
        return html;
    }
}
