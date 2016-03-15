package org.votingsystem.service;


import org.votingsystem.model.Actor;
import org.votingsystem.model.KeyStore;

public interface EJBRemoteAdminAccessControl {

    public void generateBackup(Long eventId) throws Exception;
    public byte[] generateUserKeyStore(String givenName, String surname, String nif, char[] password) throws Exception;
    public byte[] generateServerKeyStore(Actor.Type type, String givenName, String keyAlias, String nif,
                                         char[] password, KeyStore keyStore) throws Exception;
    public String validateCSR(String nif, String deviceId) throws Exception;

}
