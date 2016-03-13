package org.votingsystem.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class StringUtils {
	
    private static Logger log = Logger.getLogger(StringUtils.class.getName());

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


    public static String decodeB64_TO_UTF8 (String encodedStr) throws UnsupportedEncodingException, ScriptException {
        String decodeStr = new String(Base64.getDecoder().decode(encodedStr.getBytes()), "UTF-8");
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        String result = (String) engine.eval("unescape('" + java.net.URLDecoder.decode(decodeStr, "UTF-8") + "')");
        return result;
    }

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
                    log.log(Level.SEVERE, e.getMessage(), e);
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
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return clob;
    }

    public static String getFormattedErrorList(List<String> errorList) {
        if(errorList.isEmpty()) return null;
        else {
            StringBuilder result = new StringBuilder("");
            for(String error:errorList) {
                result.append(error + "\n");
            }
            return result.toString();
        }
    }

    public static String getUserDirPath (String userNIF) {
        int subPathLength = 5;
        String basePath = "/";
        while (userNIF.length() > 0) {
            if(userNIF.length() <= subPathLength) subPathLength = userNIF.length();
            String subPath = userNIF.substring(0, subPathLength);
            userNIF = userNIF.substring(subPathLength);
            basePath = basePath + subPath + File.separator;
        }
        if(!basePath.endsWith("/")) basePath = basePath + "/";
        return basePath;
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

    public String getStringFromDocument(org.w3c.dom.Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }

    public static String toHex(String paramsStr) {
        if (paramsStr == null) return null;
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(paramsStr.getBytes());
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) return null;
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(bytes);
    }

    public static String getNormalized (String string) {
        if(string == null) return null;
        else return normalize(string).replaceAll(" ", "_").replaceAll("[\\/:.]", "");
    }

    public static String normalize (String string) {
        StringBuilder sb = new StringBuilder(string.length());
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        for (char c : string.toCharArray()) {
            if (c <= '\u007F') sb.append(c);
        }
        return sb.toString();
    }

    //http://stackoverflow.com/questions/3322152/is-there-a-way-to-get-rid-of-accents-and-convert-a-whole-string-to-regular-lette
    public static String removeAccents(String string) {
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        //if unicode string = string.replaceAll("\\p{M}", "");
        return string.replaceAll("[^\\p{ASCII}]", "");
    }

    public static String getRandomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()* ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public static String truncateMessage(String message, int TRUNCATED_MSG_SIZE) {
        if(message != null && message.length() > TRUNCATED_MSG_SIZE)
            return message.substring(0, TRUNCATED_MSG_SIZE) + "...";
        else return message;
    }

    public static String getHashBase64 (String origStr, String digestAlgorithm) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
        byte[] resultDigest =  sha.digest( origStr.getBytes() );
        return DatatypeConverter.printBase64Binary(resultDigest);
    }

}
