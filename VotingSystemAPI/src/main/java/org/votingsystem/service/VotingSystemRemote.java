package org.votingsystem.service;


public interface VotingSystemRemote {

    public void generateBackup(Long eventId) throws Exception;

}
