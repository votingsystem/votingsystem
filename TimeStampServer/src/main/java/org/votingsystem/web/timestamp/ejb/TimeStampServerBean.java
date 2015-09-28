package org.votingsystem.web.timestamp.ejb;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.service.TimeStampService;
import org.votingsystem.service.impl.TimeStampServiceImpl;
import org.votingsystem.signature.util.TimeStampResponseGenerator;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.web.util.ConfigVS;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Named(value="timeStampService")
public class TimeStampServerBean implements TimeStampService {

    private static final Logger log = Logger.getLogger(TimeStampServerBean.class.getSimpleName());

    private TimeStampServiceImpl timeStampService = null;
    @Inject ConfigVS config;

    public TimeStampServerBean() { }

    @PostConstruct
    public void initialize() {
        log.info("initialize");
        URL res = Thread.currentThread().getContextClassLoader().getResource("TimeStampServer.jks");
        try {
            byte[] keyStoreBytes = FileUtils.getBytesFromStream(res.openStream());
            timeStampService =  new TimeStampServiceImpl(keyStoreBytes, config.getProperty("vs.signKeyAlias"),
                    config.getProperty("vs.signKeyPassword"));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] getSigningCertPEMBytes() {
        return timeStampService.getSigningCertPEMBytes();
    }

    @Override
    public TimeStampResponseGenerator getResponseGenerator(InputStream inputStream) throws Exception {
        return timeStampService.getResponseGenerator(inputStream);
    }

    @Override
    public TimeStampResponseGenerator getResponseGeneratorDiscrete(InputStream inputStream) throws Exception {
        return timeStampService.getResponseGeneratorDiscrete(inputStream);
    }

    @Override
    public byte[] getSigningCertChainPEMBytes() {
        return timeStampService.getSigningCertChainPEMBytes();
    }

    @Override
    public void validateToken(TimeStampToken timeStampToken) throws TSPException {
        timeStampService.validateToken(timeStampToken);
    }

    @Override
    public byte[] getTimeStampRequest(byte[] digest) throws IOException {
        return timeStampService.getTimeStampRequest(digest);
    }

    @Override
    public byte[] getTimeStampResponse(InputStream inputStream) throws OperatorCreationException,
            CertificateEncodingException, ExceptionVS, TSPException, IOException {
        return timeStampService.getTimeStampResponse(inputStream);
    }
}
