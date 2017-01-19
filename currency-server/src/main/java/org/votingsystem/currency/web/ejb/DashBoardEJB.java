package org.votingsystem.currency.web.ejb;

import org.votingsystem.dto.currency.DashBoardDto;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.Interval;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class DashBoardEJB {

    private static Logger log = Logger.getLogger(DashBoardEJB.class.getName());

    @PersistenceContext
    private EntityManager em;

     public DashBoardDto getUserInfo(Interval timePeriod) {
         log.info("timePeriod: " + timePeriod.toString());
         DashBoardDto dto = new DashBoardDto(timePeriod);
         Query query = em.createNamedQuery(Transaction.COUNT_BY_TO_USER_NOT_NULL_AND_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime()).setParameter("type", Transaction.Type.FROM_BANK);
         dto.setNumTransFromBank((long)query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.FROM_USER);
         dto.setNumTransFromUser((long) query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.CURRENCY_PERIOD_INIT);
         dto.setNumTransCurrencyInitPeriod((long) query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED);
         dto.setNumTransCurrencyInitPeriodTimeLimited((long) query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.CURRENCY_REQUEST);
         dto.setNumTransCurrencyRequest((long) query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.CURRENCY_SEND);
         dto.setNumTransCurrencySend((long)query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.CURRENCY_CHANGE);
         dto.setNumTransCurrencyChange((long)query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", Transaction.Type.CANCELLATION);
         dto.setNumTransCancellation((long) query.getSingleResult());
         return dto;
    }

}