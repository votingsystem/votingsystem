package org.votingsystem.service;


import org.votingsystem.model.ActorVS;

import java.util.concurrent.Future;

public interface EJBRemote {

    public void generateBackup(Long eventId) throws Exception;
    public byte[] generateKeyStore(ActorVS.Type type, String givenName, String surname, String nif,
           char[] password) throws Exception;
    public Future<String> testAsync(String message);
    public String validateCSR(String nif, String deviceId) throws Exception;

}
