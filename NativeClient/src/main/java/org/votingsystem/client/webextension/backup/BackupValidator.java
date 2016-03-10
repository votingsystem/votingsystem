package org.votingsystem.client.webextension.backup;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface BackupValidator<T>{

    public void cancel();
    public T call() throws Exception;
}
