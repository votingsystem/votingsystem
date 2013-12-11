package org.votingsystem.util;


public class NifUtils {
	    
    public static String getNif(int number) {
        String numberStr = String.format("%08d", number);
        return numberStr + getNifLetter(number);
    }
    
    public static String validate(String nif) {
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

    public static String getNifLetter(int dni) {
        String nifChars="TRWAGMYFPDXBNJZSQVHLCKET";
        int module= dni % 23;
        Character nifChar = nifChars.charAt(module);
        return nifChar.toString();
    }

}
