package org.votingsystem.jaxrs.exception;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpRequest;
import org.votingsystem.util.AppCode;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExceptionMapper that catch all application exceptions to send proper responses
 *
 * @author votingsystem
 */
@Provider
public class LocalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = Logger.getLogger(LocalExceptionMapper.class.getName());

    @Context private HttpServletRequest req;
    @Context private HttpServletResponse res;

    @Override
    public Response toResponse(Exception exception) {
        log.log(Level.SEVERE, exception.getClass().getName() + " - " + exception.getMessage(), exception);
        String reqContentType = HttpRequest.getContentType(req, true);
        if(req.getRequestURI().contains("/api/test")) {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    "TEST ERROR - " + sw.toString()).type(MediaType.TEXT_PLAIN).build();
        } else {
            AppCode appCode = AppCode.getByException(exception);
            String message = null;
            switch (appCode) {
                case vs_0430:
                    message = exception.getMessage();
                    break;
                case vs_0410:
                    message = Messages.currentInstance().get("errorLbl") + " - " + exception.getMessage();
                    break;
                case vs_0420:
                    message = Messages.currentInstance().get("errorLbl") + " - " + exception.getMessage();
                    break;
                case vs_0500:
                    message = Messages.currentInstance().get("systemErrorMsg");
                    break;
                case vs_0405:
                case vs_0400:
                    message = exception.getMessage();
                    break;
                default:
                    message = Messages.currentInstance().get("errorMsg", appCode);
            }
            try {
                if(reqContentType.contains("json") || reqContentType.contains("application/pkcs7-signature")){
                    return Response.status(appCode.getStatusCode()).entity(JSON.getMapper().writeValueAsBytes(
                                    new ResponseDto(appCode, message))).build();
                } else if(reqContentType.contains("html")) {
                    res.setStatus(appCode.getStatusCode());
                    req.getSession().setAttribute("responseDto", new ResponseDto(ResponseDto.SC_ERROR,
                            message).setCode(appCode).setCaption(Messages.currentInstance().get("errorMsg", appCode)));
                    res.sendRedirect(req.getContextPath() + "/response.xhtml");
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).entity(
                            new XmlMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(
                                    new ResponseDto(appCode, message))).build();
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), exception);
            }
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}