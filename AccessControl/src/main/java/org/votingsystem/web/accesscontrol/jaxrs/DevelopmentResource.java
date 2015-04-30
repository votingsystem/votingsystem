package org.votingsystem.web.accesscontrol.jaxrs;

import org.apache.commons.io.IOUtils;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.RepresentationDocument;
import org.votingsystem.model.voting.RepresentativeDocument;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Path("/development")
@SessionScoped
public class DevelopmentResource implements Serializable {

    private static final Logger log = Logger.getLogger(DevelopmentResource.class.getSimpleName());

    @EJB ConfigVS config;
    @EJB SubscriptionVSBean subscriptionVSBean;
    @EJB DAOBean dao;
    @EJB SignatureBean signatureBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Transactional
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

    @Transactional
    @Path("/resetRepresentatives") @POST
    public Response resetRepresentatives(MessageSMIME messageSMIME, @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        if(config.getMode() != EnvironmentVS.DEVELOPMENT) {
            throw new ValidationExceptionVS("SERVICE AVAILABLE ONLY IN DEVELOPMENT MODE");
        }
        if(!signatureBean.isAdmin(messageSMIME.getUserVS().getNif()))
            throw new ValidationExceptionVS("user without privileges");
        log.severe(" ===== VOTING SIMULATION - RESETING REPRESENTATIVES ===== ");
        List<RepresentativeDocument.State> inList = Arrays.asList(RepresentativeDocument.State.OK,
                RepresentativeDocument.State.RENEWED);
        Query query = dao.getEM().createQuery(
                "select r from RepresentativeDocument r where r.state in :inList").setParameter("inList", inList);
        List<RepresentativeDocument> representativeDocList = query.getResultList();
        StringBuilder stringBuilder = new StringBuilder("Num. representatives updated: " + representativeDocList.size());
        Integer numUserVSUpdated = 0;
        for(RepresentativeDocument representativeDocument : representativeDocList) {
            UserVS representative = representativeDocument.getUserVS();
            query = dao.getEM().createQuery("select u from UserVS u where u.representative =:representative")
                    .setParameter("representative", representative);
            List<UserVS> representedList = query.getResultList();
            numUserVSUpdated = numUserVSUpdated + representedList.size();
            for(UserVS represented : representedList) {
                query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:represented " +
                        "and r.state =:state").setParameter("represented", represented)
                        .setParameter("state", RepresentationDocument.State.OK);
                RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
                representationDocument.setState(RepresentationDocument.State.CANCELED_BY_REPRESENTATIVE)
                        .setDateCanceled(new Date());
                dao.merge(representationDocument);
                dao.merge(represented.setRepresentative(null));
            }
            dao.merge(representative.setType(UserVS.Type.USER));
            dao.merge(representativeDocument.setDateCanceled(new Date()).setState(RepresentativeDocument.State.CANCELED));
        }
        stringBuilder.append(" - Num. users updated: " + numUserVSUpdated);
        return Response.ok().entity(stringBuilder.toString()).build();
    }

    @Transactional
    @Path("/test") @GET
    public Response test(@Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        /*Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.state =:state")
                .setParameter("state", RepresentativeDocument.State.OK);
        List<RepresentativeDocument> resultList = query.getResultList();
        for(RepresentativeDocument item : resultList) {
            item.setState(RepresentativeDocument.State.CANCELED).setDateCanceled(new Date());
        }*/

        log.info(MessagesVS.getCurrentInstance().get("representativeDataLbl"));
        return Response.ok().entity(MessagesVS.getCurrentInstance().get("representativeDataLbl")).build();
    }

    @Transactional
    @Path("/test1") @GET
    public Response test1(@Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        return Response.ok().entity("test1").build();
    }
}