package org.votingsystem.jaxrs;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.qr.QRRequestBundle;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Messages;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/qr")
@Stateless
public class QRResourceEJB {

    private static final Logger log = Logger.getLogger(QRResourceEJB.class.getName());

    @Inject private Config config;
    @EJB private QRSessionsEJB qrSessions;

    @GET
    @Path("/")
    public Response getCode(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        try {
            QRUtils.ChartServletRequestParameters qrRequest = QRUtils.parseRequest(req, "POST".equals(req.getMethod()));
            BitMatrix matrix = new QRCodeWriter().encode(qrRequest.getText(), BarcodeFormat.QR_CODE,
                    qrRequest.getWidth(), qrRequest.getHeight(), qrRequest.getHints());
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
            byte[] pngData = pngOut.toByteArray();
            res.setContentType("image/png");
            res.setContentLength(pngData.length);
            res.setHeader("Cache-Control", "public");
            res.getOutputStream().write(pngData);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            res.setStatus(ResponseDto.SC_ERROR_REQUEST);
            String message = ex.getMessage() != null ? ex.getMessage(): "EXCEPTION: " + ex.getClass();
            res.getOutputStream().write(message.getBytes());
        }
        return Response.ok().build();
    }

    @POST @Path("/info")
    @Produces(MediaType.TEXT_XML)
    public Response info(@Context HttpServletRequest req, String uuid) throws Exception {
        QRRequestBundle qrRequest = qrSessions.getOperation(uuid);
        if(qrRequest != null)
            return Response.ok().entity(qrRequest.generateResponse(req, LocalDateTime.now())).build();
        else {
            return Response.status(Response.Status.NOT_FOUND).entity(new XmlMapper().writeValueAsBytes(
                    new ResponseDto(ResponseDto.SC_NOT_FOUND, Messages.currentInstance().get("itemNotFoundErrorMsg")))).build();
        }
    }

}
