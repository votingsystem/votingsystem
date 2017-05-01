package org.votingsystem.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.metadata.KeyDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.model.User;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@Lock(LockType.READ)
public class TrustedServicesEJB {

    private static final Logger log = Logger.getLogger(TrustedServicesEJB.class.getName());

    @Inject private Config config;
    @Inject private MetadataService metadataResource;
    @Inject private SignerInfoService signerInfoService;

    private static Map<String, MetadataDto> trustedEntitiesMap = new ConcurrentHashMap<>();

    /* Executor service for asynchronous processing */
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    private TrustedEntitiesDto trustedEntities;


    /**
     * Method that checks all trusted services every hour -> **:00:00
     */
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public Set<String> loadTrustedServices() throws IOException {
        File timeStampServersFile = new File(config.getApplicationDirPath() + "/sec/timestamp-servers.xml");
        List<String> timeStampServersCertificates = XML.getMapper().readValue(
                timeStampServersFile, new TypeReference<List<String>>() {});
        for(String timeStampServerCertificate : timeStampServersCertificates) {
            try {
                config.addTrustedTimeStampIssuer(PEMUtils.fromPEMToX509Cert(timeStampServerCertificate.trim().getBytes()));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        trustedEntities = TrustedEntitiesDto.loadTrustedEntities(config.getApplicationDirPath() + "/sec/trusted-entities.xml");
        if(trustedEntities.getEntities() != null) {
            List<TrustedEntitiesDto.EntityDto> entityList = new ArrayList<>(trustedEntities.getEntities());
            Collections.sort(entityList);
            for(TrustedEntitiesDto.EntityDto trustedEntity: entityList) {
                log.info("trusted entity: " + trustedEntity.getType() + " from country: " + trustedEntity.getCountryCode());
                try {
                    MetadataDto metadataDto = null;
                    switch(trustedEntity.getType()) {
                        case TIMESTAMP_SERVER:
                            metadataDto = metadataResource.getMetadataFromURL(
                                    OperationType.GET_METADATA.getUrl(trustedEntity.getId()), false, false);
                            checkTimeStampServer(metadataDto);
                        default:
                            metadataDto = metadataResource.getMetadataFromURL(
                                    OperationType.GET_METADATA.getUrl(trustedEntity.getId()), true, false);
                            break;
                    }
                    trustedEntitiesMap.put(metadataDto.getEntity().getId(), metadataDto);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Error loading trusted entity: " + trustedEntity.getId() + " - " + ex.getMessage(), ex);
                }
            }
        }
        return trustedEntitiesMap.keySet();
    }

    private void checkTimeStampServer(MetadataDto metadataDto) {
        log.info("Entity id: " + metadataDto.getEntity().getId());
        for(KeyDto keyDto : metadataDto.getKeyDescriptorSet()) {
            try {
                if(KeyDto.Use.SIGN == keyDto.getUse()) {
                    User user = signerInfoService.checkSigner(keyDto.getX509Certificate(), User.Type.TIMESTAMP_SERVER,
                            metadataDto.getEntity().getId());
                    config.addTrustedTimeStampIssuer(keyDto.getX509Certificate());
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public TrustedEntitiesDto getTrustedEntities() {
        return trustedEntities;
    }

    public MetadataDto getEntity(String entityId) {
        return trustedEntitiesMap.get(entityId);
    }

    public Set<MetadataDto> getEntitySetByType(SystemEntityType entityType) {
        Set<MetadataDto> result = new HashSet<>();
        for(MetadataDto metadata : trustedEntitiesMap.values()) {
            if(metadata.getEntity().getEntityType() == entityType)
                result.add(metadata);
        }
        return result;
    }

    public Set<MetadataDto> getEntitySetByTypeAndCountryCode(SystemEntityType entityType, String countryCode) {
        Set<MetadataDto> result = new HashSet<>();
        for(MetadataDto metadata : trustedEntitiesMap.values()) {
            if(metadata.getEntity().getEntityType() == entityType && metadata.getEntity().getLocation()
                    .getCountry().getCode().equals(countryCode)) {
                result.add(metadata);
            }
        }
        return result;
    }
}