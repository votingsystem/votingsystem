package org.votingsystem.util;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import net.sf.json.JSONSerializer;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CertGenerator {
	
    private static Logger logger = Logger.getLogger(CertGenerator.class);

    public static String ROOT_ALIAS = "rootAlias";//
    public static String END_ENTITY_ALIAS = "endEntityAlias";//


    public static long COMIEZO_VALIDEZ_CERT = System.currentTimeMillis();//
    public static final long PERIODO_VALIDEZ_ALMACEN_RAIZ = 20000000000L;//En producción durará lo que dure una votación
    public static final long PERIODO_VALIDEZ_CERT = 20000000000L;

    private String password;
    private X500PrivateCredential rootPrivateCredential;

    public static void main(String[] args) throws Exception{
        ContextVS.init(null, "VotingSystemLibrary_log4j.properties", "VotingSystemLibraryMessages_", "es");
        if(args == null || args.length == 0) {
            Map certMap = new HashMap();
            certMap.put("rootCertFile", "./webAppsRootCert.jks");
            certMap.put("rootSubjectDN", "CN=rootSUbjectDN");
            certMap.put("password", "password");      
            generate(certMap);
        } else {
            //logger.debug("args[0]: " + args[0]);
            JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(args[0]);
            generate(jsonData);
            /*File rootCertFile = new File(jsonData.getString("rootCertFile"));
            CertGenerator certGenerator = new CertGenerator(rootCertFile, jsonData.getString("rootSubjectDN"),
                    jsonData.getString("password"));
            List<Map> certList = (List<Map>)jsonData.get("certs");
            for(Map certDataMap : certList) generate(certDataMap);*/
        }
    }

    public CertGenerator(File rootCertFile, String rootSubjectDN, String password) throws Exception {
        if(ContextVS.getInstance() == null)
            ContextVS.init(null, "VotingSystemLibrary_log4j.properties", 
                    "VotingSystemLibraryMessages_", "es");
        logger.debug(" - rootSubjectDN: " + rootSubjectDN + " - password: " + password + 
                " - rootCertFile.getAbsolutePath(): " + rootCertFile.getAbsolutePath()); 
        this.password = password;
        KeyStore rootKeyStore = KeyStoreUtil.createRootKeyStore(COMIEZO_VALIDEZ_CERT, PERIODO_VALIDEZ_ALMACEN_RAIZ,
                password.toCharArray(), ROOT_ALIAS, rootSubjectDN);
        rootKeyStore.store(new FileOutputStream(rootCertFile),  password.toCharArray());
        X509Certificate rootCertificate = (X509Certificate)rootKeyStore.getCertificate(ROOT_ALIAS);
        PrivateKey rootPK = (PrivateKey) rootKeyStore.getKey(ROOT_ALIAS, password.toCharArray());
        rootPrivateCredential =	new X500PrivateCredential(rootCertificate, rootPK, ROOT_ALIAS);
    }

    public void genUserKeyStore(String subjectDN, File file, String alias) throws Exception {
        logger.debug("--- genUserKeyStore - subjectDN: " + subjectDN + 
                        " - file: " + file.getAbsolutePath() + " - alias: " + alias);
        KeyStore keyStore = KeyStoreUtil.createUserKeyStore(COMIEZO_VALIDEZ_CERT,
                        PERIODO_VALIDEZ_CERT, password.toCharArray(),
                        alias, rootPrivateCredential, subjectDN);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
    }

    public void genTimeStampingKeyStore(
        String subjectDN, File file, String alias) throws Exception {
        logger.debug("--- genTimeStampingKeyStore - subjectDN: " + subjectDN + 
                " - file: " + file.getAbsolutePath() + " - alias: " + alias);
        KeyStore keyStore = KeyStoreUtil.createTimeStampingKeyStore(
                COMIEZO_VALIDEZ_CERT, PERIODO_VALIDEZ_CERT, 
                password.toCharArray(), alias, rootPrivateCredential, subjectDN);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(
                keyStore, password.toCharArray());
        FileUtils.copyStreamToFile(
                new ByteArrayInputStream(keyStoreBytes),file);
    }
	
    public static void generate (Map certsMap) throws Exception {
        logger.debug("- generate -");
        if(certsMap == null) {
            logger.error("NULL Map");
            return;
        } 
        File rootCertFile = null;
        String rootSubjectDN = null;
        String password = null;
        if (certsMap.get("rootCertFile") != null &&  !"null".equals(certsMap.get("rootCertFile"))) {
        	rootCertFile = new File((String) certsMap.get("rootCertFile"));
        } else throw new Exception(" --- Missing arg -> rootCertFile");
        if (certsMap.get("rootSubjectDN") != null && 
                !"null".equals(certsMap.get("rootSubjectDN"))) {
        	rootSubjectDN = (String) certsMap.get("rootSubjectDN");
        } else throw new Exception(" --- Missing arg -> rootSubjectDN");
        if (certsMap.get("password")!= null && 
                !"null".equals(certsMap.get("password"))) {
        	password = (String) certsMap.get("password");
        } else throw new Exception(" --- Missing arg -> password");
        CertGenerator cerGenerator = new CertGenerator(rootCertFile, rootSubjectDN, password);
        if (certsMap.get("certs") != null &&  !"null".equals(certsMap.get("certs"))) {
            List<Map> certList = (List<Map>) certsMap.get("certs");
            for(Map certMap : certList) {
            	File certFile = null;
            	String distinguishedName = null;
            	String alias = null;
                Boolean isTimeStampingCert = false;
                if (certMap.get("file") != null) {
                	certFile = new File((String) certMap.get("file"));
                	certFile.createNewFile();
                } else throw new Exception("Cert with Missing arg -> file");
                if (certMap.get("distinguishedName") != null) {
                	distinguishedName = (String) certMap.get("distinguishedName");
                } else throw new Exception("Cert with Missing arg -> distinguishedName");
                if (certMap.containsKey("alias")) {
                	alias = (String) certMap.get("alias");
                } else throw new Exception("Cert with Missing arg -> alias");
                if (certMap.containsKey("isTimeStampingCert")) {
                	isTimeStampingCert = (Boolean) certMap.get("isTimeStampingCert");
                } else throw new Exception("Cert with Missing arg -> isTimeStampingCert");
                if(isTimeStampingCert) cerGenerator.genTimeStampingKeyStore(distinguishedName, certFile, alias);
                else cerGenerator.genUserKeyStore(distinguishedName, certFile, alias);
            }
        }
    }
}
