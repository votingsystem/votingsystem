package org.votingsystem.test;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Constants {

    public static final String TIMESTAMP_SERVICE_URL = "https://voting.ddns.net/timestamp-server/api/timestamp";
    public static final String TIMESTAMP_SERVICE_DISCRETE_URL = "https://voting.ddns.net/timestamp-server/api/timestamp/discrete";

    public static final String ID_PROVIDER_ENTITY_ID = "https://voting.ddns.net/idprovider";
    public static final String VOTING_SERVICE_ENTITY_ID = "https://voting.ddns.net/voting-service";
    public static final String CURRENCY_SERVICE_ENTITY_ID = "https://voting.ddns.net:8443/currency-server";

    public static final String OCSP_SERVER_URL = ID_PROVIDER_ENTITY_ID + "/ocsp";

    public static String ADMIN_KEYSTORE = "certs/votingsystem-idprovider.jks";
    public static String ADMIN_KEYSTORE_PASSWORD = org.votingsystem.util.Constants.PASSW_DEMO;

    public static final String PASSW_DEMO      = org.votingsystem.util.Constants.PASSW_DEMO;
}
