package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.CertificateVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/certificateVS")
public class CertificateVSResource {

    private static final Logger log = Logger.getLogger(CertificateVSResource.class.getName());


    @Inject ConfigVS app;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject CertificateVSBean certificateVSBean;

    @Transactional
    @Path("/certs")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response certs(@DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @DefaultValue("USER") @QueryParam("type") String typeStr,
                          @DefaultValue("OK") @QueryParam("state") String stateStr,
                          @DefaultValue("") @QueryParam("format") String format, @QueryParam("searchText") String searchText,
                          @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType(): "";
        CertificateVS.Type type = CertificateVS.Type.valueOf(typeStr);
        CertificateVS.State state = CertificateVS.State.valueOf(stateStr);
        Query query;
        Long totalCount = null;
        if(searchText == null) {
            query = dao.getEM().createQuery("select count(c) from CertificateVS c where c.type =:type and c.state =:state")
                    .setParameter("type", type).setParameter("state", state);
            totalCount = (long) query.getSingleResult();
            query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type and c.state =:state")
                    .setParameter("type", type).setParameter("state", state)
                    .setFirstResult(offset).setMaxResults(max);;
        } else {
            query = dao.getEM().createQuery("select count (c) from CertificateVS c where c.type =:type and c.state =:state " +
                    "and (c.userVS.name like :searchText or c.userVS.nif like :searchText " +
                    "or c.userVS.firstName like :searchText or c.userVS.lastName like :searchText " +
                    "or c.userVS.description like :searchText)").setParameter("type", type)
                    .setParameter("state", state).setParameter("searchText", "%" + searchText + "%");
            totalCount = (long) query.getSingleResult();
            query = dao.getEM().createQuery("select c from CertificateVS c where c.type =:type and c.state =:state " +
                    "and (c.userVS.name like :searchText or c.userVS.nif like :searchText " +
                    "or c.userVS.firstName like :searchText or c.userVS.lastName like :searchText " +
                    "or c.userVS.description like :searchText)").setParameter("type", type)
                    .setParameter("state", state).setParameter("searchText", "%" + searchText + "%")
                    .setFirstResult(offset).setMaxResults(max);;
        }
        List<CertificateVS> certificates = query.getResultList();
        if(contentType.contains("pem")) {
            List<X509Certificate> resultList = new ArrayList<>();
            for(CertificateVS certificateVS : certificates) {
                resultList.add(certificateVS.getX509Cert());
            }
            return Response.ok().entity(PEMUtils.getPEMEncoded(resultList)).build();
        } else {
            List<CertificateVSDto> listDto = new ArrayList<>();
            for(CertificateVS certificateVS : certificates) {
                listDto.add(new CertificateVSDto(certificateVS));
            }
            ResultListDto<CertificateVSDto> resultListDto = new ResultListDto<>(listDto, offset, max, totalCount);
            if(contentType.contains("json")) {
                return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
            } else {
                req.getSession().setAttribute("certListDto", JSON.getMapper().writeValueAsString(resultListDto));
                return Response.temporaryRedirect(new URI("../certificateVS/certs.xhtml")).build();
            }
        }
    }

    @GET @Path("/serialNumber/{serialNumber}")
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
            return Response.ok().entity(PEMUtils.getPEMEncoded (x509Cert)).build();
        } else {
            CertificateVSDto certDto = new CertificateVSDto(certificate);
            if(contentType.contains("json")) {
                return Response.ok().entity(JSON.getMapper().writeValueAsBytes(certDto))
                        .type(MediaTypeVS.JSON).build();
            } else {
                req.getSession().setAttribute("certMap", certDto);
                req.getSession().setAttribute("certDto", JSON.getMapper().writeValueAsString(certDto));
                return Response.temporaryRedirect(new URI("../certificateVS/cert.xhtml?menu="+req.getParameter("menu"))).build();
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
            return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - UserVS without active CertificateVS").build();
    }

    @Path("/eventVS/id/{eventId}/CACert")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response eventCA(@PathParam("eventId") Long eventId) throws Exception {
        EventVSElection eventVSElection = dao.find(EventVSElection.class, eventId);
        if(eventVSElection == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - EventVSElection not found - eventId: " + eventId).build();
        CertificateVS certificateCA = eventVSElection.getCertificateVS();
        X509Certificate certX509 = CertUtils.loadCertificate(certificateCA.getContent());
        return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
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
            return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.NOT_FOUND).entity("hashHex: " + hashHex).build();
    }

    @Path("/trustedCerts")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response trustedCerts() throws Exception {
        Set<X509Certificate> trustedCerts = cmsBean.getTrustedCerts();
        return Response.ok().entity(PEMUtils.getPEMEncoded (trustedCerts)).build();
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
                return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
            } else return Response.status(Response.Status.NOT_FOUND).entity("userWithoutCert - userId: " + userId).build();
        }
    }

    @Path("/editCert")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response editCert(MessageCMS messageCMS, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        CertificateVS certificateVS = certificateVSBean.editCert(messageCMS);
        return Response.ok().entity("editCert - certificateVS id: " + certificateVS.getId()).build();
    }

}