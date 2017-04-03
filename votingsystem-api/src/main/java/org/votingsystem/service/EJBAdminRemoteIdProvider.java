package org.votingsystem.service;

import org.votingsystem.model.User;

public interface EJBAdminRemoteIdProvider {

    public void generateBackup(String electionUUID) throws Exception;
    public byte[] generateUserKeyStore(String givenName, String surname, String nif, char[] password) throws Exception;
    public byte[] generateSystemEntityKeyStore(User.Type userType, String givenName, String keyAlias,
                                               char[] password) throws Exception;
    public String validateCSR(String nif, String deviceId) throws Exception;

}
