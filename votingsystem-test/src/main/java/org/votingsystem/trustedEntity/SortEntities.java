package org.votingsystem.trustedEntity;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.BaseTest;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.util.FileUtils;
import org.votingsystem.xades.SignatureGenerator;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SortEntities extends BaseTest {

    private static final Logger log = Logger.getLogger(SortEntities.class.getName());

    public static void main(String[] args) throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, KeyManagementException {
        new SortEntities().sortEntities();;
    }

    public SortEntities() {
        super();
    }

    public void sortEntities() throws IOException {
        byte[] trustedEntitiesBytes = FileUtils.getBytesFromStream(
                Thread.currentThread().getContextClassLoader().getResource("misc/trustedEntities.xml").openStream());
        TrustedEntitiesDto entitiesDto = new XmlMapper().readValue(trustedEntitiesBytes,
                TrustedEntitiesDto.class);
        List<TrustedEntitiesDto.EntityDto> entityList = new ArrayList<>(entitiesDto.getEntities());
        Collections.sort(entityList);
        for(TrustedEntitiesDto.EntityDto entityDto : entityList) {
            log.info("EntityDto: " + entityDto.getType() + " - id: " + entityDto.getId());
        }
    }
}
