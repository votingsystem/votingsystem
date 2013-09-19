package org.sistemavotacion.util;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class StringUtils {
	
	public static final String TAG = "StringUtils";
	
    private static final int TAMANYO_TITULO = 20;

    public static String getTitleString (String title) {
        if (title.length() > TAMANYO_TITULO)
            return title.substring(0, TAMANYO_TITULO) + "...";
        else return title;
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
        return new String(salida.toByteArray());
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

    public static String decodeString(String string) {
    	if(string == null) return null;
    	String result = null;
        try {
        	result = URLDecoder.decode(string, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Log.e(TAG + ".decodeString()", ex.getMessage(), ex);
		}
    	return result;
    }
    
	public static String getCadenaNormalizada(String cadena) {
        return cadena.replaceAll("[\\/:.]", ""); 
	}
        
}
