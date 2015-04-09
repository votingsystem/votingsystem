package org.votingsystem.web.util;

import javax.persistence.Query;
import java.util.List;


public class DAOUtils {

    public static <T> T getSingleResult(Class<T> type, Query query){
        List results = query.getResultList();
        if(!results.isEmpty()){
            return (T) results.get(0);
        } else return null;
    }
}
