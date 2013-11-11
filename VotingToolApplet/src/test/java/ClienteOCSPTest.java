

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.ClienteOCSP;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class ClienteOCSPTest {
    
    private static Logger logger = LoggerFactory.getLogger(ClienteOCSPTest.class);    
    
    public ClienteOCSPTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void checkCert() throws IOException, Exception {
        /*Contexto.inicializar();
        //File certFile = new File("src/main/resources/testFiles/certusu.pem");
        InputStream inputStreamUsu = getClass().
                getClassLoader().getResourceAsStream("testFiles/certusu.pem");
        byte[] pemCertBytesUsu =  FileUtils.getBytesFromInputStream(inputStreamUsu);
        X509Certificate certUsu = CertUtil.fromPEMToX509Cert(pemCertBytesUsu);
        logger.debug("certUsu: " + certUsu);
        
        InputStream inputStreamIntermedio = getClass().
                getClassLoader().getResourceAsStream("testFiles/AC_DNIE_003_SHA1.pem");
        byte[] pemCertBytesIntermedio =  FileUtils.getBytesFromInputStream(inputStreamIntermedio);
        X509Certificate certIntermedio = CertUtil.fromPEMToX509Cert(pemCertBytesIntermedio);
        
        logger.debug("certIntermedio: " + certIntermedio);
        ClienteOCSP.EstadoCertificado estado = ClienteOCSP.validarCertificado(
                certIntermedio, certUsu.getSerialNumber(), new Date(System.currentTimeMillis()));

        logger.debug("estado: " + estado);*/

    }
    
}
