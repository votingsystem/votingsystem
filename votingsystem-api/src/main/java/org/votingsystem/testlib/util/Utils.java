package org.votingsystem.testlib.util;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.util.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    public static List<TrustedEntitiesDto.EntityDto> sortEntities() throws IOException {
        byte[] trustedEntitiesBytes = FileUtils.getBytesFromStream(
                Thread.currentThread().getContextClassLoader().getResource("misc/trustedEntities.xml").openStream());
        TrustedEntitiesDto entitiesDto = new XmlMapper().readValue(trustedEntitiesBytes,
                TrustedEntitiesDto.class);
        List<TrustedEntitiesDto.EntityDto> entityList = new ArrayList<>(entitiesDto.getEntities());
        Collections.sort(entityList);
        for(TrustedEntitiesDto.EntityDto entityDto : entityList) {
            log.info("EntityDto: " + entityDto.getType() + " - id: " + entityDto.getId());
        }
        return entityList;
    }

}
