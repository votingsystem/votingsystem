package org.votingsystem.util;

import android.content.Context;

import org.votingsystem.android.lib.R;

public class NifUtils {
	    
    public static String getNif(int number) {
        String numberStr = String.format("%08d", number);
        return numberStr + getNifLetter(number);
    }

    public static String validate(String nif, Context contex) throws ExceptionVS {
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
            else throw new ExceptionVS(contex.getString(R.string.nif_with_errors_msg, nif));
        }
    }

    public static String getNifLetter(int dni) {
        String nifChars="TRWAGMYFPDXBNJZSQVHLCKET";
        int module= dni % 23;
        Character nifChar = nifChars.charAt(module);
        return nifChar.toString();
    }

}
