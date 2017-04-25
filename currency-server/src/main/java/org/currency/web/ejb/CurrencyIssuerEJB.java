package org.currency.web.ejb;

import eu.europa.esig.dss.x509.CertificateToken;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.currency.web.util.AuditLogger;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.ocsp.RootCertOCSPInfo;
import org.votingsystem.throwable.CertificateRequestException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Interval;
import org.votingsystem.util.Messages;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class CurrencyIssuerEJB {

    private static Logger log = Logger.getLogger(CurrencyIssuerEJB.class.getName());

    private BigDecimal currencyMinValue = BigDecimal.ONE;

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private SignatureService signatureService;

    private X509Certificate certIssuerSigningCert;
    private PrivateKey certIssuerPrivateKey;
    private RootCertOCSPInfo rootCertOCSPInfo;
    private Certificate authorityCertificate;

    @PostConstruct
    public void initialize() {
        try {
            Properties properties = new Properties();
            File propertiesFile = new File(config.getApplicationDirPath() + "/sec/keystore.properties");
            properties.load(new FileInputStream(propertiesFile));

            String issuerKeyStoreFileName = (String) properties.get("issuerKeyStoreFileName");
            String issuerKeyPassword = (String) properties.get("issuerKeyPassword");

            KeyStore keyStoreCertIssuer = KeyStore.getInstance("JKS");
            keyStoreCertIssuer.load(new FileInputStream(config.getApplicationDirPath() + "/sec/" + issuerKeyStoreFileName),
                    issuerKeyPassword.toCharArray());
            String keyAlias = keyStoreCertIssuer.aliases().nextElement();
            certIssuerSigningCert = (X509Certificate) keyStoreCertIssuer.getCertificate(keyAlias);
            certIssuerPrivateKey = (PrivateKey) keyStoreCertIssuer.getKey(keyAlias, issuerKeyPassword.toCharArray());

            authorityCertificate = config.loadAuthorityCertificate(new CertificateToken(certIssuerSigningCert));
            rootCertOCSPInfo = new RootCertOCSPInfo(authorityCertificate, certIssuerSigningCert, certIssuerPrivateKey);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public Set<String> signCurrencyRequest (CurrencyRequestDto requestDto) throws CertificateRequestException {
        Interval timePeriod = null;
        ZonedDateTime dateFrom = ZonedDateTime.now().withNano(0).withSecond(0).withMinute(0).withHour(0).withDayOfYear(1);
        ZonedDateTime dateTo = dateFrom.plus(Constants.CURRENY_ISSUED_LIVE_IN_YEARS, ChronoUnit.YEARS);
        timePeriod = new Interval(dateFrom, dateTo);
        Set<Currency> issuedCurrencySet = new HashSet<>();
        Set<String> issuedCertSet = new HashSet<>();
        try {
            for(CurrencyDto currencyDto : requestDto.getCurrencyDtoMap().values()) {
                X509Certificate issuedCert = CertificateUtils.signCSR(currencyDto.getCsrPKCS10(), null, certIssuerPrivateKey,
                        certIssuerSigningCert, timePeriod.getDateFrom().toLocalDateTime(),
                        timePeriod.getDateTo().toLocalDateTime(), config.getOcspServerURL());
                Currency currency = Currency.FROM_CERT(issuedCert, authorityCertificate);
                currency.setType(Currency.Type.REQUEST);
                em.persist(currency);
                issuedCurrencySet.add(currency);
                issuedCertSet.add(new String(PEMUtils.getPEMEncoded(issuedCert)));
                AuditLogger.logCurrencyIssued(currency);
            }
            return issuedCertSet;
        } catch(Exception ex) {
            for(Currency currency : issuedCurrencySet) {
                if(currency.getId() != null) {
                    currency.setState(Currency.State.ERROR).setReason(ex.getMessage());
                }
            }
            throw new CertificateRequestException("currency request error: " + ex.getMessage());
        }
    }

    public Currency signCurrencyRequest(PKCS10CertificationRequest csrReq, Currency.Type type,
                CurrencyBatch currencyBatch, int currencyIssuedLiveInYears) throws CertificateRequestException {
        CurrencyCertExtensionDto certExtensionDto = null;
        try {
            certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class, csrReq, Constants.CURRENCY_OID);
        } catch (Exception ex) {
            throw new CertificateRequestException(ex.getMessage(), ex);
        }
        if(currencyMinValue.compareTo(certExtensionDto.getAmount()) > 0)
                throw new CertificateRequestException(Messages.currentInstance().get("currencyMinValueError",
                currencyMinValue.toString(), certExtensionDto.getAmount().toString()));
        ZonedDateTime dateFrom = ZonedDateTime.now().withNano(0).withSecond(0).withMinute(0).withHour(0).withDayOfYear(1);
        ZonedDateTime dateTo = dateFrom.plus(currencyIssuedLiveInYears, ChronoUnit.YEARS);
        Interval timePeriod = new Interval(dateFrom, dateTo);
        try {
            X509Certificate issuedCert = CertificateUtils.signCSR(csrReq, null, certIssuerPrivateKey,
                    certIssuerSigningCert, timePeriod.getDateFrom().toLocalDateTime(),
                    timePeriod.getDateTo().toLocalDateTime(), config.getOcspServerURL());
            Currency currency = Currency.FROM_CERT(issuedCert,authorityCertificate).setType(type).setCurrencyBatch(currencyBatch);
            em.persist(currency);
            AuditLogger.logCurrencyIssued(currency);
            return currency;
        } catch(Exception ex) {
            throw new CertificateRequestException(Messages.currentInstance().get("currencyRequestDataError"));
        }
    }

    public RootCertOCSPInfo getRootCertOCSPInfo() {
        return rootCertOCSPInfo;
    }

}