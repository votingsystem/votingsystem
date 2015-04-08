package org.votingsystem.service;


import java.util.concurrent.Future;

public interface VotingSystemRemote {

    public void generateBackup(Long eventId) throws Exception;
    public Future<String> testAsync(String message);

}
