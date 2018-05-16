package org.votingsystem.serviceprovider.jaxrs;

import org.votingsystem.dto.ResponseDto;
import org.votingsystem.model.voting.Election;
import org.votingsystem.serviceprovider.dto.ElectionsDTDto;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Front end for election-list.xml data table
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/election-list-dt")
@Stateless
public class ElectionListDTResourceEJB {

    private static final Logger log = Logger.getLogger(ElectionListDTResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;

    // The values must be correlated with the header of the table in election-list.xhtml
    private static final int SUBJECT = 0;
    private static final int USER    = 1;
    private static final int DATE    = 2;
    private static final int STATE   = 3;


    /**
     * Front end for election-list.xml data table
     * @param draw
     * @param searchText
     * @param start
     * @param length
     * @param columnOrder
     * @param orderStr
     * @param req
     * @param res
     * @return
     * @throws IOException
     * @throws ServletException
     */
    @POST @Path("/")
    public Response dataTablesFrontEnd(@FormParam("draw") int draw,
                           @FormParam("search[value]") String searchText, @FormParam("start") int start,
                           @FormParam("length") int length, @FormParam("order[0][column]") int columnOrder,
                           @FormParam("order[0][dir]") String orderStr,
                           @Context HttpServletRequest req, @Context HttpServletResponse res) throws IOException, ServletException {
        /*Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = req.getParameter(paramName);
            log.info("paramName: " + paramName + " - paramValue: " + paramValue);
        }*/
        log.info("searchStr: " + searchText + " - start: " + start + " - length: " + length +
                " - columnOrderStr: " + columnOrder + " - orderStr: " + orderStr);
        searchText = searchText.trim();
        List<Election.State> stateInList = Arrays.asList(Election.State.ACTIVE, Election.State.PENDING,
                Election.State.CANCELED, Election.State.TERMINATED);
        Long numRecordsFiltered = 0L;

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Election> cq = builder.createQuery(Election.class);
        Root<Election> root = cq.from(Election.class);

        List<Election> electionList = null;

        List<Order> orderList = new ArrayList<>();
        switch(columnOrder) {
            case SUBJECT:
                if("asc".equals(orderStr))
                    orderList.add(builder.asc(root.get("subject")));
                else
                    orderList.add(builder.desc(root.get("subject")));
                break;
            case USER:
                if("asc".equals(orderStr)) {
                    orderList.add(builder.asc(root.get("publisher").<String>get("name")));
                    orderList.add(builder.asc(root.get("publisher").<String>get("surname")));
                } else {
                    orderList.add(builder.desc(root.get("publisher").<String>get("name")));
                    orderList.add(builder.desc(root.get("publisher").<String>get("surname")));
                }
                break;
            case DATE:
                if("asc".equals(orderStr))
                    orderList.add(builder.asc(root.get("dateBegin")));
                else
                    orderList.add(builder.desc(root.get("dateBegin")));
                break;
            case STATE:
                if("asc".equals(orderStr))
                    orderList.add(builder.asc(root.get("state")));
                else
                    orderList.add(builder.desc(root.get("state")));
                break;
            default:
        }

        long numRecordsTotal = (long) em.createQuery("SELECT COUNT(e) FROM Election e where e.state in :inList")
                .setParameter("inList", stateInList).getSingleResult();

        Predicate resultPredicate = null;
        if(searchText == null && searchText.length() == 0) {
            resultPredicate = root.get("state").in(stateInList);
            numRecordsFiltered = numRecordsTotal;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd");
            LocalDateTime searchDate = null;
            try {
                LocalDate localDate = LocalDate.parse(searchText, DateTimeFormatter.ofPattern("yyyy MM dd"));
                ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
                searchDate = zonedDateTime.toLocalDateTime();
            } catch (Exception ex) { }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(getSearchSubjectPredicate(builder, root, searchText));
            predicates.add(getSearchPublisherNamePredicate(builder, root, searchText));
            predicates.add(getSearchPublisherSurnamePredicate(builder, root, searchText));
            if(searchDate != null)
                predicates.add(builder.equal(root.get("dateBegin"), searchDate));

            Map<String, Election.State> headersI18N = new HashMap<>();
            headersI18N.put(Messages.currentInstance().get("pendingLbl"), Election.State.PENDING);
            headersI18N.put(Messages.currentInstance().get("openLbl"), Election.State.ACTIVE);
            headersI18N.put(Messages.currentInstance().get("closedLbl"), Election.State.TERMINATED);
            Election.State stateMatch = null;
            for(String msgI18n : headersI18N.keySet()) {
                if(msgI18n.toLowerCase().contains(searchText))
                    stateMatch = headersI18N.get(msgI18n);
            }

            CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            Root<Election> rootCount = countQuery.from(Election.class);
            List<Predicate> countPredicates = new ArrayList<>();
            countPredicates.add(getSearchSubjectPredicate(builder, rootCount, searchText));
            countPredicates.add(getSearchPublisherNamePredicate(builder, rootCount, searchText));
            countPredicates.add(getSearchPublisherSurnamePredicate(builder, rootCount, searchText));
            if(searchDate != null)
                countPredicates.add(builder.equal(rootCount.get("dateBegin"), searchDate));

            if(stateMatch != null) {
                predicates.add(root.get("state").in(Arrays.asList(stateMatch)));
                countPredicates.add(rootCount.get("state").in(Arrays.asList(stateMatch)));
            }
            resultPredicate = builder.or(predicates.toArray(new Predicate[predicates.size()]));

            Predicate countResultPredicate = builder.or(countPredicates.toArray(new Predicate[countPredicates.size()]));
            countQuery.select(builder.count(rootCount)).where(countResultPredicate);
            numRecordsFiltered = em.createQuery(countQuery).getSingleResult();
        }

        cq.where(resultPredicate).orderBy(orderList.toArray(new Order[orderList.size()]));

        TypedQuery<Election> typedQuery = em.createQuery(cq);
        electionList = typedQuery.setFirstResult(start).setMaxResults(length).getResultList();

        ElectionsDTDto responseDto = new ElectionsDTDto(draw, Long.valueOf(numRecordsTotal).intValue(),
                Long.valueOf(numRecordsFiltered).intValue(), electionList );
        res.setContentType(javax.ws.rs.core.MediaType.APPLICATION_JSON);
        res.getOutputStream().write(JSON.getMapper().writeValueAsBytes(responseDto));
        res.setStatus(ResponseDto.SC_OK);
        return Response.status(ResponseDto.SC_OK).build();
    }


    private Predicate getSearchSubjectPredicate(CriteriaBuilder builder, Root<Election> root, String searchText) {
        return builder.like(builder.lower(root.get("subject")), "%" + searchText.toLowerCase() + "%" );
    }

    private Predicate getSearchPublisherNamePredicate(CriteriaBuilder builder, Root<Election> root, String searchText) {
        return builder.like(builder.lower(root.get("publisher").<String>get("name")), "%" + searchText.toLowerCase() + "%" );
    }

    private Predicate getSearchPublisherSurnamePredicate(CriteriaBuilder builder, Root<Election> root, String searchText) {
        return builder.like(builder.lower(root.get("publisher").<String>get("surname")), "%" + searchText.toLowerCase() + "%" );
    }

}