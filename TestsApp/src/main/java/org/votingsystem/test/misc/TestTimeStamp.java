package org.votingsystem.test.misc;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;


public class TestTimeStamp {


    private static Logger log =  Logger.getLogger(PKCS7SignedData.class.getName());

    private static String forge_TimeStamp = "MD4CADAvMAsGCWCGSAFlAwQCAQQguO2mhtXeBKLHI/AAJjzu4zFJ5vib491ZQgGeS+wrEdwCCTk5MDk3Mjk0Nw==";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        byte[] decodedRequest = Base64.decode(forge_TimeStamp);
        TimeStampRequest timeStampRequest = new TimeStampRequest(new ByteArrayInputStream(decodedRequest));
        log.info(timeStampRequest.getEncoded().toString());
        ResponseVS responseVS = HttpHelper.getInstance().sendData(forge_TimeStamp.getBytes(), ContentType.TIMESTAMP_QUERY,
                ContextVS.getInstance().getProperty("timeStampServerURL") + "/timestamp");
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            log.info("TimeStampToken OK");
        } else throw new ExceptionVS(responseVS.getMessage());
    }
}
