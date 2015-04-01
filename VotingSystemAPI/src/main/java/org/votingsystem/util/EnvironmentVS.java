package org.votingsystem.util;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum EnvironmentVS {
    DEVELOPMENT, TEST, PRODUCTION;

    public static EnvironmentVS getMode(boolean productionMode) {
        return productionMode? EnvironmentVS.PRODUCTION:EnvironmentVS.DEVELOPMENT;
    }
}
