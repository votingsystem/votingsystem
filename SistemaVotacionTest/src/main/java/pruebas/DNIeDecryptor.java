package pruebas;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.DES3SecretKey;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.parameters.InitializationVectorParameters;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.smime.DNIeSessionHelper.LABEL_CLAVE_PRIVADA_FIRMA;
import static org.sistemavotacion.smime.DNIeSessionHelper.closeSession;
import org.sistemavotacion.util.OSValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ESTE CODIGO NO FUNCIONA!!!
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DNIeDecryptor {
    
    private static Logger logger = LoggerFactory.getLogger(DNIeDecryptor.class);    
    
    
    public static void main(String args[]) throws Exception {
        Contexto.INSTANCE.init();
        
        //Mechanism decryptionMechanism = Mechanism.get(PKCS11Constants.CKM_DES3_CBC_PAD);
        //Mechanism decryptionMechanism = Mechanism.DES3_CBC;
        Mechanism decryptionMechanism = Mechanism.DES3_CBC_PAD;
        byte[] decryptInitializationVector = { 0, 0, 0, 0, 0, 0, 0, 0 };
        InitializationVectorParameters decryptInitializationVectorParameters = new InitializationVectorParameters(
            decryptInitializationVector);
        decryptionMechanism.setParameters(decryptInitializationVectorParameters);
    }
    
    public static Session getSession (char[] password, Mechanism decryptMechanism,
            byte[] encryptedData) throws Exception {
        Module pkcs11Module  = null;
        Session pkcs11Session = null;
        Token token = null;
        try {
            if (pkcs11Module == null) {
                pkcs11Module  = Module.getInstance(OSValidator.getPKCS11ModulePath());
                pkcs11Module.initialize(null);
            }
            final LectorSlots lectorSlots = new LectorSlots(
                    pkcs11Module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT));
            lectorSlots.setSlots(pkcs11Module.getSlotList(
                        Module.SlotRequirement.TOKEN_PRESENT));
            Slot slot = lectorSlots.obtenerSlotSeleccionado();
            if(slot == null) throw new Exception ("¿Seguro que tiene el DNI corréctamente insertado en el lector?");
            token = slot.getToken();
            if(token == null) throw new Exception ("¿Seguro que tiene instalada en su"
                    + "máquina el soporte para PKCS#11?");
            pkcs11Session = token.openSession(Token.SessionType.SERIAL_SESSION,
                Token.SessionReadWriteBehavior.RO_SESSION, null, null);
            pkcs11Session.login(Session.UserType.USER, password);
            RSAPrivateKey templateSignatureKey = new RSAPrivateKey();
            templateSignatureKey.getSign().setBooleanValue(Boolean.TRUE);
            //templateSignatureKey.getDecrypt().setBooleanValue(Boolean.TRUE);
            
            
            templateSignatureKey.getLabel().setCharArrayValue(
                LABEL_CLAVE_PRIVADA_FIRMA.toCharArray());
            pkcs11Session.findObjectsInit(templateSignatureKey);
            iaik.pkcs.pkcs11.objects.Object[] foundSignatureKeyObjects = pkcs11Session.findObjects(1); // find first
            if (foundSignatureKeyObjects.length > 0) {
                RSAPrivateKey signatureKey = (RSAPrivateKey) foundSignatureKeyObjects[0];
                
                logger.debug(" --- signatureKey.getObjectHandle(): " 
                        + signatureKey.getObjectHandle());
                logger.debug(" --- signatureKey.toString(): " + signatureKey.toString());
                
                DES3SecretKey des3SecretKey = (DES3SecretKey) DES3SecretKey.getInstance(
                        pkcs11Session, signatureKey.getObjectHandle());
     		DES3SecretKey secretEncryptionKeyTemplate = new DES3SecretKey();
		secretEncryptionKeyTemplate.getEncrypt().setBooleanValue(Boolean.TRUE);
		secretEncryptionKeyTemplate.getDecrypt().setBooleanValue(Boolean.TRUE);
                des3SecretKey.getAttributeTable().put(Attribute.DECRYPT, true);
                
                //logger.debug(" --- des3SecretKey: " + des3SecretKey.toString());                
                        
                logger.debug(" --------------------------------------------- ");    
                
                pkcs11Session.decryptInit(decryptMechanism, des3SecretKey);
                

                byte[] decryptedData = pkcs11Session.decrypt(encryptedData);
                logger.debug(" --- decryptedData: " + new String(decryptedData));
            } else {
                throw new Exception ("No se ha encontrado la clave de firma");
            }
            pkcs11Session.findObjectsFinal();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            String mensajeError = ex.getMessage();
            if (ex instanceof ArrayIndexOutOfBoundsException) {
                mensajeError = Contexto.INSTANCE.getString("smartCardReaderErrorMsg");
            }
            if ("CKR_DEVICE_ERROR".equals(ex.getMessage()) || 
                    "CKR_CRYPTOKI_ALREADY_INITIALIZED".equals(ex.getMessage()) ||
                    "CKR_USER_ALREADY_LOGGED_IN".equals(ex.getMessage())) {
                closeSession();
                //return getSession(password, decryptMechanism);
            }
            if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) { }
            if ("CKR_HOST_MEMORY".equals(ex.getMessage())) mensajeError = 
                    Contexto.INSTANCE.getString("smartCardReaderErrorMsg");
            throw new Exception(mensajeError);
        }
        //closeSession();
        return pkcs11Session;
    }
         
    private static class LectorSlots {

        Slot[] slots;

        public LectorSlots (Slot[] slots) {
            this.slots = slots;
        }

        public void setSlots (Slot[] slots) {
            this.slots = slots;
        }

        public Slot obtenerSlotSeleccionado () {
            if(slots == null || slots.length == 0) return null;
            return slots[0];
        }

    }
    
}
