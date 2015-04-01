package org.votingsystem.web.accesscontrol.jaxrs;

import org.apache.commons.io.IOUtils;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.model.RepresentativeDocument;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Logger;

@Path("/development")
public class DevelopmentResource {

    private static final Logger log = Logger.getLogger(DevelopmentResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject DAOBean dao;

    public Response adduser(@Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        if(config.getMode() != EnvironmentVS.DEVELOPMENT) {
            throw new ValidationExceptionVS("SERVICE AVAILABLE ONLY IN DEVELOPMENT MODE");
        }
        byte[] requestBytes = IOUtils.toByteArray(req.getInputStream());
        Collection<X509Certificate> userCertCollection = CertUtils.fromPEMToX509CertCollection(requestBytes);
        X509Certificate userCert = userCertCollection.iterator().next();
        if(userCert != null) {
            UserVS userVS = UserVS.getUserVS(userCert);
            userVS = subscriptionVSBean.checkUser(userVS);
            userVS.setType(UserVS.Type.USER);
            userVS.setRepresentative(null);
            userVS.setMetaInf(null);
            Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS")
                    .setParameter("userVS", userVS);
            RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
            if(representativeDocument != null) {
                dao.merge(representativeDocument.setState(RepresentativeDocument.State.CANCELED));
            }
            dao.merge(userVS);
            return Response.ok().entity("UservS OK").build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - missing user certs").build();
    }

}
