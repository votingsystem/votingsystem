package org.votingsystem.test.misc;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FetchX509Cert {

    private static Logger log =  Logger.getLogger(FetchX509Cert.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        String serverURL = "http://localhost:8080/TimeStampServer";
        String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
        ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode())
            throw new ExceptionVS("serverInfoURL - error: " + responseVS.getMessage());
        ActorVS actorVS = ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
        if(serverURL.equals(actorVS.getServerURL())) {
            Collection<X509Certificate> certCollection = CertUtils.fromPEMToX509CertCollection(
                    actorVS.getCertChainPEM().getBytes());
            for(X509Certificate cert: certCollection) {
                log.info(format("cert {0} - not valid after {1}", cert.getSubjectDN(), cert.getNotAfter()));
            }
            X509Certificate x509TimeStampServerCert = certCollection.iterator().next();
            log.info("subjectDN " + x509TimeStampServerCert.getSubjectDN().toString() + " - not valid after:" +
                    x509TimeStampServerCert.getNotAfter());
            if(new Date().after(x509TimeStampServerCert.getNotAfter())) {
                log.log(Level.SEVERE, format("{0} signing cert is lapsed - cert not valid after: {1}",
                        serverInfoURL, x509TimeStampServerCert.getNotAfter()));
                throw new ExceptionVS(serverInfoURL + " signing cert is lapsed");
            }
        } else throw new ExceptionVS(format("Expected server URL {0} found {1}", serverURL, actorVS.getServerURL()));
        System.exit(0);
    }
}

