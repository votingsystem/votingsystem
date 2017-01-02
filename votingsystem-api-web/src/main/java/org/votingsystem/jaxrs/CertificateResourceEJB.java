package org.votingsystem.jaxrs;

import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.ejb.Config;
import org.votingsystem.model.Certificate;
import org.votingsystem.util.FileUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/certs")
@Stateless
public class CertificateResourceEJB {

    private static final Logger log = Logger.getLogger(CertificateResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject Config config;

    @POST @Path("/getBySerialNumber")
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestAnonymousCertificate(@Context HttpServletRequest req) throws Exception {
        Long serialNumber = Long.valueOf(FileUtils.getStringFromStream(req.getInputStream()));
        List<Certificate> certificateList = em.createQuery("select c from Certificate c where c.serialNumber=:serialNumber")
                .setParameter("serialNumber", serialNumber).getResultList();
        if(certificateList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Certificate with serialNumber " +
                    serialNumber + " not found").build() ;
        }
        X509Certificate x509Cert = certificateList.iterator().next().getX509Certificate();
        return Response.ok().entity(PEMUtils.getPEMEncoded(x509Cert)).build();
    }

}
