package org.votingsystem.model;

import java.io.Serializable;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FieldEventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private EventVS eventVS;
    private String content;
    private String value;

    public FieldEventVS() {}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static FieldEventVS populate (Map fieldMap) {
        FieldEventVS fieldEvent = null;
        try {
            fieldEvent = new FieldEventVS();
            if(fieldMap.containsKey("id"))
                fieldEvent.setId(((Integer) fieldMap.get("id")).longValue());
            if(fieldMap.containsKey("content")) {
                fieldEvent.setContent((String) fieldMap.get("content"));
            }
            if(fieldMap.containsKey("value")) {
                fieldEvent.setValue((String) fieldMap.get("value"));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return fieldEvent;
    }

}