package org.votingsystem.web.timestamp.servlet;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.tsp.TimeStampResponse;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TimeStamp;
import org.votingsystem.service.TimeStampService;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.crypto.TimeStampResponseGeneratorHelper;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.logging.Level;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/timestampResponse")
public class TimeStampResponseServlet extends HttpServlet {

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(TimeStampResponseServlet.class.getName());

    @Inject TimeStampService timeStampService;
    @Inject DAOBean dao;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = null;
        String contentEncoding = req.getHeader("Content-Encoding");
        try {
            TimeStampResponseGeneratorHelper responseGenerator = null;
            if(contentEncoding != null && "base64".equals(contentEncoding)) {
                byte[] requestBytesBase64 =  IOUtils.toByteArray(req.getInputStream());
                byte[] requestBytes = Base64.getDecoder().decode(requestBytesBase64);
                responseGenerator = timeStampService.getXAdESResponseGenerator(
                        new ByteArrayInputStream(requestBytes));
            } else {
                responseGenerator = timeStampService.getXAdESResponseGenerator(
                        req.getInputStream());
            }
            TimeStampResponse timeStampResponse = responseGenerator.getTimeStampResponse();
            byte[] tokenBytes = timeStampResponse.getTimeStampToken().getEncoded();
            dao.persist(new TimeStamp(responseGenerator.getSerialNumber().longValue(),
                    tokenBytes, TimeStamp.State.OK));

            byte[] responseBytes = timeStampResponse.getEncoded();

            resp.setContentType(ContentType.TIMESTAMP_RESPONSE.getName());
            final ServletOutputStream out = resp.getOutputStream();
            if(contentEncoding != null && "base64".equals(contentEncoding)) {
                out.write(Base64.getEncoder().encode(responseBytes));
            } else out.write(responseBytes);
            out.flush();
            return;
        } catch(Exception ex) {
            log.log(Level.INFO, ex.getMessage(), ex);
            resp.setContentType("text/plain");
            resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
            if(writer == null) writer = resp.getWriter();
            writer.println(ex.getMessage());
        }
        if(writer != null) writer.close();
    }

}