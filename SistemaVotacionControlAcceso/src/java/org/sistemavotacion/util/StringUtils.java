package org.sistemavotacion.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static String getStringFromClob (Clob clob) {
    		if(clob == null) return null;
            String stringClob = "";
            try {
            	if (clob.length() == 0) return stringClob;
                long i = 1;
                int clobLength = (int) clob.length();
                stringClob = clob.getSubString(i, clobLength);
            }
            catch (Exception e) {
                    logger.error(e.getMessage(), e);
            }
            return stringClob;
    }	

    public static String randomLowerString(long seed, int size) {
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
    
    public static Clob getClobFromString (String string) {
    	if(string == null) return null;
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
    
    public static String getStringFromInputStream(InputStream entrada) throws IOException {
    	ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buf =new byte[1024];
        int len;
        while((len = entrada.read(buf)) > 0){
            salida.write(buf,0,len);
        }
        salida.close();
        entrada.close();
        return new String(salida.toByteArray(), "UTF-8");
    }
    
    public static String getCadenaNormalizada (String cadena) {
        if(cadena == null) return null;
        else return cadena.replaceAll("[\\/:.]", ""); 
    }
    
    // sacado de http://felinfo.blogspot.com.es/2010/12/calcular-la-letra-del-dni-con-java.html
    public static String calculaLetraNIF(int dni) {
	    String juegoCaracteres="TRWAGMYFPDXBNJZSQVHLCKET";
	    int modulo= dni % 23;
	    Character letra = juegoCaracteres.charAt(modulo);
	    return letra.toString(); 
    }
    
    public static String validarNIF(String nif) {
    	if(nif == null) return null;
    	nif  = nif.toUpperCase();
    	if(nif.length() < 9) {
            int numberZeros = 9 - nif.length();
			for(int i = 0; i < numberZeros ; i++) {
				nif = "0" + nif;
			}
    	}
    	String number = nif.substring(0, 8);
        String letter = nif.substring(8, 9);
        try {
            if(!letter.equals(calculaLetraNIF(new Integer(number)))) return null;
            else return nif;
        } catch (Exception ex) {
            return null;
        }
    }
}
