package org.votingsystem.web.timestamp.servlet;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TimeStampVS;
import org.votingsystem.services.TimeStampService;
import org.votingsystem.signature.util.TimeStampResponseGenerator;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.timestamp.ejb.DAOBean;
import org.votingsystem.web.timestamp.ejb.AppData;
import org.votingsystem.web.timestamp.filter.FilterVS;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Level;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/timestamp")
public class TimeStampServlet extends HttpServlet {

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(TimeStampServlet.class.getSimpleName());

    @Inject AppData data;
    @Inject TimeStampService timeStampService;
    @Inject DAOBean dao;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = null;
        if(((FilterVS.RequestVSWrapper)req).getContentTypeVS() == ContentTypeVS.TIMESTAMP_QUERY) {
            try {
                TimeStampResponseGenerator responseGenerator = timeStampService.getResponseGenerator(
                        req.getInputStream());
                byte[] tokenBytes = responseGenerator.getTimeStampToken().getEncoded();
                final InputStream inputStream = new ByteArrayInputStream(tokenBytes);
                TimeStampVS timeStampVS = dao.create(new TimeStampVS(responseGenerator.getSerialNumber().longValue(),
                        tokenBytes, TimeStampVS.State.OK));
                resp.setContentType(ContentTypeVS.TIMESTAMP_RESPONSE.getName());
                final ServletOutputStream out = resp.getOutputStream();
                out.write(tokenBytes);
                out.flush();
                return;
            } catch(Exception ex) {
                log.log(Level.INFO, ex.getMessage(), ex);
                resp.setContentType("text/plain");
                resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
                if(writer == null) writer = resp.getWriter();
                writer.println(ex.getMessage());
            }
        } else {
            resp.setContentType("text/plain");
            resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
            if(writer == null) writer = resp.getWriter();
            writer.println(data.get("requestWithoutFile", req.getLocale()));
        }
        if(writer != null) writer.close();
    }

}