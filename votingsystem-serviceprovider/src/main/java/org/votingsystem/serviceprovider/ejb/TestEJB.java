package org.votingsystem.serviceprovider.ejb;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.Response;
import java.time.*;
import java.util.UUID;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TestEJB {

    private static final Logger log = Logger.getLogger(TestEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject @Push
    private PushContext serviceUpdated;


    public void loadTestElections(int numInserts, Election.State state) throws Exception {
        log.info("numInserts: " + numInserts + " - state: " + state);
        String userName = "User" + UUID.randomUUID().toString().substring(0, 5);
        String surname = "Surname" + UUID.randomUUID().toString().substring(0, 5);
        User publisher = new User(User.Type.USER, userName, surname,
                userName + "@" + "votingsystem", "5555555");
        //All elections begin at 00:00 GMT+00:00
        LocalDate localDate = LocalDate.now();
        ZonedDateTime zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
        LocalDateTime dateBegin = zonedDateBegin.toLocalDateTime();
        em.persist(publisher);
        int i = 0;
        for(; i < numInserts; i++) {
            switch (state) {
                case ACTIVE:
                    break;
                case PENDING:
                    localDate = LocalDate.now().plusDays(i + 1);
                    zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
                    dateBegin = zonedDateBegin.toLocalDateTime();
                    break;
                default:
                    localDate = LocalDate.now().plusDays(-(i + 1));
                    zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
                    dateBegin = zonedDateBegin.toLocalDateTime();
            }

            Election election = new Election("Asunto de la votación " + i,
                    "Contenido de la votación " + i, dateBegin, dateBegin.plusDays(1), state);
            election.setUUID(UUID.randomUUID().toString()).setPublisher(publisher);

            em.persist(election);
            ElectionOption electionOption1 = new ElectionOption("Option 1").setElection(election);
            ElectionOption electionOption2 = new ElectionOption("Option 2").setElection(election);
            em.persist(electionOption1);
            em.persist(electionOption2);
            if((i % 10) == 0 && i > 0)
                log.info("Num. inserts: " + i);
        }
        log.info("Num. inserts: " + i);

    }


    public Response push(String socketClientId) throws Exception {
        ResponseDto response = new ResponseDto(ResponseDto.SC_OK, "messsage");
        serviceUpdated.send(new JSON().getMapper().writeValueAsString(response), socketClientId);
        return Response.ok().entity("push sent").build();
    }

}