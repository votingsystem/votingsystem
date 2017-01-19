package org.votingsystem.xml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.votingsystem.dto.SystemOperationDeserializer;
import org.votingsystem.util.SystemOperation;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XML {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public static ObjectMapper getMapper() {
        final ObjectMapper mapper = new XmlMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(FORMATTER));
        javaTimeModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());

        mapper.registerModule(javaTimeModule);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(SystemOperation.class, new SystemOperationDeserializer());
        mapper.registerModule(module);

        return mapper;
    }

    public static class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {
        @Override
        public ZonedDateTime deserialize(JsonParser arg0, DeserializationContext arg1) throws IOException {
            return ZonedDateTime.parse(arg0.getText(), FORMATTER);
        }
    }

}
