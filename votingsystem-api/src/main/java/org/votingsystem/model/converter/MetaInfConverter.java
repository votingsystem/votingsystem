package org.votingsystem.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author votingsystem
 */
@Converter
public class MetaInfConverter implements AttributeConverter<MetaInfDto, String> {

    private static Logger log = Logger.getLogger(MetaInfConverter.class.getName());


    @Override
    public String convertToDatabaseColumn(MetaInfDto metaInf) {
        if(metaInf != null) {
            try {
                return new XmlMapper().writeValueAsString(metaInf);
            } catch (JsonProcessingException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return null;
    }

    @Override
    public MetaInfDto convertToEntityAttribute(String s) {
        if(!StringUtils.isEmpty(s)) {
            try {
                return new XmlMapper().readValue(s, MetaInfDto.class);
            } catch (IOException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return null;
    }
}
