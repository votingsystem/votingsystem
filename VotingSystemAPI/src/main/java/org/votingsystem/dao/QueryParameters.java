package org.votingsystem.dao;

import java.util.HashMap;
import java.util.Map;

/**
 * https://github.com/ccordenier/tapestry5-hotel-booking
 */
public class QueryParameters {

    private Map<String, Object> parameters = null;

    private QueryParameters(String name, Object value) {
        this.parameters = new HashMap<String, Object>();
        this.parameters.put(name, value);
    }

    public static QueryParameters with(String name, Object value)
    {
        return new QueryParameters(name, value);
    }

    public QueryParameters and(String name, Object value) {
        this.parameters.put(name, value);
        return this;
    }

    public Map<String, Object> parameters() {
        return this.parameters;
    }

}