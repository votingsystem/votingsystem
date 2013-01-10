package org.sistemavotacion.smime;

import static org.sistemavotacion.Contexto.*;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.OSValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class DNIeSessionHelper {
    
    private static Logger logger = LoggerFactory.getLogger(DNIeSessionHelper.class);    
    
    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN = "CertFirmaDigital";
    public static final String CERT_CA = "CertCAIntermediaDGP";
    public static final String LABEL_CLAVE_PRIVADA_AUTENTICACION = "KprivAutenticacion";
    public static final String LABEL_CLAVE_PRIVADA_FIRMA = "KprivFirmaDigital";

    
    
    private static Session pkcs11Session;
    private static Module pkcs11Module;
    private static Token token;
    private static RSAPrivateKey signatureKey;
    private static X509Certificate certificadoUsuario;
    private static X509Certificate certificadoIntermedio;
    private static X509Certificate certificadoCA;
    
    public static String CERT_STORE_TYPE = "Collection";
       

    public DNIeSessionHelper ( ) { }
    
    
    public static Session getSession (char[] password, Mechanism signatureMechanism) throws Exception {
        pkcs11Module  = null;
        pkcs11Session = null;
        token = null;
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
            templateSignatureKey.getLabel().setCharArrayValue(
                LABEL_CLAVE_PRIVADA_FIRMA.toCharArray());
            pkcs11Session.findObjectsInit(templateSignatureKey);
            Object[] foundSignatureKeyObjects = pkcs11Session.findObjects(1); // find first
            if (foundSignatureKeyObjects.length > 0) {
                signatureKey = (RSAPrivateKey) foundSignatureKeyObjects[0];
                pkcs11Session.signInit(signatureMechanism, signatureKey);
            } else {
                throw new Exception ("No se ha encontrado la clave de firma");
            }
            pkcs11Session.findObjectsFinal();
            X509PublicKeyCertificate certificateTemplate = new X509PublicKeyCertificate();
            pkcs11Session.findObjectsInit(certificateTemplate);
            Object[] tokenCertificateObjects;
            FileInputStream fis =  new FileInputStream(FileUtils.APPDIR + Contexto.CERT_RAIZ_PATH);
            certificadoCA = CertUtil.loadCertificateFromStream(fis);
            while ((tokenCertificateObjects = pkcs11Session.findObjects(1)).length > 0) {
                iaik.pkcs.pkcs11.objects.Object object = (Object) tokenCertificateObjects[0];
                Hashtable attributes = object.getAttributeTable();
                ByteArrayAttribute valueAttribute = (ByteArrayAttribute) attributes.get(Attribute.VALUE);
                byte[] value = valueAttribute.getByteArrayValue();
                X509PublicKeyCertificate cert =
                    (X509PublicKeyCertificate)tokenCertificateObjects[0];
                if (CERT_SIGN.equals(cert.getLabel().toString())) {
                    certificadoUsuario = (X509Certificate)CMSUtils.getCertificate(value);
                    Contexto.setUsuario(Usuario.getUsuario(certificadoUsuario));
                } else if (CERT_CA.equals(cert.getLabel().toString())) {
                    certificadoIntermedio = (X509Certificate)CMSUtils.getCertificate(value);
                }
            }
            pkcs11Session.findObjectsFinal();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            String mensajeError = ex.getMessage();
            if (ex instanceof ArrayIndexOutOfBoundsException) {
                mensajeError = getString("smartCardReaderErrorMsg");
            }
            if ("CKR_DEVICE_ERROR".equals(ex.getMessage()) || 
                    "CKR_CRYPTOKI_ALREADY_INITIALIZED".equals(ex.getMessage()) ||
                    "CKR_USER_ALREADY_LOGGED_IN".equals(ex.getMessage())) {
                closeSession();
                return getSession(password, signatureMechanism);
            }
            if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) {
                Contexto.setDNIePassword(null);
            }
            if ("CKR_HOST_MEMORY".equals(ex.getMessage())) mensajeError = getString("smartCardReaderErrorMsg");
            throw new Exception(mensajeError);
        }
        //closeSession();
        return pkcs11Session;
    }
 
    public static void closeSession () {
        logger.debug("closeSession");
        try {
            if (token != null && token.getTokenInfo() != null 
                    && token.getTokenInfo().isTokenInitialized()) {
                token.closeAllSessions();
            }
            if (pkcs11Session != null ) {
                pkcs11Session.closeSession();
                pkcs11Session = null;
            } 
            if (pkcs11Module != null ) {
                pkcs11Module.finalize(null);
                pkcs11Module = null;
            } 
            System.gc();
        } catch (TokenException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    
    /**
     * @return the certificadoUsuario
     */
    public static X509Certificate getCertificadoUsuario() {
        return certificadoUsuario;
    }
    
    /**
     * @return the certificadoUsuario
     */
    public static X509Certificate getCertificadoIntermedio() {
        return certificadoIntermedio;
    }
    
            /**
     * @return the certificadoUsuario
     */
    public static  X509Certificate getCertificadoCA() {
        return certificadoCA;
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
