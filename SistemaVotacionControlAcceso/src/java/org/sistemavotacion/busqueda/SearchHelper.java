package org.sistemavotacion.busqueda;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.sistemavotacion.controlacceso.modelo.Evento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository("searchHelper")
public class SearchHelper {

    private static Logger logger =
            LoggerFactory.getLogger(SearchHelper.class);

    // a Hibernate SessionFactory
    @Autowired private SessionFactory sessionFactory;

    @Transactional(readOnly = true)
    public <T> List<T> findByFullText(Class<T> entityClass,
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
    
    @Transactional(readOnly = true)
    public FullTextQuery getFullTextQuery(Class<?> entityClass,
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
    public FullTextQuery getCombinedQuery(Class<?> entityClass,
            String[] entityFields, String textToFind, Date fechaInicioDesde,
            Date fechaInicioHasta, Date fechaFinDesde, Date fechaFinHasta, 
            List<Evento.Estado> estados) {
    	logger.debug("getCombinedQuery -- fechaInicioDesde: " + fechaInicioDesde + 
    			" -fechaInicioHasta: " + fechaInicioHasta + "fechaFinDesde: " + fechaFinDesde + 
    			" -fechaFinHasta: " + fechaFinHasta);
        FullTextSession session = Search.getFullTextSession(
                sessionFactory.getCurrentSession());
        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_31,
        		entityFields, new StandardAnalyzer(Version.LUCENE_31));
        Query luceneQuery = null;
        try {
        	if(textToFind != null && !"".equals(textToFind))
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
        if(fechaInicioDesde != null) {
        	logger.debug("Restringiendo por fechaIncioDesde");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("fechaInicio").above(fechaInicioDesde).createQuery());
        } 
        if(fechaInicioHasta != null) {
        	logger.debug("Restringiendo por fechaIncioHasta");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("fechaInicio").below(fechaInicioHasta).createQuery());
        } 
        if(fechaFinDesde != null) {
        	logger.debug("Restringiendo por fechaFinDesde");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("fechaFin").above(fechaFinDesde).createQuery()); 
        }        
        if(fechaFinHasta != null) {
        	logger.debug("Restringiendo por fechaFinHasta");
        	if(booleanJunction == null) booleanJunction = queryBuilder.bool();
        	booleanJunction.must(queryBuilder.range()
        	        .onField("fechaFin").below(fechaFinHasta).createQuery()); 
        } 
        if(estados != null) {
        	BooleanQuery estadoFilter = new BooleanQuery();
        	estadoFilter.setMinimumNumberShouldMatch(1);
        	for(Evento.Estado estado:estados) {
        		estadoFilter.add(queryBuilder.phrase().onField("estado").
        				sentence(estado.toString()).createQuery(), BooleanClause.Occur.SHOULD);
        	}
			if(booleanJunction == null) booleanJunction = queryBuilder.bool();
			booleanJunction.must(estadoFilter);
        }
        if(booleanJunction == null) return null;
        return session.createFullTextQuery( booleanJunction.createQuery(), entityClass );
    }
    
}
