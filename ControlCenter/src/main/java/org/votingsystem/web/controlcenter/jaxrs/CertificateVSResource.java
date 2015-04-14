package org.votingsystem.web.controlcenter.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
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
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

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


    @Path("/certs") @GET
    public Response certs(@DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
            @PathParam("serialNumber") Long serialNumber, @DefaultValue("") @QueryParam("searchText") String searchText,
            @QueryParam("type") String typeReq,
            @QueryParam("state") String stateReq, @QueryParam("format") String formatReq, @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        CertificateVS.Type type = CertificateVS.Type.USER;
        CertificateVS.State state = CertificateVS.State.OK;
        try {type = CertificateVS.Type.valueOf(typeReq);} catch(Exception ex) {}
        try {state = CertificateVS.State.valueOf(stateReq);} catch(Exception ex) { }
        if(contentType.contains("pem") || contentType.contains("json") || "pem".equals(formatReq)) {
            /*Map<String, Object> params = new HashMap<String, Object>();
            StringBuffer hql = new StringBuffer("select c from CertificateVS c where c.type =:type and c.state =:state");
            if (searchText != null) {
                hql.append(" and (c.userVS.name like :searchText or c.userVS.nif like :searchText or c.userVS.firstName like :searchText " +
                        "or c.userVS.lastName like :searchText or c.userVS.description like :searchText)");
                params.put("searchText", "%" + searchText + "%");
            }
            query = dao.getEM().createQuery(hql.toString());
            for(Map.Entry<String, Object> paramEntry :params.entrySet()) {
                query.setParameter(paramEntry.getKey(), paramEntry.getValue());
            }*/
            searchText = "%" + searchText + "%";
            Query query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type and c.state =:state and " +
                    "(c.userVS.name like :searchText or c.userVS.nif like :searchText or c.userVS.firstName like :searchText " +
                    "or c.userVS.lastName like :searchText or c.userVS.description like :searchText)")
                    .setParameter("type", type).setParameter("state", state).setParameter("searchText", searchText);
            List<CertificateVS> certList = query.getResultList();
            if(contentType.contains("pem") || "pem".equals(formatReq)) {
                List<X509Certificate> resultList = new ArrayList<>();
                for(CertificateVS certificateVS : certList) {
                    resultList.add(certificateVS.getX509Cert());
                }
                return Response.ok().entity(CertUtils.getPEMEncoded (resultList)).build();
            } else {
                List<CertificateVSDto> resultList = new ArrayList<>();
                for(CertificateVS certificateVS : certList) {
                    resultList.add(new CertificateVSDto(certificateVS));
                }
                Map resultMap = new HashMap<>();
                resultMap.put("certList", resultList);
                resultMap.put("type", type);
                resultMap.put("state", state);
                resultMap.put("offset", offset);
                resultMap.put("max", max);
                resultMap.put("totalCount", resultList.size());
                return Response.ok().entity(new ObjectMapper().writeValueAsBytes(resultMap))
                        .type(MediaTypeVS.JSON).build();
            }
        }
        context.getRequestDispatcher("/certificateVS/certs.xhtml").forward(req, resp);
        return Response.ok().build();
    }


    @Path("/serialNumber/{serialNumber}")
    @GET
    public Response cert(@PathParam("serialNumber") Long serialNumber, @QueryParam("format") String format,
             @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.serialNumber =:serialNumber")
                .setParameter("serialNumber", serialNumber);
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        if(certificate == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - CertificateVS not found - serialNumber: " + serialNumber).build();
        X509Certificate x509Cert = certificate.getX509Cert();
        if(contentType.contains("pem") || (format != null && "pem".equals(format.toLowerCase()))){
            resp.setHeader("Content-Disposition", format("inline; filename='trustedCert_{0}'", serialNumber));
            return Response.ok().entity(CertUtils.getPEMEncoded (x509Cert)).build();
        } else {
            CertificateVSDto certJSON = new CertificateVSDto(certificate);
            if(req.getContentType().contains("json")) {
                return Response.ok().entity(new ObjectMapper().writeValueAsBytes(certJSON))
                        .type(MediaTypeVS.JSON).build();
            } else {
                req.setAttribute("certMap", JSON.getMapper().writeValueAsString(certJSON));
                context.getRequestDispatcher("/certificateVS/cert.xhtml").forward(req, resp);
                return Response.ok().build();
            }
        }
    }


    @Path("/userVS/id/{userId}")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response userVS(@PathParam("userId") Long userId) throws Exception {
        UserVS userVS = dao.find(UserVS.class, userId);
        if(userVS == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - UserVS not found - userId: " + userId).build();
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.userVS =:userVS and c.state =:state")
                .setParameter("userVS", userVS).setParameter("state", CertificateVS.State.OK);
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        if (certificate != null) {
            X509Certificate certX509 = CertUtils.loadCertificate(certificate.getContent());
            return Response.ok().entity(CertUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - UserVS without active CertificateVS").build();
    }

    @Path("/eventVS/id/{eventId}/CACert")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response eventCA(@PathParam("eventId") Long eventId) throws Exception {
        EventVSElection eventVSElection = dao.find(EventVSElection.class, eventId);
        if(eventVSElection == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - EventVSElection not found - eventId: " + eventId).build();
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.eventVS =:eventVS and " +
                "c.type =:type").setParameter("eventVS", eventVSElection).setParameter("type", CertificateVS.Type.VOTEVS_ROOT);
        CertificateVS certificateCA = dao.getSingleResult(CertificateVS.class, query);
        X509Certificate certX509 = CertUtils.loadCertificate(certificateCA.getContent());
        return Response.ok().entity(CertUtils.getPEMEncoded (certX509)).build();
    }

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

    @Path("/trustedCerts")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response trustedCerts() throws Exception {
        Set<X509Certificate> trustedCerts = signatureBean.getTrustedCerts();
        return Response.ok().entity(CertUtils.getPEMEncoded (trustedCerts)).build();
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

    @Path("/editCert")
    @POST @Consumes(MediaTypeVS.JSON_SIGNED) @Produces(MediaType.APPLICATION_JSON)
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
                CertificateVSDto certJSON = new CertificateVSDto(certificate);
                if(req.getContentType().contains("json")) {
                   return certJSON;
                } else {
                    req.setAttribute("certMap", JSON.getMapper().writeValueAsString(certJSON));
                    context.getRequestDispatcher("/certificateVS/cert.xhtml").forward(req, resp);
                    return Response.ok().build();
                }
            }
        } else return Response.status(Response.Status.NOT_FOUND).entity("serialNumber: " + serialNumber).build();
    }

}