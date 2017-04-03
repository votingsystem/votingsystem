package org.votingsystem.serviceprovider.jaxrs;

import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.serviceprovider.ejb.ConfigServiceProvider;
import org.votingsystem.util.JSON;
import org.votingsystem.xml.XML;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.model.voting.Election;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
@Stateless
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private SignatureService signatureService;
    @Inject @Push
    private PushContext serviceUpdated;
    @Inject
    ConfigServiceProvider configServiceProvider;
    @Inject
    TrustedServicesEJB trustedServices;
    @Inject private Config config;
    @EJB QRSessionsEJB qrSessionsEJB;

    @GET @Path("/")
    public Response test(@Context HttpServletRequest req) throws Exception {
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/load")
    public Response load(@Context HttpServletRequest req) throws Exception {
        trustedServices.loadTrustedServices();
        return Response.ok().entity("OK - load").build();
    }

    @GET @Path("/check")
    public Response check(@Context HttpServletRequest req) throws Exception {
        configServiceProvider.checkElectionStates();
        return Response.ok().entity("check OK").build();
    }

    @GET @Path("/push/{socketClientId}")
    public Response push(@PathParam("socketClientId") String socketClientId, @Context HttpServletRequest req) throws Exception {
        ResponseDto response = new ResponseDto(ResponseDto.SC_OK, "messsage");
        serviceUpdated.send(JSON.getMapper().writeValueAsString(response), socketClientId);
        return Response.ok().entity("push sent").build();
    }

    @GET @Path("/cert")
    public Response cert(@Context HttpServletRequest req) throws Exception {
        return Response.ok().entity("KeyHash").build();
    }

    @GET @Path("/saveElection")
    public Response saveElection(@Context HttpServletRequest req) throws Exception {
        List<SignedDocument> signedDocumentList = em.createQuery("SELECT sd FROM SignedDocument sd WHERE sd.id=:signedDocumentId")
                .setParameter("signedDocumentId", 6L).getResultList();
        SignedDocument signedDocument = signedDocumentList.iterator().next();
        ElectionDto electionDto = XML.getMapper().readValue(signedDocument.getBody(), ElectionDto.class);
        electionDto.validatePublishRequest();
        Election election = new Election(electionDto, signedDocument);
        em.persist(election);
        return Response.ok().entity("SignedDocument OK election: " + election.getId()).build();
    }

}