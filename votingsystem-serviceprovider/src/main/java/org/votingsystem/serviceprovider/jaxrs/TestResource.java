package org.votingsystem.serviceprovider.jaxrs;

import eu.europa.esig.dss.InMemoryDocument;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.Election;
import org.votingsystem.serviceprovider.ejb.ConfigServiceProvider;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Enumeration;
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
    @Inject ConfigServiceProvider configServiceProvider;
    @Inject TrustedServicesEJB trustedServices;
    @Inject private Config config;
    @EJB QRSessionsEJB qrSessionsEJB;

    @GET @Path("/")
    public Response test(@Context HttpServletRequest req) throws Exception {
        List<SignedDocument> signedDocumentList = em.createQuery("SELECT sd FROM SignedDocument sd WHERE sd.id=:signedDocumentId")
                .setParameter("signedDocumentId", 6L).getResultList();
        SignedDocument signedDocument = signedDocumentList.iterator().next();
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                SignedDocumentType.NEW_ELECTION_REQUEST).setWithTimeStampValidation(true);
        FileUtils.copyBytesToFile(signedDocument.getBody().getBytes(), new File("/home/jgzornoza/temp/electionError.xml"));
        signedDocument = signatureService.validateXAdES(new InMemoryDocument(signedDocument.getBody().getBytes()), signatureParams);
        return Response.ok().entity("SignedDocument OK").build();
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

    @GET @Path("/sign")
    public Response sign(@Context HttpServletRequest req) throws Exception {
        String dummyRequest = "<Election><EntityId>https://192.168.1.5:8443/voting-service</EntityId><DateBegin>2016-11-29T01:00:00+01:00</DateBegin><DateFinish>2016-11-30T01:00:00+01:00</DateFinish><Subject>asunto votración</Subject><Content>&lt;pholaarial></Content><State>PENDING</State><Options><Option><Content>hola</Content></Option><Option><Content>hola1</Content></Option></Options><UUID>50f6a887-a740-42b9-862b-132fac654f7e</UUID></Election>";

        byte[] signatureBytes = signatureService.signXAdES(dummyRequest.getBytes());
        FileUtils.copyBytesToFile(signatureBytes, new File("/home/jgzornoza/temp/xadesSigned.xml"));
        return Response.ok().entity(signatureBytes).build();
    }

    @POST
    @Path("/ocsp")
    public Response publish(@Context HttpServletRequest req) throws Exception {
        byte[] ocspRequest = FileUtils.getBytesFromStream(req.getInputStream());
        Enumeration<String> headers = req.getHeaderNames();
        while(headers.hasMoreElements()) {
            String header = headers.nextElement();
            log.info("--- header : " + header + " - " + req.getHeader(header));
        }
        return Response.ok().entity("ocspRequest: " + new String(ocspRequest)).build();
    }

}