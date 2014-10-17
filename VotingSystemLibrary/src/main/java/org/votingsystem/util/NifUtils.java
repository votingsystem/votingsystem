package org.votingsystem.util;

import org.votingsystem.model.ContextVS;

public class NifUtils {
	    
    public static String getNif(int number) {
        return String.format("%08d", number) + getNifLetter(number);
    }
    
    public static String validate(String nif) throws ExceptionVS {
        String result = null;
        try {
            if(nif != null && nif.length() <= 9) {
                nif = String.format("%09d", nif).toUpperCase();
                String number = nif.substring(0, 8);
                String letter = nif.substring(8, 9);
                if(letter.equals(getNifLetter(new Integer(number)))) result = nif;
            }
        } catch(Exception ex) {} finally {
            if(result != null) return result;
            else throw new ExceptionVS(ContextVS.getMessage("NIFWithErrorsMsg", nif));
        }

    }

    public static String getNifLetter(int dni) {
        String nifChars ="TRWAGMYFPDXBNJZSQVHLCKET";
        int module = dni % 23;
        Character letter = nifChars.charAt(module);
        return letter.toString();
    }

}