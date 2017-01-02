package org.votingsystem.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.util.JSON;
import org.votingsystem.xml.XML;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class HttpResponse {

    public static Response sendResponseDto(final int statusCode, final HttpServletRequest req,
            final HttpServletResponse res, final Object responseDto) throws IOException, ServletException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json")) {
            res.setContentType(javax.ws.rs.core.MediaType.APPLICATION_JSON);
            res.getOutputStream().write(JSON.getMapper().writeValueAsBytes(responseDto));
        } else {
            res.setContentType(javax.ws.rs.core.MediaType.APPLICATION_XML);
            res.getOutputStream().write(XML.getMapper().writeValueAsBytes(responseDto));
        }
        res.setStatus(statusCode);
        return Response.status(statusCode).build();
    }

    /*
                    if(reqContentType.contains("json")){
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
     */
    public static byte[] getResponse(final HttpServletRequest req, final Object responseDto) throws IOException, 
            ServletException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json")) {
            return JSON.getMapper().writeValueAsBytes(responseDto);
        } else {
            return XML.getMapper().writeValueAsBytes(responseDto);
        }
    }

    public static Response getResponse(final HttpServletRequest req, final Integer statusCode,
                                     final Object responseDto) throws JsonProcessingException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json")) {
            return Response.status(statusCode).type(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                    .entity(JSON.getMapper().writeValueAsBytes(responseDto)).build();
        } else {
            return Response.status(statusCode).type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE)
                    .entity(XML.getMapper().writeValueAsBytes(responseDto)).build();
        }
    }

    public static Response getResponseFromBytes(final HttpServletRequest req, final Integer statusCode,
                                       final byte[] response) throws JsonProcessingException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json")) {
            return Response.status(statusCode).type(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).entity(response).build();
        } else {
            return Response.status(statusCode).type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE).entity(response).build();
        }
    }
}
