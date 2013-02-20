package org.sistemavotacion.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* */
public class FileUtils {
	
    public static byte[] getBytesFromInputStream(InputStream entrada) throws IOException {
    	ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buf =new byte[1024];
        int len;
        while((len = entrada.read(buf)) > 0){
            salida.write(buf,0,len);
        }
        salida.close();
        entrada.close();
        return salida.toByteArray();
    }
	
	public static byte[] getBytesFromFile(File file) throws IOException { 
	    byte[] b = new byte[(int) file.length()];
	    FileInputStream fs = new FileInputStream(file);
	    fs.read(b);
	    fs.close();
	    return b;
	}
	
	public static void copy(File src, File dst) throws IOException { 
		InputStream in = new FileInputStream(src); 
		OutputStream out = new FileOutputStream(dst); 
		byte[] buf = new byte[(int) src.length()]; 
		in.read(buf);
		out.write(buf);
		in.close(); 
		out.close(); 
	} 
    
	public static File copyStreamToFile(InputStream entrada, File outputFile)
			throws Exception {
		OutputStream salida = new FileOutputStream(outputFile);
		byte[] buf =new byte[1024];
		int len;
		while((len = entrada.read(buf)) > 0){
		salida.write(buf,0,len);
		}
		salida.close();
		entrada.close();
		return outputFile;
	}
	
	public static byte[] concat(byte[] first, byte[] second) {
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static <T> T[] concatAll(T[] first, T[]... rest) {
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
		System.arraycopy(array, 0, result, offset, array.length);
		offset += array.length;
		}
		return result;
	}
    
}
