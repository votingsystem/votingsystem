package org.votingsystem;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Constants {

    public static final String TIMESTAMP_SERVICE_URL = "https://192.168.1.5/timestamp-server/api/timestamp";
    public static final String TIMESTAMP_SERVICE_DISCRETE_URL = "https://192.168.1.5/timestamp-server/api/timestamp/discrete";

    public static final String ID_PROVIDER_SERVICE_ENTITY_ID = "https://192.168.1.5/idprovider";
    public static final String VOTING_SERVICE_SERVICE_ENTITY_ID = "https://192.168.1.5/voting-service";

    public static final String OCSP_SERVER_URL = ID_PROVIDER_SERVICE_ENTITY_ID + "/ocsp";

    public static String ADMIN_KEYSTORE = "certs/votingsystem-idprovider.jks";
    public static String ADMIN_KEYSTORE_PASSWORD = org.votingsystem.util.Constants.PASSW_DEMO;

}
