package org.currency.web.ejb;

import org.votingsystem.dto.currency.DashBoardDto;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyOperation;
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
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime()).setParameter("type",
                         CurrencyOperation.TRANSACTION_FROM_BANK);
         dto.setNumTransFromBank((long)query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", CurrencyOperation.TRANSACTION_FROM_USER);
         dto.setNumTransFromUser((long) query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", CurrencyOperation.CURRENCY_REQUEST);
         dto.setNumTransCurrencyRequest((long) query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", CurrencyOperation.CURRENCY_SEND);
         dto.setNumTransCurrencySend((long)query.getSingleResult());
         query = em.createNamedQuery(Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN)
                 .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                 .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                 .setParameter("type", CurrencyOperation.CURRENCY_CHANGE);
         dto.setNumTransCurrencyChange((long)query.getSingleResult());
         return dto;
    }

}