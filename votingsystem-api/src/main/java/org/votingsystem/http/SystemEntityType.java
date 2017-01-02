package org.votingsystem.http;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum SystemEntityType {

    @JsonProperty("voting-service-provider")
    VOTING_SERVICE_PROVIDER("voting-service-provider"),
    @JsonProperty("voting-idprovider")
    VOTING_ID_PROVIDER("voting-idprovider"),
    @JsonProperty("timestamp-server")
    TIMESTAMP_SERVER("timestamp-server"),
    @JsonProperty("anonymizer")
    ANONYMIZER("anonymizer");

    private String name;

    SystemEntityType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SystemEntityType getByName(String name) {
        for(SystemEntityType systemEntityType: SystemEntityType.values()) {
            if(systemEntityType.getName().equals(name))
                return systemEntityType;
        }
        throw new RuntimeException("There's not SystemEntityType with name: " + name);
    }

}
