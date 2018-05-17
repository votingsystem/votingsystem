package org.votingsystem.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private TrustedEntitiesDto trustedEntities;

    private static final Map<String, MetadataDto> trustedEntitiesMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        try {
            trustedEntities = TrustedEntitiesDto.loadTrustedEntities(config.getApplicationDirPath() + "/sec/trusted-entities.xml");
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Method that checks all trusted services every hour -> **:00:00
     */
    //@Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void loadTrustedServices() throws IOException {
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
        for(TrustedEntitiesDto.EntityDto trustedEntity: trustedEntities.getEntities()) {
            log.info(trustedEntity.getId() + " - " + trustedEntity.getType() + " from country: " + trustedEntity.getCountryCode());
            try {
                MetadataDto metadataDto = metadataResource.getMetadataFromURL(OperationType.GET_METADATA.getUrl(
                        trustedEntity.getId()), true, false);
                trustedEntitiesMap.put(trustedEntity.getId(), metadataDto);
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Error loading trusted entity: " + trustedEntity.getId() + " - " + ex.getMessage(), ex);
            }
        }
    }

    public Set<String> getLoadedEntities () {
        return trustedEntitiesMap.keySet();
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

    public MetadataDto getEntityMetadata(String entityId) {
        MetadataDto metadata = trustedEntitiesMap.get(entityId);
        if(metadata != null) {
            if (metadata.getValidUntilDate().isAfter(LocalDateTime.now())) {
                log.info("metadata expired - entityId: " + entityId);
                trustedEntitiesMap.remove(entityId);
                return null;
            }
        }
        return metadata;
    }

    public MetadataDto getFirstEntity(SystemEntityType entityType) {
        for(MetadataDto metadata : trustedEntitiesMap.values()) {
            if(entityType == metadata.getEntity().getEntityType())
                return metadata;
        }
        return null;
    }

    public TrustedEntitiesDto getTrustedEntities() {
        return trustedEntities;
    }

    public MetadataDto checkEntity(String entityId) throws IOException {
        MetadataDto result = trustedEntitiesMap.get(entityId);
        if(result == null) {
            for(TrustedEntitiesDto.EntityDto entityDto : trustedEntities.getEntities()) {
                if(entityDto.getId().equals(entityId)) {
                    loadTrustedServices();
                    return trustedEntitiesMap.get(entityId);
                }
            }
            return null;
        } else
            return result;
    }

}