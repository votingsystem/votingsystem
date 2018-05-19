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

    public HttpResponse() {}

    public Response sendResponseDto(final int statusCode, final HttpServletRequest req,
            final HttpServletResponse res, final Object responseDto) throws IOException, ServletException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json")) {
            res.setContentType(javax.ws.rs.core.MediaType.APPLICATION_JSON);
            res.getOutputStream().write(new JSON().getMapper().writeValueAsBytes(responseDto));
        } else {
            res.setContentType(javax.ws.rs.core.MediaType.APPLICATION_XML);
            res.getOutputStream().write(new XML().getMapper().writeValueAsBytes(responseDto));
        }
        res.setStatus(statusCode);
        return Response.status(statusCode).build();
    }

    public Response getResponse(final HttpServletRequest req, final Integer statusCode,
                                     final Object responseDto) throws JsonProcessingException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json")) {
            return Response.status(statusCode).type(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                    .entity(new JSON().getMapper().writeValueAsBytes(responseDto)).build();
        } else {
            return Response.status(statusCode).type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE)
                    .entity(new XML().getMapper().writeValueAsBytes(responseDto)).build();
        }
    }

    public byte[] getResponseContent(final HttpServletRequest req, final Object responseDto) throws JsonProcessingException {
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("json"))
            return new JSON().getMapper().writeValueAsBytes(responseDto);
        else
            return new XML().getMapper().writeValueAsBytes(responseDto);
    }

}