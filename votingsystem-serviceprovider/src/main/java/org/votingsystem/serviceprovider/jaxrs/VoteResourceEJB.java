package org.votingsystem.serviceprovider.jaxrs;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/vote")
@Stateless
public class VoteResourceEJB {

    private static final Logger log = Logger.getLogger(VoteResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private SignatureService signatureService;
    @Inject private Config config;

    @POST @Path("/")
    public Response vote(@Context HttpServletRequest req, @Context HttpServletResponse res, byte[] signedVote)
            throws Exception {
        VoteDto voteDto = null;
        try {
            voteDto = XML.getMapper().readValue(signedVote, VoteDto.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Vote with bad format");
        }
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ANON_ELECTOR,
                SignedDocumentType.VOTE).setWithTimeStampValidation(true);
        SignedDocument signedDocument = signatureService.validateXAdESAndSave(new InMemoryDocument(signedVote), signatureParams);
        Certificate certificate = signedDocument.getAnonSigner().getCertificate();
        if(!signedDocument.getAnonSigner().isValidElector()) {
            String message = Messages.currentInstance().get("voteCertificateRepeatedErrorMsg");
            log.severe("VOTE REPEATED - signedDocument id: " + signedDocument.getId());
            return HttpResponse.sendResponseDto(ResponseDto.SC_ERROR_REQUEST_REPEATED, req, res,
                    new ResponseDto(ResponseDto.SC_ERROR_REQUEST_REPEATED, message));
        }
        voteDto.validate(certificate.getCertVoteExtension(), config.getEntityId());

        List<Election> electionList = em.createQuery("select e from Election e where e.uuid =:uuid")
                .setParameter("uuid", voteDto.getElectionUUID()).getResultList();
        if(electionList.isEmpty()) {
            throw new IllegalArgumentException("Election with uuid: " + voteDto.getElectionUUID() + " not found");
        }
        Election election = electionList.iterator().next();
        List<ElectionOption> electionOptions = em.createQuery("select eo from ElectionOption eo where eo.content =:content and " +
                "eo.election=:election").setParameter("content", voteDto.getOptionSelected().getContent()).setParameter(
                "election", election).getResultList();
        if(electionOptions.isEmpty()) {
            throw new IllegalArgumentException("Election with uuid: " + voteDto.getElectionUUID() +
                    " doesn't have the option: " + voteDto.getOptionSelected().getContent());
        }
        LocalDateTime voteDate = signedDocument.getSignatures().iterator().next().getSignatureDate();
        if(!voteDate.isAfter(election.getDateBegin()) || voteDate.isAfter(election.getDateFinish())) {
            throw new IllegalArgumentException(Messages.currentInstance().get("dateOutOfRangeErrorMsg",
                    DateUtils.getDateStr(voteDate), DateUtils.getDateStr(election.getDateBegin()),
                    DateUtils.getDateStr(election.getDateFinish())));
        }
        em.persist(new Vote(electionOptions.iterator().next(), election, Vote.State.OK, certificate, signedDocument,
                certificate.getCertVoteExtension().getIdentityServiceEntity()));
        byte[] receipt = signatureService.signXAdES(signedVote);
        return Response.ok().entity(receipt).build();
    }

    @POST @Path("/repository")
    public Response repository(@Context HttpServletRequest req, @Context HttpServletResponse res, String revocationHash)
            throws Exception {
        List<Vote> votes = em.createQuery("select v from Vote v where v.certificate.revocationHashBase64 =:revocationHashBase64 and " +
                "v.state=:state").setParameter("revocationHashBase64", revocationHash).setParameter(
                "state", Vote.State.OK).getResultList();
        if(votes.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity(Messages.currentInstance().get("voteNotFoundMsg")).build();
        }
        return Response.ok().type(MediaType.APPLICATION_XML_TYPE)
                .entity(votes.iterator().next().getSignedDocument().getBody()).build();
    }

}
