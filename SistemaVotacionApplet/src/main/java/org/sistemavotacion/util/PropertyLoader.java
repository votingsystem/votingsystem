package org.sistemavotacion.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class PropertyLoader {

    private static Logger logger = LoggerFactory.getLogger(PropertyLoader.class);

    /*
    public static final String VALIDACION_OCSP = "VALIDACION_OCSP";
    public static final String URL_SERVIDOR_OCSP = "URL_SERVIDOR_OCSP";
    public static final String ACCESO_PROXY = "ACCESO_PROXY";
    public static final String DIRECCION_PROXY = "DIRECCION_PROXY";
    public static final String PUERTO_PROXY = "PUERTO_PROXY";
    public static final String USUARIO_AUTENTICADO = "USUARIO_AUTENTICADO";
    public static final String USUARIO_PROXY = "USUARIO_PROXY";
    public static final String PASSWORD_PROXY = "PASSWORD_PROXY";
    public static final String RUTA_SERVLET = "RUTA_SERVLET";
    public static final String ESTADO_APLICACION = "ESTADO_APLICACION";
    public static final String APLICACION_DESCARGADA = "DESCARGADA";
    public static final String APLICACION_INSTALADA = "INSTALADA";*/  
    
    private Properties properties;
    private File propertiesFile;

    public PropertyLoader (String propertiesPath) {
        try {
            propertiesFile = new File(propertiesPath);
            propertiesFile.createNewFile();
            properties = new Properties();
            properties.load(new FileInputStream(propertiesFile));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public String getProperty (String key) {
        return properties.getProperty(key);
    }
    
    public void setProperty (String key, String value) {
        properties.setProperty(key, value);
        save();
    }
    
    private void save () {
        try {
            FileOutputStream out = new FileOutputStream(propertiesFile);
            properties.store(out, null);
            out.close();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
}
