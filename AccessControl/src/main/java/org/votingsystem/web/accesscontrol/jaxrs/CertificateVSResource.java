package org.votingsystem.web.accesscontrol.jaxrs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventElection;
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
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(CertificateVS.class)
                .createAlias("user", "user");
        criteria.add(Restrictions.eq("state", state));
        criteria.add(Restrictions.eq("type", type));
        if(searchText != null) {
            Criterion rest1= Restrictions.ilike("user.name", "%" + searchText + "%");
            Criterion rest2= Restrictions.ilike("user.firstName", "%" + searchText + "%");
            Criterion rest3= Restrictions.ilike("user.lastName", "%" + searchText + "%");
            Criterion rest4= Restrictions.ilike("user.description", "%" + searchText + "%");
            Criterion rest5= Restrictions.ilike("user.nif", "%" + searchText + "%");
            criteria.add(Restrictions.or(rest1, rest2, rest3, rest4, rest5));
        }
        List<CertificateVS> certificates = criteria.setFirstResult(offset).setMaxResults(max).list();
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
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
            return Response.ok().entity(PEMUtils.getPEMEncoded (x509Cert)).build();
        } else {
            CertificateVSDto certDto = new CertificateVSDto(certificate);
            if(contentType.contains("json")) {
                return Response.ok().entity(JSON.getMapper().writeValueAsBytes(certDto)).type(MediaTypeVS.JSON).build();
            } else {
                req.getSession().setAttribute("certMap", certDto);
                req.getSession().setAttribute("certDto", JSON.getMapper().writeValueAsString(certDto));
                return Response.temporaryRedirect(new URI("../certificateVS/cert.xhtml?menu="+req.getParameter("menu"))).build();
            }
        }
    }

    @Path("/user/id/{userId}")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response user(@PathParam("userId") Long userId) throws Exception {
        User user = dao.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - User not found - userId: " + userId).build();
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.user =:user and c.state =:state")
                .setParameter("user", user).setParameter("state", CertificateVS.State.OK);
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        if (certificate != null) {
            X509Certificate certX509 = CertUtils.loadCertificate(certificate.getContent());
            return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - User without active CertificateVS").build();
    }

    @Path("/eventVS/id/{eventId}/CACert")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response eventCA(@PathParam("eventId") Long eventId) throws Exception {
        EventElection eventElection = dao.find(EventElection.class, eventId);
        if(eventElection == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - EventElection not found - eventId: " + eventId).build();
        X509Certificate certX509 = CertUtils.loadCertificate(eventElection.getCertificateVS().getContent());
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

    @Path("/user/{userId}")
    @GET  @Produces(MediaType.TEXT_PLAIN)
    public Response user(@PathParam("userId") long userId, @Context Request req, @Context HttpServletResponse resp)
            throws Exception {
        User user = dao.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("userId: " + userId).build();
        } else {
            Query query = dao.getEM().createQuery("select c from CertificateVS c where c.user =:user and c.state =:state")
                    .setParameter("user", user).setParameter("state", CertificateVS.State.OK);
            CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
            if (certificate != null) {
                X509Certificate certX509 = CertUtils.loadCertificate (certificate.getContent());
                return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
            } else return Response.status(Response.Status.NOT_FOUND).entity("userWithoutCert - userId: " + userId).build();
        }
    }

    @Path("/editCert")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response editCert(CMSMessage cmsMessage, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        CertificateVS certificateVS = certificateVSBean.editCert(cmsMessage);
        return Response.ok().entity("editCert - certificateVS id: " + certificateVS.getId()).build();
    }

}