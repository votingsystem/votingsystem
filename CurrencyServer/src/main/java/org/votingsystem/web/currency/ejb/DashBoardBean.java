package org.votingsystem.web.currency.ejb;


import org.votingsystem.dto.DashBoardDto;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.Interval;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class DashBoardBean {

    private static Logger log = Logger.getLogger(DashBoardBean.class.getName());

    @PersistenceContext private EntityManager em;

     public DashBoardDto getUserInfo(Interval timePeriod) {
         log.info("timePeriod: " + timePeriod.toString());
         DashBoardDto dto = new DashBoardDto(timePeriod);
         Query query = em.createNamedQuery("countTransByToUserIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.FROM_BANK);
         dto.setNumTransFromBank((long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.FROM_USER);
         dto.setNumTransFromUser((long) query.getSingleResult());
         query = em.createNamedQuery("countTransByToUserIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.FROM_GROUP_TO_MEMBER_GROUP);
         Long numTrans = (long)query.getSingleResult();
         query = em.createNamedQuery("countTransByToUserIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.FROM_GROUP_TO_MEMBER_GROUP);
         Long numUsers = (long)query.getSingleResult();
         dto.setTransFromGroupToMemberGroup(new DashBoardDto.TransFromGroup(numTrans, numUsers));
         query = em.createNamedQuery("countTransByToUserIsNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.FROM_GROUP_TO_ALL_MEMBERS);
         numTrans = (long)query.getSingleResult();
         query = em.createNamedQuery("countTransByToUserIsNotNullAndTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.FROM_GROUP_TO_ALL_MEMBERS);
         numUsers = (long)query.getSingleResult();
         dto.setTransFromGroupToAllMembers(new DashBoardDto.TransFromGroup(numTrans, numUsers));
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.CURRENCY_PERIOD_INIT);
         dto.setNumTransCurrencyInitPeriod((long) query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED);
         dto.setNumTransCurrencyInitPeriodTimeLimited((long) query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.CURRENCY_REQUEST);
         dto.setNumTransCurrencyRequest((long) query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.CURRENCY_SEND);
         dto.setNumTransCurrencySend((long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.CURRENCY_CHANGE);
         dto.setNumTransCurrencyChange((long)query.getSingleResult());
         query = em.createNamedQuery("countTransByTypeAndDateCreatedBetween").setParameter(
                 "dateFrom", timePeriod.getDateFrom(), TemporalType.TIMESTAMP).setParameter("dateTo", timePeriod.getDateTo(),
                 TemporalType.TIMESTAMP).setParameter("type", Transaction.Type.CANCELLATION);
         dto.setNumTransCancellation((long) query.getSingleResult());
         return dto;
    }

}
