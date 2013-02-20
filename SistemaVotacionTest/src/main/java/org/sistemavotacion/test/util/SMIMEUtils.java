package org.sistemavotacion.test.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.json.DeJSONAObjeto;
import org.sistemavotacion.test.modelo.SolicitudAcceso;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class SMIMEUtils {
        
    private static Logger logger = LoggerFactory.getLogger(SMIMEUtils.class);
    
    private SolicitudAcceso obtenerSolicitudAcceso(File archivo) {
        List<SolicitudAcceso>  solicitudes = new ArrayList<SolicitudAcceso>();
        SolicitudAcceso solicitud = null;
        try {
            byte[] bytes = FileUtils.getBytesFromFile(archivo);
            SMIMEMessageWrapper smimeMessage = new SMIMEMessageWrapper(
            		null, new ByteArrayInputStream(bytes), null);
            if (smimeMessage.isValidSignature()) {
                solicitud = DeJSONAObjeto.obtenerSolicitudAcceso(
                        smimeMessage.getSignedContent());
                solicitud.setFirmaCorrecta(true);
                //String[] hashCertificado = smimeMessage.getHeader(ContextoPruebas.NOMBRE_ENCABEZADO_HASH);
                //solicitud.setHashCertificadoVotoBase64(hashCertificado[0]);
                File parentDir = archivo.getParentFile();
                File[] userCerts = parentDir.listFiles(new FilenameFilter() {
                    public boolean accept(File d, String name) { 
                        return (name.startsWith(ContextoPruebas.PREFIJO_USER_JKS));
                    }
                });
                if(userCerts != null) {
                    KeyStore userKeyStore = KeyStoreUtil.getKeyStoreFromFile(
                        userCerts[0], ContextoPruebas.PASSWORD.toCharArray());
                    if(userKeyStore!= null) {
                        solicitud.setUserKeyStore(userKeyStore);
                    } else logger.debug("No se ha encontrado el almacén de claves del usuario");
                } else logger.debug("No se ha encontrado el almacén de claves del usuario");
            } else {
                solicitud = new SolicitudAcceso(false);
            }
            solicitudes.add(solicitud);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }            
        return solicitud;
    }
}
