package org.votingsystem.web.accesscontrol.jaxrs;

import org.apache.commons.io.IOUtils;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.RepresentationDocument;
import org.votingsystem.model.voting.RepresentativeDocument;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.util.ConfigVS;
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

    private static final Logger log = Logger.getLogger(DevelopmentResource.class.getName());

    @EJB ConfigVS config;
    @EJB SubscriptionBean subscriptionBean;
    @EJB DAOBean dao;
    @EJB CMSBean cmsBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Transactional
    @Path("/adduser") @POST
    public Response adduser(@Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        byte[] requestBytes = IOUtils.toByteArray(req.getInputStream());
        Collection<X509Certificate> userCertCollection = PEMUtils.fromPEMToX509CertCollection(requestBytes);
        X509Certificate userCert = userCertCollection.iterator().next();
        if(userCert != null) {
            User user = User.FROM_X509_CERT(userCert);
            user = subscriptionBean.checkUser(user);
            user.setType(User.Type.USER);
            user.setRepresentative(null);
            user.setMetaInf(null);
            Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.user =:user")
                    .setParameter("user", user);
            RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
            if(representativeDocument != null) {
                dao.merge(representativeDocument.setDateCanceled(new Date()).setState(RepresentativeDocument.State.CANCELED));
            }
            dao.merge(user);
            return Response.ok().entity("User OK").build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - missing user certs").build();
    }

    @Transactional
    @Path("/resetRepresentatives") @POST
    public Response resetRepresentatives(CMSMessage cmsMessage, @Context ServletContext context,
                                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        if(!cmsBean.isAdmin(cmsMessage.getUser().getNif()))
            throw new ValidationException("user without privileges");
        log.severe(" ===== VOTING SIMULATION - RESETING REPRESENTATIVES ===== ");
        List<RepresentativeDocument.State> inList = Arrays.asList(RepresentativeDocument.State.OK,
                RepresentativeDocument.State.RENEWED);
        Query query = dao.getEM().createQuery(
                "select r from RepresentativeDocument r where r.state in :inList").setParameter("inList", inList);
        List<RepresentativeDocument> representativeDocList = query.getResultList();
        StringBuilder stringBuilder = new StringBuilder("Num. representatives updated: " + representativeDocList.size());
        Integer numUserUpdated = 0;
        for(RepresentativeDocument representativeDocument : representativeDocList) {
            User representative = representativeDocument.getUser();
            query = dao.getEM().createQuery("select u from User u where u.representative =:representative")
                    .setParameter("representative", representative);
            List<User> representedList = query.getResultList();
            numUserUpdated = numUserUpdated + representedList.size();
            for(User represented : representedList) {
                query = dao.getEM().createQuery("select r from RepresentationDocument r where r.user =:represented " +
                        "and r.state =:state").setParameter("represented", represented)
                        .setParameter("state", RepresentationDocument.State.OK);
                RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
                representationDocument.setState(RepresentationDocument.State.CANCELED_BY_REPRESENTATIVE)
                        .setDateCanceled(new Date());
                dao.merge(representationDocument);
                dao.merge(represented.setRepresentative(null));
            }
            dao.merge(representative.setType(User.Type.USER));
            dao.merge(representativeDocument.setDateCanceled(new Date()).setState(RepresentativeDocument.State.CANCELED));
        }
        query = dao.getEM().createQuery("select u from User u where u.type =:type")
                .setParameter("type", User.Type.REPRESENTATIVE);
        List<User> representativeList = query.getResultList();
        for(User representative : representativeList) {
            dao.merge(representative.setType(User.Type.USER));
        }
        stringBuilder.append(" - Num. users updated: " + numUserUpdated);
        return Response.ok().entity(stringBuilder.toString()).build();
    }

    @Transactional
    @Path("/test") @GET
    public Response test(@Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.state =:state")
                .setParameter("state", RepresentativeDocument.State.OK);
        List<RepresentativeDocument> resultList = query.getResultList();
        for(RepresentativeDocument item : resultList) {
            item.setState(RepresentativeDocument.State.CANCELED).setDateCanceled(new Date());
        }

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