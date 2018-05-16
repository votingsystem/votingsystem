package org.votingsystem.serviceprovider.jaxrs;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.ElectionStatsDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.http.HttpRequest;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.jsf.ServiceUpdatedMessage;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/election")
@Stateless
public class ElectionResourceEJB {

    private static final Logger log = Logger.getLogger(ElectionResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private SignatureServiceEJB signatureService;
    @Inject private Config config;
    @Inject private BeanManager beanManager;
    @EJB private QRSessionsEJB qrSessions;


    @POST @Path("/save")
    @Produces({"application/xml"})
    public Response save(@Context HttpServletRequest req, byte[] xmlRequestSigned) throws Exception {
        ElectionDto electionDto = XML.getMapper().readValue(xmlRequestSigned, ElectionDto.class);
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                SignedDocumentType.NEW_ELECTION_REQUEST).setWithTimeStampValidation(true);
        SignedDocument signedDocument = signatureService.validateXAdESAndSave(new InMemoryDocument(xmlRequestSigned), signatureParams);
        electionDto.validatePublishRequest();
        byte[] receiptXML = signatureService.signXAdES(xmlRequestSigned);
        signedDocument.setBody(new String(receiptXML));
        Election election = new Election(electionDto, signedDocument).setEntityId(config.getEntityId());
        em.persist(election);
        qrSessions.removeOperation(election.getUUID());
        ResponseDto response = new ResponseDto(ResponseDto.SC_OK,
                Messages.currentInstance().get("electionPublishOKMsg", election.getSubject()));
        beanManager.fireEvent(new ServiceUpdatedMessage(response, election.getUUID()));
        return Response.ok().entity(Messages.currentInstance().get("electionPublishOKMsg", election.getSubject())).build();
    }

    @POST @Path("/uuid")
    @Produces({"application/xml"})
    public Response uuid(@Context HttpServletRequest req, String electionUUID) throws Exception {
        List<Election> elections = em.createQuery("select e from Election e where e.uuid=:electionUUID")
                .setParameter("electionUUID", electionUUID).getResultList();
        if(elections.isEmpty())
            return Response.status(Response.Status.NOT_FOUND).entity(Messages.currentInstance().get("itemNotFoundErrorMsg")).build();
        else
            return Response.ok().entity(elections.iterator().next().getSignedDocument().getBody()).build();
    }

    @GET @Path("/list")
    public Response list(@QueryParam("state") String state,
                           @DefaultValue("0") @QueryParam("offset") int offset,
                           @DefaultValue("50") @QueryParam("max") int max, @Context ServletContext context,
                           @Context HttpServletRequest req, @Context HttpServletResponse res) throws ValidationException,
            IOException, ServletException {
        List<Election.State> inList = Arrays.asList(Election.State.ACTIVE);
        if(state != null) {
            try {
                Election.State electionState = Election.State.valueOf(state);
                if(electionState == Election.State.TERMINATED) {
                    inList = Arrays.asList(Election.State.TERMINATED, Election.State.CANCELED);
                } else if(electionState != Election.State.DELETED_FROM_SYSTEM) inList = Arrays.asList(electionState);
            } catch(Exception ex) {}
        }
        List<Election> resultList = em.createQuery("select e from Election e where e.state in :inList order by e.dateBegin desc ")
                .setParameter("inList", inList).setParameter("inList", inList).setMaxResults(max).setFirstResult(offset).getResultList();
        long totalCount = (long)em.createQuery("SELECT COUNT(e) FROM Election e where e.state in :inList")
                .setParameter("inList", inList).getSingleResult();
        List<ElectionDto> electionDtoList = new ArrayList<>();
        for(Election election : resultList) {
            electionDtoList.add(new ElectionDto(election));
        }
        ResultListDto<ElectionDto> resultListDto = new ResultListDto<>(electionDtoList, offset, max, totalCount);
        return HttpResponse.sendResponseDto(ResponseDto.SC_OK, req, res, resultListDto);
    }

    @GET @Path("/search")
    public Response search(@QueryParam("searchText") String searchText, @QueryParam("state") String electionStateStr,
                            @DefaultValue("0") @QueryParam("offset") int offset,
                            @DefaultValue("100") @QueryParam("max") int max,
                            @Context HttpServletRequest req, @Context HttpServletResponse res) throws IOException, ServletException {
        List<Election.State> inList = Arrays.asList(Election.State.ACTIVE, Election.State.PENDING,
                Election.State.CANCELED, Election.State.TERMINATED);
        if(electionStateStr != null) {
            try {
                Election.State electionState = Election.State.valueOf(electionStateStr);
                if(electionState == Election.State.TERMINATED) {
                    inList = Arrays.asList(Election.State.TERMINATED, Election.State.CANCELED);
                } else if(electionState != Election.State.DELETED_FROM_SYSTEM) inList = Arrays.asList(electionState);
            } catch(Exception ex) {}
        }
        List<Election> electionList = em.createQuery("select e from Election e where e.state in :inList and " +
                "(lower(e.subject) like :searchText or lower(e.content) like :searchText)").setParameter("inList", inList)
                .setParameter("searchText", "%" + searchText.toLowerCase() + "%").getResultList();
        List<ElectionDto> electionDtoList = new ArrayList<>();
        for(Election election : electionList) {
            electionDtoList.add(new ElectionDto(election));
        }
        ResultListDto<ElectionDto> resultListDto = new ResultListDto<>(electionDtoList, offset, max, electionList.size());
        return HttpResponse.sendResponseDto(ResponseDto.SC_OK, req, res, resultListDto);

    }

    @GET @Path("/uuid/{uuid}/stats")
    public Response getStats (@PathParam("uuid") String electionUUID, @Context HttpServletRequest req,
                              @Context HttpServletResponse res) throws IOException, ServletException {
        List<Election> elections = em.createQuery("select e from Election e where e.uuid=:electionUUID")
                .setParameter("electionUUID", electionUUID).getResultList();
        if(elections.isEmpty())
            return Response.status(Response.Status.NOT_FOUND).entity(Messages.currentInstance().get("itemNotFoundErrorMsg")).build();

        Election election = elections.iterator().next();
        ElectionStatsDto statsDto = new ElectionStatsDto(election);
        Query query = em.createQuery("select count(v) from Vote v where v.election =:election " +
                "and v.state =:state").setParameter("election", election).setParameter("state", Vote.State.OK);
        statsDto.setNumVotes((long) query.getSingleResult());
        statsDto.setNumVotes((long) query.getSingleResult());
        Set<ElectionOptionDto> electionOptions = new HashSet<>();
        for(ElectionOption option : election.getElectionOptions()) {
            query = em.createQuery("select count(v) from Vote v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", Vote.State.OK);
            electionOptions.add(new ElectionOptionDto(option.getContent(), (long) query.getSingleResult()));
        }
        statsDto.setElectionOptions(electionOptions);
        String reqContentType = HttpRequest.getContentType(req, true);
        if(reqContentType.contains("html")) {
            req.getSession().setAttribute("statsDto", statsDto);
            res.sendRedirect(req.getContextPath() + "/election/stats.xhtml");
            return Response.ok().build();
        } else return HttpResponse.sendResponseDto(ResponseDto.SC_OK, req, res, statsDto);
    }

}