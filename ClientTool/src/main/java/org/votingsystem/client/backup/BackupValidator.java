package org.votingsystem.client.backup;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface BackupValidator<T>{

    public void cancel();
    public T call() throws Exception;
}
