package org.votingsystem.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSON {

    private static Logger log = Logger.getLogger(JSON.class.getSimpleName());

    private static final JSON INSTANCE = new JSON();
    private ObjectMapper mapper;

    public JSON() {
        mapper = new ObjectMapper(); // create once, reuse
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(dateFormat);
        mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        //mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,true);
        //mapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,true);
    }

    public static JSON getInstance() {
        return INSTANCE;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }


    public byte[] writeValueAsBytes(Map data) {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); return null;}
    }

    public String writeValueAsString(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); return null;}
    }

    public void writeValueAsFile(File dest, Map data) {
        try {
            mapper.writeValue(dest, data);
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
    }

    public Map readValue(byte[] src) {
        try {
            return mapper.readValue(src, Map.class);
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); return null;}
    }

    public Map readValue(String src) {
        try {
            return mapper.readValue(src, Map.class);
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); return null;}
    }

    public Map readValue(File src) {
        try {
            return mapper.readValue(src, Map.class);
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); return null;}
    }

}
