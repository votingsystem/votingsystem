package org.votingsystem.util;

import org.hibernate.Criteria;
import org.hibernate.internal.CriteriaImpl;

import javax.persistence.Query;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class with utility methods to work with JPA
 *
 * @author oesia
 */
public class JPA {

    /**
     * This method is to avoid null pointer exceptions with empty results on query.getSingleResult()
     *
     * @param type
     * @param query
     * @param <T>
     * @return a single item of a query result or null if there are no matches
     */
    public static <T> T getSingleResult(Class<T> type, Query query){
        List results = query.getResultList();
        if(!results.isEmpty()){
            return (T) results.get(0);
        } else return null;
    }

    //to avoid exceptions when adding orderings and Projections.rowCount
    public static Criteria cleanOrderings(Criteria criteria) {
        Iterator<CriteriaImpl.OrderEntry> orderIter = ((CriteriaImpl)criteria).iterateOrderings();
        while (orderIter.hasNext()) {
            orderIter.next();
            orderIter.remove();
        }
        return criteria;
    }

}
