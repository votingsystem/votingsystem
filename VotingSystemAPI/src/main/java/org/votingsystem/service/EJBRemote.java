package org.votingsystem.service;


import org.votingsystem.model.ActorVS;
import org.votingsystem.model.KeyStoreVS;

import java.util.concurrent.Future;

public interface EJBRemote {

    public void generateBackup(Long eventId) throws Exception;
    public byte[] generateUserKeyStore(String givenName, String surname, String nif, char[] password) throws Exception;
    public byte[] generateServerKeyStore(ActorVS.Type type, String givenName, String keyAlias, String nif,
           char[] password, KeyStoreVS keyStoreVS) throws Exception;
    public Future<String> testAsync(String message);
    public String validateCSR(String nif, String deviceId) throws Exception;

}
