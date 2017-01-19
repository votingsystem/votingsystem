package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.votingsystem.dto.SystemOperationDeserializer;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSON {

    private static Logger log = Logger.getLogger(JSON.class.getName());

    public static class HTMLCharacterEscapes extends CharacterEscapes {
        private final int[] asciiEscapes;

        public HTMLCharacterEscapes() {
            // start with set of characters known to require escaping (double-quote, backslash etc)
            int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
            // and force escaping of a few others:
            //esc['<'] = CharacterEscapes.ESCAPE_STANDARD;
            //esc['>'] = CharacterEscapes.ESCAPE_STANDARD;
            esc['&'] = CharacterEscapes.ESCAPE_STANDARD;
            esc['\''] = CharacterEscapes.ESCAPE_STANDARD;
            asciiEscapes = esc;
        }
        // this method gets called for character codes 0 - 127
        @Override public int[] getEscapeCodesForAscii() {
            return asciiEscapes;
        }
        // and this for others; we don't need anything special here
        @Override public SerializableString getEscapeSequence(int ch) {
            // no further escaping (beyond ASCII chars) needed:
            return null;
        }
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        mapper.getJsonFactory().setCharacterEscapes(new HTMLCharacterEscapes());

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        javaTimeModule.addDeserializer(ZonedDateTime.class, InstantDeserializer.ZONED_DATE_TIME);

        mapper.registerModule(javaTimeModule);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(SystemOperation.class, new SystemOperationDeserializer());
        mapper.registerModule(module);

        return mapper;
    }

}
