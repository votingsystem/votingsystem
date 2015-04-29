package org.votingsystem.web.accesscontrol.jaxrs;

import org.apache.commons.io.IOUtils;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.RepresentationDocument;
import org.votingsystem.model.voting.RepresentativeDocument;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Path("/development")
public class DevelopmentResource {

    private static final Logger log = Logger.getLogger(DevelopmentResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;

    @Path("/adduser") @POST
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
                dao.merge(representativeDocument.setDateCanceled(new Date()).setState(RepresentativeDocument.State.CANCELED));
            }
            dao.merge(userVS);
            return Response.ok().entity("UservS OK").build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - missing user certs").build();
    }

    @Path("/resetRepresentatives") @POST
    public Response resetRepresentatives(MessageSMIME messageSMIME, @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        if(config.getMode() != EnvironmentVS.DEVELOPMENT) {
            throw new ValidationExceptionVS("SERVICE AVAILABLE ONLY IN DEVELOPMENT MODE");
        }
        if(signatureBean.isAdmin(messageSMIME.getUserVS().getNif()))
            throw new ValidationExceptionVS("user without privileges");
        log.severe(" ===== VOTING SIMULATION - RESETING REPRESENTATIVES ===== ");
        List<RepresentativeDocument> representativeDocList = dao.findAll(RepresentativeDocument.class);
        for(RepresentativeDocument representativeDocument : representativeDocList) {
            UserVS representative = representativeDocument.getUserVS();
            dao.merge(representative.setType(UserVS.Type.USER));
            dao.merge(representativeDocument.setDateCanceled(new Date()).setState(RepresentativeDocument.State.CANCELED));
            Query query = dao.getEM().createQuery("select u from UserVS u where u.representative =:representative")
                    .setParameter("representative", representative);
            List<UserVS> representedList = query.getResultList();
            for(UserVS represented : representedList) {
                query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:represented " +
                        "and r.representative =:representative and r.state =:state").setParameter("represented", represented)
                        .setParameter("representative", representative).setParameter("state", RepresentationDocument.State.OK);
                RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
                representationDocument.setState(RepresentationDocument.State.CANCELED_BY_REPRESENTATIVE)
                        .setDateCanceled(represented.getTimeStampToken().getTimeStampInfo().getGenTime());
                dao.merge(representationDocument);
                dao.merge(represented.setRepresentative(null));
            }
        }
        return Response.ok().build();
    }

}