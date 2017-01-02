package org.votingsystem.util;

import org.votingsystem.throwable.ValidationException;

public class NifUtils {
	    
    public static String getNif(int number) {
        return String.format("%08d", number) + getNifLetter(number);
    }
    
    public static String validate(String nif) throws ValidationException {
        String result = null;
        try {
            if(nif != null && nif.length() <= 9) {
                String number = nif.substring(0, nif.length() -1);
                String letter = nif.substring(nif.length() -1, nif.length()).toUpperCase();
                if(letter.equals(getNifLetter(new Integer(number))))
                    result = String.format("%08d", Integer.valueOf(number)) + letter;
            }
        } catch(Exception ex) {} finally {
            if(result != null) return result;
            else throw new ValidationException("Bad NIF: " + nif);
        }

    }

    public static String getNifLetter(int dni) {
        String nifChars ="TRWAGMYFPDXBNJZSQVHLCKET";
        int module = dni % 23;
        Character letter = nifChars.charAt(module);
        return letter.toString();
    }

}