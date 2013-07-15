package org.sistemavotacion.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class FileUtils {

    private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static byte[] getBytesFromFile(File file) throws IOException {
        byte[] b = new byte[(int) file.length()];
        FileInputStream fs = new FileInputStream(file);
        fs.read(b);
        fs.close();
        return b;
    }
    
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

   public static File copyFileToFile(File inputFile, File outputFile)
         throws Exception {
       FileInputStream fs = new FileInputStream(inputFile);
       return copyStreamToFile(fs, outputFile);
    }

   public static File copyStringToFile(String string, File file) 
           throws FileNotFoundException {
       PrintWriter out = new PrintWriter(file);
       out.write(string);
       out.close();
       return file;
   }
   
    public static File copyStreamToFile(InputStream entrada, File outputFile)
         throws Exception {
        OutputStream salida = new FileOutputStream(outputFile);
        byte[] buf =new byte[4096];
        int len;
        while((len = entrada.read(buf)) > 0){
            salida.write(buf,0,len);
        }
        salida.close();
        entrada.close();
        return outputFile;
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

     public static File getFileFromString (String contenido) {
        File temp = null;
        try {
             temp = new File("recibo");
             temp.deleteOnExit();
             BufferedWriter out = new BufferedWriter(new FileWriter(temp));
             out.write(contenido);
             out.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return temp;
     }

    public static void save(String content, String filePath,
            String fileExtension) {
        String rutaCompletaArchivo = filePath;
        if (!(fileExtension == null || fileExtension.equals("")))
            rutaCompletaArchivo = filePath + "." + fileExtension;
        try {
            File archivoSalida = new File(rutaCompletaArchivo);
            FileWriter out = new FileWriter(archivoSalida);
            out.write(content);
            out.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public static void deleteDir(File dir) throws IOException {
        logger.info("deleteDir: " + dir.getAbsolutePath());
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                deleteDir(file);
            }
        } 
        dir.delete();
    } 

    public static void addDirToSystemClasspath(String s) throws IOException {
        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            //
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[])field.get(null);
            for (int i = 0; i < paths.length; i++) {
                    if (s.equals(paths[i])) {
                            return;
                    }
            }
            String[] tmp = new String[paths.length+1];
            System.arraycopy(paths,0,tmp,0,paths.length);
            tmp[paths.length] = s;
            field.set(null,tmp);
            System.setProperty("java.library.path",
                    System.getProperty("java.library.path") + File.pathSeparator + s);
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set library path");
        }
    }
    
    public static String getStringFromFile (File file) 
            throws FileNotFoundException, IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        }
        finally {
            stream.close();
        }
     }

    /*
     * http://www.mkyong.com/java/how-to-delete-directory-in-java/
     */
    public static void deleteRecursively(File file) throws IOException {
    	if(file.isDirectory()){
            //directory is empty, then delete it
            if(file.list().length==0){
               file.delete();
               logger.debug("Directory is deleted : " 
                                             + file.getAbsolutePath());
            }else{
               //list all the directory contents
               String files[] = file.list();
               for (String temp : files) {
                  //construct the file structure
                  File fileDelete = new File(file, temp);
                  //recursive delete
                 deleteRecursively(fileDelete);
               }
               //check the directory again, if empty then delete it
               if(file.list().length==0){
                 file.delete();
                 logger.debug("Directory is deleted : " 
                                              + file.getAbsolutePath());
               }
            }
    	}else{
            file.delete();//if file, then delete it
    	}
    }

    public static List<File> findRecursively(
            File baseDir, final String textToFind) throws IOException {
    	List<File> result = new ArrayList<File>();
        if(baseDir.isDirectory()){
            File[] matchingFiles = baseDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains(textToFind);
                }
            });
            result.addAll(Arrays.asList(matchingFiles));
            File[] dirFiles = baseDir.listFiles();
            for(File file: dirFiles) {
                if(file.isDirectory()) {
                    List<File> subdirMatchingFiles = findRecursively(file, textToFind);
                    result.addAll(subdirMatchingFiles);
                }
            }
    	} 
        return result; 
    }

}