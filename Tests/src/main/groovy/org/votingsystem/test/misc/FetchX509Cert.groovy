package org.votingsystem.test.misc

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.test.util.TestUtils
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.HttpHelper

import java.security.cert.X509Certificate

Logger log = TestUtils.init(FetchX509Cert.class)

String serverURL = "http://cooins/TimeStampServer"
String serverInfoURL = ActorVS.getServerInfoURL(serverURL)
ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);

if(ResponseVS.SC_OK != responseVS.getStatusCode())
    throw new ExceptionVS("$serverInfoURL - error: " + responseVS.getMessage())

ActorVS actorVS = ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
if(serverURL.equals(actorVS.getServerURL())) {
    Collection<X509Certificate> certCollection = CertUtils.fromPEMToX509CertCollection(
            actorVS.certChainPEM.getBytes())
    for(X509Certificate cert: certCollection) {
        log.debug("cert ${cert.subjectDN} - not valid after ${cert.notAfter}")
    }
    x509TimeStampServerCert = certCollection.iterator().next()
    log.debug("x509TimeStampServerCert ${x509TimeStampServerCert.subjectDN} - not valid after ${x509TimeStampServerCert.notAfter}")
    if(Calendar.getInstance().getTime().after(x509TimeStampServerCert.notAfter)) {
        log.error("$serverInfoURL signing cert is lapsed - " +
                " cert not valid after: ${x509TimeStampServerCert.notAfter.toString()}")
        throw new ExceptionVS("$serverInfoURL signing cert is lapsed")
    }
} else throw new ExceptionVS("Expected server URL '$serverURL' found " + "'${actorVS.getServerURL()}'")

System.exit(0)