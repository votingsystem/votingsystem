package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectedUsersDto {

    private List<String> notAuthenticatedSessions;
    private Map<Long, Set<DeviceVSDto>> usersAuthenticated;

    public ConnectedUsersDto() {}

    public List<String> getNotAuthenticatedSessions() {
        return notAuthenticatedSessions;
    }

    public void setNotAuthenticatedSessions(List<String> notAuthenticatedSessions) {
        this.notAuthenticatedSessions = notAuthenticatedSessions;
    }

    public Map<Long, Set<DeviceVSDto>> getUsersAuthenticated() {
        return usersAuthenticated;
    }

    public void setUsersAuthenticated(Map<Long, Set<DeviceVSDto>> usersAuthenticated) {
        this.usersAuthenticated = usersAuthenticated;
    }

}
