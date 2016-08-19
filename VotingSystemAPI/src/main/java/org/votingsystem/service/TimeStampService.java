package org.votingsystem.service;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.crypto.TimeStampResponseGeneratorHelper;
import org.votingsystem.util.crypto.TimeStampTokenGeneratorHelper;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface TimeStampService {

    public byte[] getSigningCertPEMBytes();

    public TimeStampTokenGeneratorHelper getResponseGenerator(InputStream inputStream) throws Exception;

    public TimeStampResponseGeneratorHelper getXAdESResponseGenerator(InputStream inputStream) throws Exception;

    public TimeStampTokenGeneratorHelper getResponseGeneratorDiscrete(InputStream inputStream) throws Exception;

    public byte[] getSigningCertChainPEMBytes();

    public void validateToken(TimeStampToken timeStampToken) throws TSPException;

    public byte[] getTimeStampRequest(byte[] digest) throws IOException;

    public byte[] getTimeStampResponse(InputStream inputStream) throws OperatorCreationException,
            CertificateEncodingException, ExceptionVS, TSPException, IOException;

}