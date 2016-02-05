package org.votingsystem.web.timestamp.servlet;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TimeStampVS;
import org.votingsystem.service.TimeStampService;
import org.votingsystem.signature.util.TimeStampResponseGenerator;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.timestamp.filter.FilterVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/timestamp/discrete")
public class TimeStampDiscreteServlet extends HttpServlet {

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(TimeStampDiscreteServlet.class.getName());

    @Inject TimeStampService timeStampService;
    @Inject DAOBean dao;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        PrintWriter writer = null;
        ContentTypeVS contentTypeVS = ContentTypeVS.getByName(req.getContentType());
        if(contentTypeVS == ContentTypeVS.TIMESTAMP_QUERY) {
            try {
                TimeStampResponseGenerator responseGenerator = timeStampService.getResponseGeneratorDiscrete(
                        req.getInputStream());
                byte[] tokenBytes = responseGenerator.getTimeStampToken().getEncoded();
                dao.persist(new TimeStampVS(responseGenerator.getSerialNumber().longValue(), tokenBytes,
                        TimeStampVS.State.OK));
                resp.setContentType(ContentTypeVS.TIMESTAMP_RESPONSE.getName());
                final ServletOutputStream out = resp.getOutputStream();
                out.write(tokenBytes);
                out.flush();
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
            writer.println(messages.get("requestWithoutFile"));
        }
        if(writer != null) writer.close();
    }

}