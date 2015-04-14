package org.votingsystem.web.currency.ejb;


import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.TimePeriod;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class DashBoardBean {

    private static Logger log = Logger.getLogger(DashBoardBean.class.getSimpleName());

    @PersistenceContext private EntityManager em;

     public Map getUserVSInfo(TimePeriod timePeriod) {
         log.info("timePeriod: " + timePeriod.toString());
         Map result = new HashMap<>();
         result.put("timePeriod", timePeriod);

         Query query = em.createNamedQuery("countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_BANKVS);
         result.put("numTransFromBankVS", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_USERVS);
         result.put("numTransFromUserVS", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_MEMBER);
         result.put("numTransFromUserVS", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_MEMBER);
         result.put("numTransFromGroupVSToMember", (long)query.getSingleResult());

         Map resultFromGroupVSToMemberGroup = new HashMap<>();
         query = em.createNamedQuery("countTransByToUserVSIsNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP);
         resultFromGroupVSToMemberGroup.put("numTrans", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP);
         resultFromGroupVSToMemberGroup.put("numUsers", (long)query.getSingleResult());
         result.put("transFromGroupVSToMemberGroup", resultFromGroupVSToMemberGroup);
         query = em.createNamedQuery("countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP);
         result.put("numTransFromGroupVSToMemberGroup", (long)query.getSingleResult());

         Map resultTransFromGroupVSToMemberGroup = new HashMap<>();
         query = em.createNamedQuery("countTransByToUserVSIsNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS);
         resultTransFromGroupVSToMemberGroup.put("numTrans", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS);
         resultTransFromGroupVSToMemberGroup.put("numUsers", (long)query.getSingleResult());
         result.put("numTransFromGroupVSToAllMembers", resultTransFromGroupVSToMemberGroup);
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.CURRENCY_INIT_PERIOD);
         result.put("numTransCurrencyInitPeriod", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.CURRENCY_INIT_PERIOD_TIME_LIMITED);
         result.put("numTransCurrencyInitPeriodTimeLimited", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.CURRENCY_REQUEST);
         result.put("numTransCurrencyRequest", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.CURRENCY_SEND);
         result.put("numTransCurrencySend", (long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", TransactionVS.Type.CANCELLATION);
         result.put("numTransCancellation", (long)query.getSingleResult());
        return result;
    }

}
