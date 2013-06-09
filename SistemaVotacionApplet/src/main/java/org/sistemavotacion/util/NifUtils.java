package org.sistemavotacion.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
public class NifUtils {
	
    private static Logger logger = LoggerFactory.getLogger(NifUtils.class);
    
    public static String getNif(int number) {
        String numberStr = String.format("%08d", number);
        return numberStr + getNifLetter(number);
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
            if(!letter.equals(getNifLetter(new Integer(number)))) return null;
            else return nif;
        } catch (Exception ex) {
            return null;
        }
    }
    
    // sacado de http://felinfo.blogspot.com.es/2010/12/calcular-la-letra-del-dni-con-java.html
    public static String getNifLetter(int dni) {
        String juegoCaracteres="TRWAGMYFPDXBNJZSQVHLCKET";
        int modulo= dni % 23;
        Character letra = juegoCaracteres.charAt(modulo);
        return letra.toString(); 
    }
    
    public static void main(String[] args) {
        /*for(int i = 0; i <10; i++) {
            logger.debug("Numero " + i + " -> nif: " + validarNIF(getNif(i)));
        }*/
        logger.debug("Numero " + 100000000 + " -> nif: " + getNif(1234567));
    }
    
    
}
