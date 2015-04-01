package org.votingsystem.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertGenerator {
	
    private static Logger log = Logger.getLogger(CertGenerator.class.getSimpleName());

    public static String ROOT_ALIAS = "rootAlias";//
    public static String END_ENTITY_ALIAS = "endEntityAlias";//


    public static long DATE_BEGIN_CERT = System.currentTimeMillis();
    public static final long DURATION_ROOT_KEYSTORE = 20000000000L;
    public static final long DURATION_CERT = 20000000000L;

    private String password;
    private X500PrivateCredential rootPrivateCredential;

    public static void main(String[] args) throws Exception{
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("votingSystemAPI.properties"), null);

        String testStr = "{\"rootCertFile\":\"/home/jgzornoza/github/votingsystem/templates/appsRootCert.jks\"," +
                "\"rootSubjectDN\":\"CN=Voting System Certificate Authority, OU=Certs\",\"password\":\"PemPass\"," +
                "\"certs\":[{\"file\":\"/home/jgzornoza/github/votingsystem/AccessControl/web-app/WEB-INF/votingsystem/AccessControl.jks\"," +
                "\"distinguishedName\":\"CN=Voting System Access Control, SERIALNUMBER=50000000R\",\"alias\":\"AccessControlKeys\"," +
                "\"isTimeStampingCert\":false},{\"file\":\"/home/jgzornoza/github/votingsystem/ControlCenter/web-app/WEB-INF/votingsystem/ControlCenter.jks\"," +
                "\"distinguishedName\":\"CN=Voting System Control Center, SERIALNUMBER=50000001W\",\"alias\":\"ControlCenterKeys\",\"isTimeStampingCert\":false}," +
                "{\"file\":\"/home/jgzornoza/github/votingsystem/TimeStampServer/web-app/WEB-INF/votingsystem/TimeStampServer.jks\"," +
                "\"distinguishedName\":\"CN=Voting System TimeStamp Server, SERIALNUMBER=80000000C\",\"alias\":\"TimeStampServerKeys\"," +
                "\"isTimeStampingCert\":true},{\"file\":\"/home/jgzornoza/github/votingsystem/CurrencyServer/web-app/WEB-INF/votingsystem/CurrencyServer.jks\"," +
                "\"distinguishedName\":\"CN=Voting System Currency Server, SERIALNUMBER=90000000B\",\"alias\":\"CurrencyServerKeys\",\"isTimeStampingCert\":true}]}";
        generate(new CertRequest(testStr));
        //generate(new CertRequest(args[0]));
    }

    public CertGenerator(File rootCertFile, String rootSubjectDN, String password) throws Exception {
        if(ContextVS.getInstance() == null) {
            ContextVS.getInstance().initTestEnvironment(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("votingSystemAPI.properties"), null);
        }
        log.info(" - rootSubjectDN: " + rootSubjectDN + " - password: " + password +
                " - rootCertFile.getAbsolutePath(): " + rootCertFile.getAbsolutePath()); 
        this.password = password;
        KeyStore rootKeyStore = KeyStoreUtil.createRootKeyStore(DATE_BEGIN_CERT, DURATION_ROOT_KEYSTORE,
                password.toCharArray(), ROOT_ALIAS, rootSubjectDN);
        rootKeyStore.store(new FileOutputStream(rootCertFile),  password.toCharArray());
        X509Certificate rootCertificate = (X509Certificate)rootKeyStore.getCertificate(ROOT_ALIAS);
        PrivateKey rootPK = (PrivateKey) rootKeyStore.getKey(ROOT_ALIAS, password.toCharArray());
        rootPrivateCredential =	new X500PrivateCredential(rootCertificate, rootPK, ROOT_ALIAS);
    }

    public void genUserKeyStore(String subjectDN, File file, String alias) throws Exception {
        log.info("genUserKeyStore - subjectDN: " + subjectDN +
                        " - file: " + file.getAbsolutePath() + " - alias: " + alias);
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(DATE_BEGIN_CERT,
                        DURATION_CERT, password.toCharArray(), alias, rootPrivateCredential, subjectDN);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
    }

    public void genTimeStampingKeyStore(
        String subjectDN, File file, String alias) throws Exception {
        log.info("genTimeStampingKeyStore - subjectDN: " + subjectDN +
                " - file: " + file.getAbsolutePath() + " - alias: " + alias);
        KeyStore keyStore = KeyStoreUtil.createTimeStampingKeyStore(
                DATE_BEGIN_CERT, DURATION_CERT, password.toCharArray(), alias, rootPrivateCredential, subjectDN);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
    }
	
    public static void generate (CertRequest request) throws Exception {
        log.info("generate");
        if(request.certList.isEmpty()) throw new ExceptionVS("Request with empty cert list");
        CertGenerator cerGenerator = new CertGenerator(request.getRootCertFile(), request.rootSubjectDN, request.passwd);
        for(CertRequest certRequest: request.certList) {
            if(certRequest.isTimeStampingCert) cerGenerator.genTimeStampingKeyStore(certRequest.distinguishedName,
                    certRequest.certFile, certRequest.alias);
            else cerGenerator.genUserKeyStore(certRequest.distinguishedName, certRequest.certFile, certRequest.alias);
        }
    }

    private static class CertRequest {
        String rootCertFile, rootSubjectDN, passwd, distinguishedName, alias;
        List<CertRequest> certList = new ArrayList<>();
        File certFile;
        Boolean isTimeStampingCert;

        CertRequest() {}

        CertRequest(String dataMapStr) throws ExceptionVS, IOException {
            Map<String, Object> dataMap = new ObjectMapper().readValue(dataMapStr, new TypeReference<HashMap<String, Object>>() {});
            if(!dataMap.containsKey("rootCertFile")) throw new ExceptionVS("Missing arg 'rootCertFile'");
            rootCertFile = (String) dataMap.get("rootCertFile");
            if(!dataMap.containsKey("rootSubjectDN")) throw new ExceptionVS("Missing arg 'rootSubjectDN'");
            rootSubjectDN = (String) dataMap.get("rootSubjectDN");
            if(!dataMap.containsKey("rootSubjectDN")) throw new ExceptionVS("Missing arg 'password'");
            passwd = (String) dataMap.get("password");
            if(dataMap.containsKey("certs")) {
                for(Map certDataMap : (List<Map>) dataMap.get("certs")) {
                    if(certDataMap.get("file") == null) throw new ExceptionVS(
                            "Missing arg 'file' from cert request in certs");
                    CertRequest request = new CertRequest();
                    request.certFile = new File((String) certDataMap.get("file"));
                    request.certFile.getParentFile().mkdirs();
                    request.certFile.createNewFile();
                    if(certDataMap.get("distinguishedName") == null) throw new ExceptionVS(
                            "Missing arg 'distinguishedName' from cert request in certs");
                    request.distinguishedName = (String) certDataMap.get("distinguishedName");
                    if(certDataMap.get("alias") == null) throw new ExceptionVS(
                            "Missing arg 'alias' from cert request in certs");
                    request.alias = (String) certDataMap.get("alias");
                    if(certDataMap.get("isTimeStampingCert") == null) throw new ExceptionVS(
                            "Missing arg 'isTimeStampingCert' from cert request in certs");
                    request.isTimeStampingCert = (Boolean) certDataMap.get("isTimeStampingCert");
                    certList.add(request);
                }
            }
        }
        File getRootCertFile() {
            return new File(rootCertFile);
        }
    }
}
