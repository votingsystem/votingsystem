package org.votingsystem.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.votingsystem.model.EventVS;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository("searchHelper")
public class SearchHelper {

    private static Logger logger = LoggerFactory.getLogger(SearchHelper.class);

    // a Hibernate SessionFactory
    @Autowired private SessionFactory sessionFactory;

    @Transactional(readOnly = true) public <T> List<T> findByFullText(Class<T> entityClass,
            String[] entityFields, String textToFind, int firstResult, int maxResults) {
        FullTextSession session = Search.getFullTextSession(
                sessionFactory.getCurrentSession());
        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_31,
        		entityFields, new StandardAnalyzer(Version.LUCENE_31));
        Query luceneQuery;
        try {
            luceneQuery = parser.parse(textToFind);
        } catch (ParseException e) {
            logger.error("Cannot parse [" + textToFind + "] to a full text query", e);
            return new ArrayList<T>(0);
        }
        FullTextQuery query = session.createFullTextQuery( luceneQuery, entityClass );
        return query.setFirstResult(firstResult).setMaxResults(maxResults).list();
    }

    @Transactional(readOnly = true) public FullTextQuery getFullTextQuery(Class<?> entityClass,
            String[] entityFields, String textToFind) {
        FullTextSession session = Search.getFullTextSession(
                sessionFactory.getCurrentSession());
        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_31,
        		entityFields, new StandardAnalyzer(Version.LUCENE_31));
        Query luceneQuery;
        try {
            luceneQuery = parser.parse(textToFind);
        } catch (ParseException e) {
            logger.error("Cannot parse [" + textToFind + "] to a full text query", e);
            return null;
        }
        return session.createFullTextQuery( luceneQuery, entityClass );
    }
    
    @Transactional(readOnly = true)
    public FullTextQuery getCombinedQuery(Class<?> entityClass, String[] entityFields, String textToFind,
            Date dateBeginFrom, Date dateBeginTo, Date dateFinishFrom, Date dateFinishTo, List<EventVS.State> states) {
    	logger.debug("getCombinedQuery -- dateBeginFrom: " + dateBeginFrom +
    			" -dateBeginTo: " + dateBeginTo + "dateFinishFrom: " + dateFinishFrom +
    			" -dateFinishTo: " + dateFinishTo);
        FullTextSession session = Search.getFullTextSession(
                sessionFactory.getCurrentSession());
        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_31,
        		entityFields, new StandardAnalyzer(Version.LUCENE_31));
        Query luceneQuery = null;
        try {
        	if(textToFind != null && !textToFind.trim().isEmpty())
        		luceneQuery = parser.parse(textToFind);
        } catch (ParseException e) {
            logger.error("Cannot parse [" + textToFind + "] to a full text query", e);
            return null;
        }
        SearchFactory searchFactory = session.getSearchFactory();
        QueryBuilder queryBuilder = searchFactory.buildQueryBuilder().forEntity(entityClass).get();
		BooleanJunction<BooleanJunction> booleanJunction = null;
		if(luceneQuery != null) {
			if(booleanJunction == null) booleanJunction = queryBuilder.bool();
			booleanJunction.must(luceneQuery);
		} 
        if(dateBeginFrom != null) {
        	logger.debug("Restringiendo por fechaIncioDesde");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("dateBegin").above(dateBeginFrom).createQuery());
        } 
        if(dateBeginTo != null) {
        	logger.debug("Restringiendo por fechaIncioHasta");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("dateBegin").below(dateBeginTo).createQuery());
        } 
        if(dateFinishFrom != null) {
        	logger.debug("Restringiendo por dateFinishFrom");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("dateFinish").above(dateFinishFrom).createQuery());
        }        
        if(dateFinishTo != null) {
        	logger.debug("Restringiendo por dateFinishTo");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("dateFinish").below(dateFinishTo).createQuery());
        }
        if(states != null) {
            BooleanQuery stateFilter = new BooleanQuery();
            stateFilter.setMinimumNumberShouldMatch(1);
            for(EventVS.State state:states) {
                stateFilter.add(queryBuilder.phrase().onField("state").
                        sentence(state.toString()).createQuery(), BooleanClause.Occur.SHOULD);
            }
            if(booleanJunction == null) booleanJunction = queryBuilder.bool();
            booleanJunction.must(stateFilter);
        }
        if(booleanJunction == null) return null;
        return session.createFullTextQuery( booleanJunction.createQuery(), entityClass );
    }
    
}
