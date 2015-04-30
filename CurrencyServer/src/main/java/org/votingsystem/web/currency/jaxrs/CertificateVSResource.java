package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.ejb.CertificateVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/certificateVS")
public class CertificateVSResource {

    private static final Logger log = Logger.getLogger(CertificateVSResource.class.getSimpleName());

    @Inject ConfigVS app;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject CertificateVSBean certificateVSBean;

    @Path("/hashHex/{hashHex}")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response hashHex(@PathParam("hashHex") String hashHex, @Context Request req,
            @Context HttpServletResponse resp) throws Exception {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        String hashCertVSBase64 = new String(hexConverter.unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVS")
                .setParameter("hashCertVS", hashCertVSBase64);
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        if (certificate != null) {
            X509Certificate certX509 = CertUtils.loadCertificate(certificate.getContent());
            return Response.ok().entity(CertUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.NOT_FOUND).entity("hashHex: " + hashHex).build();
    }

    @Path("/userVS/{userId}")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response userVS(@PathParam("userId") long userId, @Context Request req, @Context HttpServletResponse resp)
            throws Exception {
        UserVS userVS = dao.find(UserVS.class, userId);
        if(userVS == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("userId: " + userId).build();
        } else {
            Query query = dao.getEM().createQuery("select c from CertificateVS c where c.userVS =:userVS and c.state =:state")
                    .setParameter("userVS", userVS).setParameter("state", CertificateVS.State.OK);
            CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
            if (certificate != null) {
                X509Certificate certX509 = CertUtils.loadCertificate (certificate.getContent());
                return Response.ok().entity(CertUtils.getPEMEncoded (certX509)).build();
            } else return Response.status(Response.Status.NOT_FOUND).entity("userWithoutCert - userId: " + userId).build();
        }
    }

    @Path("/addCertificateAuthority")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response addCertificateAuthority(MessageSMIME messageSMIME, @Context HttpServletRequest req,
            @Context HttpServletResponse resp) throws Exception {
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(certificateVSBean.addCertificateAuthority(messageSMIME)))
                .type(MediaTypeVS.JSON).build();
    }

    @Path("/editCert")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response editCert(MessageSMIME messageSMIME, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        CertificateVS certificateVS = certificateVSBean.editCert(messageSMIME);
        return Response.ok().entity("editCert - certificateVS id: " + certificateVS.getId()).build();
    }

    @Path("/certs")
    @GET @Consumes(MediaType.TEXT_PLAIN) @Produces(MediaType.APPLICATION_JSON)
    public Object certs(@DefaultValue("USER") @QueryParam("type") String typeStr,
            @DefaultValue("OK") @QueryParam("state") String stateStr,
            @DefaultValue("") @QueryParam("format") String format, @QueryParam("searchText") String searchText,
            @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType(): "";
        CertificateVS.Type type = CertificateVS.Type.valueOf(typeStr);
        CertificateVS.State state = CertificateVS.State.valueOf(stateStr);
        if(contentType.contains("pem") || contentType.contains("json") || "pem".equals(format)) {
            Query query;
            if(searchText != null) {
                query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type and c.state =:state")
                        .setParameter("type", type).setParameter("state", state);
            } else {
                query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type and c.state =:state " +
                        "and (c.userVS.name like :searchText or c.userVS.nif like :searchText " +
                        "or c.userVS.firstName like :searchText or c.userVS.lastName like :searchText " +
                        "or c.userVS.description like :searchText)").setParameter("type", type)
                        .setParameter("state", state).setParameter("searchText", "%" + searchText + "%");

            }
            List<CertificateVS> certificates = query.getResultList();
            if(certificates.isEmpty()) return Response.status(Response.Status.NOT_FOUND).build();
            List resultList = new ArrayList<>();
            if(req.getContentType() != null && req.getContentType().contains("json")) {
                for(CertificateVS certificateVS : certificates) {
                    resultList.add(new CertificateVSDto(certificateVS));
                }
                Map resultMap = new HashMap<>();
                resultMap.put("certList", resultList);
                resultMap.put("type", type.toString());
                resultMap.put("state", state.toString());
                resultMap.put("totalCount", resultList.size());
                return resultMap;
            } else {
                for(CertificateVS certificateVS : certificates) {
                    resultList.add(certificateVS.getX509Cert());
                }
                return Response.ok().entity(CertUtils.getPEMEncoded (resultList)).build();
            }
        } else {
            Map resultMap = new HashMap<>();
            resultMap.put("type", type.toString());
            resultMap.put("state", state.toString());
            req.setAttribute("certsMap", resultMap);
            context.getRequestDispatcher("/certificateVS/certs.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/cert/{serialNumber}")
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Object cert(@PathParam("serialNumber") long serialNumber, @Context HttpServletRequest req,
             @Context HttpServletResponse resp, @DefaultValue("") @QueryParam("format") String format,
             @Context ServletContext context) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType(): "";
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.serialNumber =:serialNumber")
                .setParameter("serialNumber", serialNumber);
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        if(certificate != null) {
            if(contentType.contains("pem") || "pem".equals(format)) {
                resp.setHeader("Content-Disposition", "inline; filename='trustedCert_" + serialNumber + "'");
                return Response.ok().entity(CertUtils.getPEMEncoded(certificate.getX509Cert()))
                        .type(ContentTypeVS.PEM.getName()).build();
            } else {
                if(req.getContentType().contains("json")) {
                   return Response.ok().entity(JSON.getMapper().writeValueAsBytes(new CertificateVSDto(certificate)));
                } else {
                    req.setAttribute("certMap", JSON.getMapper().writeValueAsString(new CertificateVSDto(certificate)));
                    context.getRequestDispatcher("/certificateVS/cert.xhtml").forward(req, resp);
                    return Response.ok().build();
                }
            }
        } else return Response.status(Response.Status.NOT_FOUND).entity("serialNumber: " + serialNumber).build();
    }

}