package org.votingsystem.services;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface EventBusService {

    public void register(Object eventBusListener);
    public void post(Object eventData);

}
