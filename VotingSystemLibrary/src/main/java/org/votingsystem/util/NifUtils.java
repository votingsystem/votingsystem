package org.votingsystem.util;

import org.votingsystem.model.ContextVS;

public class NifUtils {
	    
    public static String getNif(int number) {
        String numberStr = String.format("%08d", number);
        return numberStr + getNifLetter(number);
    }
    
    public static String validate(String nifParam) throws ExceptionVS {
        try {
            String nif = new String(nifParam);
            if(nif != null && nif.length() <= 9) {
                nif  = nif.toUpperCase();
                int numberZeros = 9 - nif.length();
                for(int i = 0; i < numberZeros ; i++) {
                    nif = "0" + nif;
                }
                String number = nif.substring(0, 8);
                String letter = nif.substring(8, 9);
                if(letter.equals(getNifLetter(new Integer(number)))) return nif;
            }
        } catch(Exception ex) {
            throw new ExceptionVS(ContextVS.getMessage("NIFWithErrorsMsg", nifParam));
        }
        throw new ExceptionVS(ContextVS.getMessage("NIFWithErrorsMsg", nifParam));
    }

    public static String getNifLetter(int dni) {
        String nifChars ="TRWAGMYFPDXBNJZSQVHLCKET";
        int module = dni % 23;
        Character letter = nifChars.charAt(module);
        return letter.toString();
    }

}