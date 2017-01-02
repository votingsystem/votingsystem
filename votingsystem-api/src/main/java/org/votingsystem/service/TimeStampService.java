package org.votingsystem.service;

import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.crypto.TimeStampResponseGeneratorHelper;

import java.io.IOException;
import java.io.InputStream;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface TimeStampService {

    public byte[] getSigningCertPEMBytes();

    public TimeStampResponseGeneratorHelper getResponseGenerator(InputStream inputStream) throws Exception;

    public TimeStampResponseGeneratorHelper getResponseGeneratorDiscrete(InputStream inputStream) throws Exception;

    public byte[] getSigningCertChainPEMBytes();

    public void validateToken(TimeStampToken timeStampToken) throws TSPException;

    public byte[] getTimeStampRequest(byte[] digest) throws IOException;

    public byte[] getTimeStampResponse(InputStream inputStream) throws Exception;

}