package org.votingsystem.serviceprovider.cdi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.jsf.ServiceUpdatedMessage;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;

import javax.ejb.EJB;
import javax.enterprise.event.Observes;
import javax.faces.bean.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to hold variables and methods needed by some web pages
 */
@Named("pageBean")
@ApplicationScoped
public class PageBean implements Serializable {

    private static final Logger log = Logger.getLogger(PageBean.class.getName());

    @EJB Config config;
    @EJB TrustedServicesEJB trustedServices;
    @Inject @Push
    private PushContext serviceUpdated;

    public void serviceUpdated(@Observes ServiceUpdatedMessage serviceUpdatedMessage) {
        log.log(Level.INFO, "uuid: " + serviceUpdatedMessage.getClientUUID());
        try {
            serviceUpdated.send(JSON.getMapper().writeValueAsString(serviceUpdatedMessage.getResponse()),
                    serviceUpdatedMessage.getClientUUID());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Method that format a Date with the pattern yyyy-MM-dd HH:mm:ss
     *
     * @param date
     * @return the formatted date
     */
    public String formatDate(Date date) {
        if(date == null) return "";
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    public String formatDate(LocalDateTime date) {
        if(date == null) return "";
        return DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss zzz").format(ZonedDateTime.of(date, ZoneId.systemDefault()));
    }

    public String formatDate(ZonedDateTime date) {
        if(date == null) return "";
        return DateUtils.getDateStr(date);
    }

    public String applicationCodeMsg(String applicationCodeStr) {
        return Messages.currentInstance().get(applicationCodeStr);
    }

    public String locale() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return request.getLocale().toString();
    }

    public String now() {
        return formatDate(new Date());
    }

    public String getVotingServicesCountryMapAsJSON() throws JsonProcessingException {
        Set<MetadataDto> votingServices = trustedServices.getEntitySetByType(SystemEntityType.VOTING_SERVICE_PROVIDER);
        return getCountryMapAsJSON(votingServices);
    }

    public String getIdProvidersCountryMapAsJSON() throws JsonProcessingException {
        Set<MetadataDto> idProviders = trustedServices.getEntitySetByType(SystemEntityType.ID_PROVIDER);
        return getCountryMapAsJSON(idProviders);
    }

    private String getCountryMapAsJSON(Set<MetadataDto> entities) throws JsonProcessingException {
        Map<String, String> resultMap = new HashMap<>();
        for(MetadataDto metadataDto : entities) {
            String countryCode = metadataDto.getEntity().getLocation().getCountry().getCode();
            resultMap.put(countryCode, new Locale("", countryCode).getDisplayCountry());
        }
        return JSON.getMapper().writeValueAsString(resultMap);
    }

}