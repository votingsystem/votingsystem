package org.sistemavotacion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500PrivateCredential;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertGenerator {
	
    private static Logger logger = LoggerFactory.getLogger(CertGenerator.class);

	public static String ROOT_ALIAS = "rootAlias";//
	public static String END_ENTITY_ALIAS = "endEntityAlias";//


	public static long COMIEZO_VALIDEZ_CERT = System.currentTimeMillis();//
	public static final int PERIODO_VALIDEZ_ALMACEN_RAIZ = 2000000000;//En producción durará lo que dure una votación
	 public static final int PERIODO_VALIDEZ_CERT = 2000000000;
	 
	private File rootCertFile;
	private String rootSubjectDN;
	private String password;

	
	X500PrivateCredential rootPrivateCredential;
	
	public static void main(String[] args) throws Exception{
		if(args == null) return;
		generate(args[0]);
	}
	
	public CertGenerator(File rootCertFile, String rootSubjectDN, 
			String password) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		this.rootCertFile = rootCertFile;
		this.rootSubjectDN = rootSubjectDN;
		this.password = password;
		KeyStore rootKeyStore = KeyStoreUtil.createRootKeyStore(COMIEZO_VALIDEZ_CERT,
				PERIODO_VALIDEZ_ALMACEN_RAIZ, password.toCharArray(), ROOT_ALIAS,
				rootCertFile.getAbsolutePath(), rootSubjectDN);
		X509Certificate rootCertificate = (X509Certificate)rootKeyStore.getCertificate(ROOT_ALIAS);
		PrivateKey rootPK = (PrivateKey) rootKeyStore.getKey(ROOT_ALIAS, password.toCharArray());
		rootPrivateCredential =	new X500PrivateCredential(rootCertificate, rootPK, ROOT_ALIAS);
	}
	
	public void genUserKeyStore(String subjectDN, File file, String alias) throws Exception {
		logger.debug("--- genUserKeyStore - subjectDN: " + subjectDN + 
				" - file: " + file.getAbsolutePath() + " - alias: " + alias);
		KeyStore keyStore = KeyStoreUtil.createActorKeyStore(COMIEZO_VALIDEZ_CERT,
				PERIODO_VALIDEZ_CERT, password.toCharArray(),
				alias, rootPrivateCredential, subjectDN);
		byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
		FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
	}
	
    public static void generate (String operacionStr) throws Exception {
        logger.debug("- generate: '" + operacionStr + "'");
        if(operacionStr == null) return;
        File rootCertFile = null;
        String rootSubjectDN = null;
        String password = null;
        JSON datosJSON = JSONSerializer.toJSON(operacionStr);
        if(JSONNull.getInstance().equals(datosJSON)) return;
        JSONObject operacionJSON = null;
        if(datosJSON instanceof JSONArray) {
            operacionJSON = ((JSONArray)datosJSON).getJSONObject(0);
        } else operacionJSON = (JSONObject)datosJSON;
        if (operacionJSON.containsKey("rootCertFile")) {
        	rootCertFile = new File(operacionJSON.getString("rootCertFile"));
        } else throw new Exception(" --- Missing arg -> rootCertFile");
        if (operacionJSON.containsKey("rootSubjectDN")) {
        	rootSubjectDN = operacionJSON.getString("rootSubjectDN");
        } else throw new Exception(" --- Missing arg -> rootSubjectDN");
        if (operacionJSON.containsKey("password")) {
        	password = operacionJSON.getString("password");
        } else throw new Exception(" --- Missing arg -> password");
        CertGenerator cerGenerator = new CertGenerator(rootCertFile, rootSubjectDN, password);
        if (operacionJSON.containsKey("certs")) {
            JSONArray arrayCerts = operacionJSON.getJSONArray("certs");
            for(int i = 0; i < arrayCerts.size(); i++) {
            	JSONObject certData  = arrayCerts.getJSONObject(i);
            	File certFile = null;
            	String distinguishedName = null;
            	String alias = null;
                if (certData.containsKey("file")) {
                	certFile = new File(certData.getString("file"));
                	certFile.createNewFile();
                } else throw new Exception("Cert: " + certData.toString() + 
                		" --- Missing arg -> file");
                if (certData.containsKey("distinguishedName")) {
                	distinguishedName = certData.getString("distinguishedName");
                } else throw new Exception("Cert: " + certData.toString() + 
                		" --- Missing arg -> distinguishedName");
                if (certData.containsKey("alias")) {
                	alias = certData.getString("alias");
                } else throw new Exception("Cert: " + certData.toString() + 
                		" --- Missing arg -> alias");
                cerGenerator.genUserKeyStore(distinguishedName, certFile, alias);
            }
        }
    }
}
