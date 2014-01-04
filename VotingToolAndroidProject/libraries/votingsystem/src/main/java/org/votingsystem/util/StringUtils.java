package org.votingsystem.util;

import android.util.Log;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.signature.smime.CMSUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class StringUtils {
	
	public static final String TAG = "StringUtils";
 
    public static String getStringFromInputStream(InputStream entrada) throws IOException {
    	ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buf =new byte[4096];
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
    
	public static String getNormalized(String cadena) {
        return cadena.replaceAll("[\\/:.]", ""); 
	}

    public static String checkURL(String url) {
        if(url == null) return null;
        url = url.trim();
        String result = null;
        if(url.startsWith("http://") || url.startsWith("https://")) result = url;
        else result = "http://" + url;
        while(result.endsWith("/")) {
            result = result.substring(0, result.length() -1);
        }
        return result;
    }

    /** Write the object to a Base64 string. */
    public static String serializeObjectToString(Serializable serializable) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();
        return new String(Base64.encode(baos.toByteArray()));
    }

    /** Read the object from Base64 string. */
    public static Object deSerializedObjectFromString(String base64SerializedObject) throws IOException,
            ClassNotFoundException {
        byte [] data = Base64.decode(base64SerializedObject);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object object  = ois.readObject();
        ois.close();
        return object;
    }
}
