package org.votingsystem.util;

import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.TagVS;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MapUtils {

    public static class TagData {
        String tag;
        BigDecimal total;
        BigDecimal timeLimited;
        public TagData(String tag, BigDecimal total, BigDecimal timeLimited) {
            this.tag = tag;
            this.total = total;
            this.timeLimited = timeLimited;
        }

        public TagData(String tag, BigDecimal total) {
            this.tag = tag;
            this.total = total;
        }
    }

    public static Set<Map> getTagSet(Collection<TagVS> tags) {
        Set<Map> result = new HashSet<>();
        for(TagVS tag : tags) {
            Map tagMap = new HashMap<>();
            tagMap.put("id", tag.getId());
            tagMap.put("content", tag.getName());
            result.add(tagMap);
        }
        return result;
    }

    public static Set<Map> getFieldSet(Collection<FieldEventVS> fields) {
        Set<Map> result = new HashSet<>();
        for(FieldEventVS field : fields) {
            Map fieldMap = new HashMap<>();
            fieldMap.put("id", field.getId());
            fieldMap.put("content", field.getContent());
            result.add(fieldMap);
        }
        return result;
    }

    public static Map<String, String> getActorVSMap(String serverURL, String name) {
        Map result = new HashMap<>();
        result.put("serverURL", serverURL);
        result.put("name", name);
        return result;
    }

    public static Map<String, Map> getTagMapForIncomes(TagData... tags) {
        //HIDROGENO:[total:new BigDecimal(880.5), timeLimited:new BigDecimal(700.5)]
        Map result = new HashMap<>();
        for(TagData tagData : tags) {
            Map tagmap = new HashMap<>();
            tagmap.put("total", tagData.total);
            tagmap.put("timeLimited", tagData.timeLimited);
            result.put(tagData.tag, tagmap);
        }
        return result;
    }

    public static Map<String, BigDecimal> getTagMapForExpenses(TagData... tags) {
        Map result = new HashMap<>();
        for(TagData tagData : tags) {
            result.put(tagData.tag, tagData.total);
        }
        return result;
    }

    public static Map getTagMap(TagVS tagVS) {
        Map result = new HashMap<>();
        result.put("id", tagVS.getId());
        result.put("name", tagVS.getName());
        return null;
    }
}
