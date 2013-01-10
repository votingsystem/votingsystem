package pruebas;

import java.io.File;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 'Compañero' de ValidateSignedWithKeyStore
 */
public class CreatedSignedWithKeyStore {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(MultiFirmaTest.class);
    
    public static final String keyAlias1 = "aliascertfirmado";
    public static final String password1 = "PemPass";
    public static final String keyStorePath1 = "/host/temp/KeyStoreFirmaVoto.jks";
    
    public static final String keyAlias2 = "clavessistemavotacion";
    public static final String password2 = "PemPass";
    public static final String keyStorePath2 = "/host/temp/KeyStoreVerisign.jks";
    
    public static final String signedFilePath = "/host/temp/signedWithKeyStore";
    
    public static void main(String args[]) {
        crearArchivoFirmado();
    }
    
    
    public static void crearArchivoFirmado () {
        try {
            Contexto.inicializar();
            File keyStoreFile = new File(keyStorePath2);
            SignedMailGenerator dnies = new SignedMailGenerator(
                    FileUtils.getBytesFromFile(keyStoreFile), keyAlias2, password2.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            logger.debug(dnies.genString("from@m.com", "toUser@m.com", 
                    "blim blim", "asunto", null, SignedMailGenerator.Type.USER));
            FileUtils.copyFileToFile(dnies.genFile("from@m.com", "toUser@m.com", 
                    "blim blim", "asunto", null, SignedMailGenerator.Type.USER), 
                    new File(signedFilePath));
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } 
    }

}
