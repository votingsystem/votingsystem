package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class EventTagVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private EventVS eventVS;
    private TagVS tagVS;

    public EventTagVS() { }

    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public EventVS getEventVS() {
        return this.eventVS;
    }
    
    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public TagVS getTagVS() {
        return this.tagVS;
    }
    
    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

}


