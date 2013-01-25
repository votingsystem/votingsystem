import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500PrivateCredential;
import org.gradle.api.DefaultTask
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.FileUtils;
import org.gradle.api.tasks.*;
import org.sistemavotacion.Contexto;

class CertGenerator extends DefaultTask {


	public static String ROOT_ALIAS = "rootAlias";//
	public static String END_ENTITY_ALIAS = "endEntityAlias";//


	public static long COMIEZO_VALIDEZ_CERT = System.currentTimeMillis();//
	public static final int PERIODO_VALIDEZ_ALMACEN_RAIZ = 2000000000;//En producción durará lo que dure una votación
	 public static final int PERIODO_VALIDEZ_CERT = 2000000000;
	 
	File rootCertFile;
	String rootCertNIF;
	String rootSubjectDN
	String password
	
	
	X500PrivateCredential rootPrivateCredential;
	
	@TaskAction def generateCerts() {
		println "--- :${project.name}:generateCerts - rootSubjectDN:${rootSubjectDN} - " + 
				"rootCertFile.path:${rootCertFile.path} "
		Contexto.inicializar();
		KeyStore rootKeyStore = KeyStoreUtil.createRootKeyStore(COMIEZO_VALIDEZ_CERT,
				PERIODO_VALIDEZ_ALMACEN_RAIZ, password.toCharArray(), ROOT_ALIAS,
				rootCertFile.getAbsolutePath(), rootSubjectDN);
		X509Certificate rootCertificate = (X509Certificate)rootKeyStore.getCertificate(ROOT_ALIAS);
		PrivateKey rootPK = (PrivateKey) rootKeyStore.getKey(ROOT_ALIAS, password.toCharArray());
		rootPrivateCredential =	new X500PrivateCredential(rootCertificate, rootPK, ROOT_ALIAS);
	}
	
	public void genUserKeyStore(String subjectDN, File file, String alias) throws Exception {
		println "--- :${project.name}:genUserKeyStore - subjectDN:${subjectDN} - file:${file.path}"
		KeyStore keyStore = KeyStoreUtil.createActorKeyStore(COMIEZO_VALIDEZ_CERT,
				PERIODO_VALIDEZ_CERT, password.toCharArray(),
				alias, rootPrivateCredential, subjectDN);
		byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
		FileUtils.copyStreamToFile(new ByteArrayInputStream(keyStoreBytes),file);
	}

		
}