package org.votingsystem.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FileUtils {

    private static Logger logger = Logger.getLogger(FileUtils.class);

    private static final int  BUFFER_SIZE = 4096;

    public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while((len = inputStream.read(buf)) > 0){
            outputStream.write(buf,0,len);
        }
        outputStream.close();
        inputStream.close();
        return outputStream.toByteArray();
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

    public static File copyStreamToFile(InputStream inputStream, File outputFile)
            throws Exception {
        OutputStream outputStream = new FileOutputStream(outputFile);
        byte[] buf =new byte[BUFFER_SIZE];
        int len;
        while((len = inputStream.read(buf)) > 0){
            outputStream.write(buf,0,len);
        }
        outputStream.close();
        inputStream.close();
        return outputFile;
    }

    public static File copyStreamToFile(InputStream inputStream) throws Exception {
        File outputFile = File.createTempFile("streamToFile", ".html");
        outputFile.deleteOnExit();
        OutputStream outputStream = new FileOutputStream(outputFile);
        byte[] buf =new byte[BUFFER_SIZE];
        int len;
        while((len = inputStream.read(buf)) > 0){
            outputStream.write(buf,0,len);
        }
        outputStream.close();
        inputStream.close();
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

    public static File getFileFromString (String content) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("getFileFromString", ".temp");
            tempFile.deleteOnExit();
            BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
            out.write(content);
            out.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return tempFile;
    }

    public static File getFileFromBytes (byte[] bytes) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("getFileFromBytes", ".temp");
            tempFile.deleteOnExit();
            copyStreamToFile(new ByteArrayInputStream(bytes),tempFile);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return tempFile;
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
        } else file.delete();//if file, then delete it
    }

    public static String getStringFromFile (File file) throws FileNotFoundException, IOException {
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

    public static void copyStringToFile(String stringToCopy, File file) throws IOException {
        FileWriter fileWriter = null;
        fileWriter = new FileWriter(file);
        fileWriter.write(stringToCopy);
        fileWriter.close();
    }

    public static List<File> findRecursively( File baseDir, final String textToFind) throws IOException {
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

    private static String buildPath(String path, String file)  {
        if (path == null || path.isEmpty()) return file;
        else return path + "/" + file;
    }

    private static void zipDir(ZipOutputStream zos, String path, File dir) throws IOException {
        if (!dir.canRead()) {
           logger.debug("Cannot read " + dir.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }
        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());
        logger.debug("Adding Directory " + path);
        for (File source : files) {
            if (source.isDirectory()) zipDir(zos, path, source);
            else zipFile(zos, path, source);
        }
        logger.debug("Leaving Directory " + path);
    }

    private static void zipFile(ZipOutputStream zos, String path, File file) throws IOException {
        if (!file.canRead()) {
            logger.debug("Cannot read " + file.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }
        logger.debug("Compressing " + file.getName());
        zos.putNextEntry(new ZipEntry(buildPath(path, file.getName())));
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4092];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1) {
            zos.write(buffer, 0, byteCount);
        }
        fis.close();
        zos.closeEntry();
    }

    /*
     * From -> http://stackoverflow.com/questions/10103861/adding-files-to-zip-file
     */
    public static void packZip(File output, List<File> sources) throws IOException {
        logger.debug("Packaging to " + output.getName());
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output));
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
        for (File source : sources)  {
            if (source.isDirectory()) zipDir(zipOut, "", source);
            else zipFile(zipOut, "", source);
        }
        zipOut.flush();
        zipOut.close();
    }

    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir,name)));
        int count = -1;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        out.close();
    }

    private static void mkdirs(File outdir,String path) {
        File d = new File(outdir, path);
        if( !d.exists() )
            d.mkdirs();
    }

    private static String dirpart(String name) {
        int s = name.lastIndexOf( File.separatorChar );
        return s == -1 ? null : name.substring( 0, s );
    }

    /***
     * from - >http://stackoverflow.com/questions/10633595/java-zip-how-to-unzip-folder
     * Extract zipfile to outdir with complete directory structure
     * @param zipfile Input .zip file
     * @param outdir Output directory
     */
    public static void unpackZip(File zipfile, File outdir) {
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipfile));
            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null) {
                name = entry.getName();
                if( entry.isDirectory() ) {
                    mkdirs(outdir,name);
                    continue;
                }
                /* this part is necessary because file entry can come before
                 * directory entry where is file located
                 * i.e.:
                 *   /foo/foo.txt
                 *   /foo/
                 */
                dir = dirpart(name);
                if( dir != null ) mkdirs(outdir,dir);
                extractFile(zin, outdir, name);
            }
            zin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
