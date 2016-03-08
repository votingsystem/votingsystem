package org.votingsystem.web.util;

import org.hibernate.Criteria;
import org.hibernate.internal.CriteriaImpl;

import javax.persistence.Query;
import java.util.Iterator;
import java.util.List;


public class DAOUtils {

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
