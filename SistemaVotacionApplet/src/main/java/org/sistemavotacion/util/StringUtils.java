package org.sistemavotacion.util;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.Random;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class StringUtils {
	
    private static Logger logger = LoggerFactory.getLogger(StringUtils.class);

    private static final int TAMANYO_TITULO = 20;

    public static String getStringFromClob (Clob clob) {
        String stringClob = null;
        try {
                long i = 1;
                int clobLength = (int) clob.length();
                stringClob = clob.getSubString(i, clobLength);
        }
        catch (Exception e) {
                logger.error(e.getMessage(), e);
        }
        return stringClob;
    }	
    
    public static Clob getClobFromString (String string) {
        Clob clob = null;
        try {
            clob = new SerialClob(string.toCharArray());
        } catch (SerialException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return clob;
    }

    public static String getTitleString (String title) {
        if (title.length() > TAMANYO_TITULO)
            return title.substring(0, TAMANYO_TITULO) + "...";
        else return title;
    }
    
    public static String getCadenaNormalizada (String cadena) {
        if(cadena == null) return null;
        else return cadena.replaceAll("[\\/:.]", ""); 
    }
    
    public static String prepararURL(String url) {
        String resultado = null;
        if(url.contains("http://")) {
            resultado = url;
        } else {
            resultado = "http://" + url;
        }
        while(resultado.endsWith("/")) {
            resultado = resultado.substring(0, resultado.length() -1);
        }
        return resultado.trim();
    }

    public static String RandomLowerString(long seed, int size) {
        StringBuffer tmp = new StringBuffer();
        Random random = new Random(seed);
        for (int i = 0; i < size; i++) {
            long newSeed = random.nextLong();
            int currInt = (int) (26 * random.nextFloat());
            currInt += 97;
            random = new Random(newSeed);
            tmp.append((char) currInt);
        }
        return tmp.toString();
    }
    
}
