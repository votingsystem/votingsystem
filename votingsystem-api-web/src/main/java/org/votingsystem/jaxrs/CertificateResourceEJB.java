package org.votingsystem.jaxrs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.CertificateDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.HttpRequest;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/certificate")
public class CertificateResourceEJB {

    private static final Logger log = Logger.getLogger(CertificateResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private SignatureService signatureService;
    @Inject private Config config;

    @Path("/revocationHash")
    @POST  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response revocationHash(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        String revocationHashBase64 = FileUtils.getStringFromStream(req.getInputStream());
        List<Certificate> certificates = em.createQuery(
                "select c from Certificate c where c.revocationHashBase64=:revocationHashBase64")
                .setParameter("revocationHashBase64", revocationHashBase64).getResultList();
        if (certificates.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Revocation hash: " + revocationHashBase64 +
                    " not found").build();
        }
        X509Certificate certX509 = CertUtils.loadCertificate(certificates.iterator().next().getContent());
        return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
    }

    @Path("/uuid")
    @POST @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response uuid(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        String uuid = FileUtils.getStringFromStream(req.getInputStream());
        List<Certificate> certificates = em.createQuery(
                "select c from Certificate c where c.UUID=:UUID").setParameter("UUID", uuid).getResultList();
        if (certificates.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cert with uuid: " + uuid +
                    " not found").build();
        }
        X509Certificate certX509 = CertUtils.loadCertificate(certificates.iterator().next().getContent());
        return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
    }

    @Path("/user/id")
    @POST  @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response user(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        Long userId = Long.valueOf(FileUtils.getStringFromStream(req.getInputStream()));
        User signer = em.find(User.class, userId);
        if(signer == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("userId: " + userId).build();
        } else {
            List<Certificate> certificates = em.createQuery("select c from Certificate c where c.signer=:signer and c.state =:state")
                    .setParameter("signer", signer).setParameter("state", Certificate.State.OK).getResultList();
            if(certificates.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity("userWithoutCert - userId: " + userId).build();
            }
            X509Certificate certX509 = CertUtils.loadCertificate (certificates.iterator().next().getContent());
            return Response.ok().entity(PEMUtils.getPEMEncoded (certX509)).build();
        }
    }

    @Transactional
    @Path("/certs")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response certs(@DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @DefaultValue("USER") @QueryParam("type") String typeStr,
                          @DefaultValue("OK") @QueryParam("state") String stateStr,
                          @DefaultValue("") @QueryParam("format") String format, @QueryParam("searchText") String searchText,
                          @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = HttpRequest.getContentType(req, true);
        Certificate.Type type = Certificate.Type.valueOf(typeStr);
        Certificate.State state = Certificate.State.valueOf(stateStr);
        Criteria criteria = em.unwrap(Session.class).createCriteria(Certificate.class);
        criteria.add(Restrictions.eq("state", state));
        criteria.add(Restrictions.eq("type", type));
        if(searchText != null) {
            criteria.createAlias("user", "user");
            Criterion rest1= Restrictions.ilike("user.name", "%" + searchText + "%");
            Criterion rest2= Restrictions.ilike("user.firstName", "%" + searchText + "%");
            Criterion rest3= Restrictions.ilike("user.lastName", "%" + searchText + "%");
            Criterion rest4= Restrictions.ilike("user.description", "%" + searchText + "%");
            Criterion rest5= Restrictions.ilike("user.nif", "%" + searchText + "%");
            criteria.add(Restrictions.or(rest1, rest2, rest3, rest4, rest5));
        }
        List<Certificate> certificates = criteria.setFirstResult(offset).setMaxResults(max).list();
        criteria.setFirstResult(0); //reset offset for total count
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        if(contentType.contains("pem")) {
            List<X509Certificate> resultList = new ArrayList<>();
            for(Certificate certificate : certificates) {
                resultList.add(certificate.getX509Certificate());
            }
            return Response.ok().entity(PEMUtils.getPEMEncoded(resultList)).build();
        } else {
            List<CertificateDto> listDto = new ArrayList<>();
            for(Certificate certificate : certificates) {
                listDto.add(new CertificateDto(certificate));
            }
            ResultListDto<CertificateDto> resultListDto = new ResultListDto<>(listDto, offset, max, totalCount);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
        }
    }

    @Path("/serialNumber")
    @POST  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response serialNumber(@Context HttpServletRequest req, @Context HttpServletResponse resp,
                       @DefaultValue("") @QueryParam("format") String format, @Context ServletContext context) throws Exception {
        String contentType = HttpRequest.getContentType(req, true);
        Long serialNumber = Long.valueOf(FileUtils.getStringFromStream(req.getInputStream()));
        List<Certificate> certificates = em.createQuery("select c from Certificate c where c.serialNumber =:serialNumber")
                .setParameter("serialNumber", serialNumber).getResultList();
        if(certificates.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Certificate with serialNumber " +
                    serialNumber + " not found").build();
        }
        Certificate certificate = certificates.iterator().next();
        if(contentType.contains("pem") || "pem".equals(format)) {
            resp.setHeader("Content-Disposition", "inline; filename='trustedCert_" + serialNumber + "'");
            return Response.ok().entity(PEMUtils.getPEMEncoded(certificate.getX509Certificate())).type(MediaType.PEM).build();
        } else
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(new CertificateDto(certificate))).build();
    }

    @GET @Path("/trusted")
    public Response trusted(@Context HttpServletRequest req) throws Exception {
        return Response.ok().entity(PEMUtils.getPEMEncoded(config.getTrustedCertAnchors())).build();
    }

}