package org.votingsystem.serviceprovider.jaxrs;

import org.votingsystem.model.voting.Election;
import org.votingsystem.serviceprovider.ejb.TestEJB;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
@Stateless
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getName());

    @Inject
    private TestEJB testEJB;
    @PersistenceContext
    private EntityManager em;


    @GET @Path("/")
    public Response test(@Context HttpServletRequest req) throws Exception {
        testEJB.loadTestElections(10, Election.State.ACTIVE);
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/query")
    public Response query(@Context HttpServletRequest req) throws Exception {
        LocalDate localDate = LocalDate.parse("2018 05 15", DateTimeFormatter.ofPattern("yyyy MM dd"));
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
        LocalDateTime searchDate = zonedDateTime.toLocalDateTime();
        LocalDateTime dateFrom = searchDate.minusSeconds(1);
        LocalDateTime dateTo = searchDate.plusSeconds(1);
        log.info("searchDate: " + searchDate + " - dateFrom: " + dateFrom + " - dateTo: " + dateTo);
        //long numResults = (long)em.createQuery("SELECT COUNT (e) FROM Election e where e.dateBegin between :dateFrom and :dateTo")
        //        .setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo).getSingleResult();
        long numResults = (long)em.createQuery("SELECT COUNT (e) FROM Election e where e.dateBegin = :searchDate")
                .setParameter("searchDate", searchDate).getSingleResult();
        return Response.ok().entity("num results: " + numResults).build();
    }


}