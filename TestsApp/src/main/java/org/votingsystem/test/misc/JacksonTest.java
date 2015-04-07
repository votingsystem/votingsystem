package org.votingsystem.test.misc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.votingsystem.json.RepresentativeDelegationRequest;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JacksonTest {

    private static final Logger log = Logger.getLogger(JacksonTest.class.getSimpleName());

    public static void main(String[] args) throws Exception {
        Map testMap = new HashMap<>();
        testMap.put("statusCode", 200);
        testMap.put("message", "O'tool with 'quotes'");
        log.info("testMap: " + JSON.getEscapingMapper().writeValueAsString(testMap));
    }

    //Map<String, Object> map = new ObjectMapper().readValue(jsonStr, new TypeReference<HashMap<String, Object>>() {});
    private static void treeModelfromString() throws IOException {
        String jsonStr = null;
        //JsonNode map = new ObjectMapper().readTree(jsonStr);
        ObjectNode map = (ObjectNode) new ObjectMapper().readTree(jsonStr);
        map.put("insertedNode", 111111111111L);

        log.log(Level.FINE, "group class: " + map.get("group").getClass());
        log.log(Level.FINE, "-- group name: " + map.get("group").get("name").asText());
        log.info("listParam class: " + map.get("listParam").getClass());
        Iterator<JsonNode> ite = ((ArrayNode)map.get("listParam")).elements();
        while (ite.hasNext()) {
            JsonNode temp = ite.next();
            log.info("item from array: " + temp.asText());
        }
        //log.info("group name: " + ((Map)map.get("group")).get("name"));
        log.info("doubleParam: " + map.get("doubleParam"));
        log.info("intParam: " + map.get("intParam").longValue());
        log.info("missing value: " + (map.get("missing") == null));
        log.info("missing value: " + map.get("nullValueStr").getClass());
        log.info("fromString: " + map);
    }

    //http://www.mkyong.com/java/how-to-convert-java-map-to-from-json-jackson/
    private static void printString() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        map.put("statusCode", 200);
        map.put("message", "hello test");
        //convert map to JSON string
        String result = new ObjectMapper().writeValueAsString(map);
        //new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT,true).writeValueAsString(dataMap)
        log.info("result: " + result);
    }

    //http://wiki.fasterxml.com/JacksonTreeModel
    public static void treeModel() throws  Exception {
        ObjectMapper mapper = new ObjectMapper();
        BufferedReader fileReader = new BufferedReader(new FileReader("c:\\user.json"));
        JsonNode rootNode = mapper.readTree(fileReader);
        /*** read ***/
        JsonNode nameNode = rootNode.path("name");
        log.info(nameNode.asText());
        JsonNode ageNode = rootNode.path("age");
        log.info("int: " + ageNode.asInt());
        JsonNode msgNode = rootNode.path("messages");
        Iterator<JsonNode> ite = msgNode.elements();
        while (ite.hasNext()) {
            JsonNode temp = ite.next();
            log.info(temp.asText());
        }
        /*** update ***/
        ((ObjectNode)rootNode).put("nickname", "new nickname");
        ((ObjectNode)rootNode).put("name", "updated name");
        ((ObjectNode)rootNode).remove("age");
        mapper.writeValue(System.out, rootNode);
    }

}
