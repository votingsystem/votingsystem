package org.votingsystem.util;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FileUtils {

    private static Logger log = Logger.getLogger(FileUtils.class.getSimpleName());

    private static final int  BUFFER_SIZE = 4096;

    public static byte[] getBytesFromStream(InputStream inputStream) throws IOException {
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

    public static String getStringFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = br.readLine()) != null) { sb.append(line);  }
        if (br != null) br.close();
        return sb.toString();
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

    public static File copyStreamToFile(InputStream inputStream, File outputFile) throws Exception {
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

    public static File copyBytesToFile(byte[] bytes, File outputFile) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(bytes);
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
            log.log(Level.SEVERE, ex.getMessage(), ex);
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
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return tempFile;
    }


    public static void deleteDir(File dir) throws IOException {
        log.info("deleteDir: " + dir.getAbsolutePath());
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
        if(file.isDirectory()) {
            log.info("deleteRecursively dir: " + file.getAbsolutePath());
            if(file.list().length == 0){ //directory is empty, then delete it
                file.delete();
            } else { //list all the directory contents
                String files[] = file.list();
                for (String temp : files) {
                    File fileDelete = new File(file, temp);
                    deleteRecursively(fileDelete);
                }
                if(file.list().length == 0){
                    file.delete();
                }
            }
        } else file.delete();
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
        FileWriter fileWriter = new FileWriter(file);
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
           log.info("Cannot read " + dir.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }
        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());
        log.info("Adding Directory " + path);
        for (File source : files) {
            if (source.isDirectory()) zipDir(zos, path, source);
            else zipFile(zos, path, source);
        }
        log.info("Leaving Directory " + path);
    }

    private static void zipFile(ZipOutputStream zos, String path, File file) throws IOException {
        if (!file.canRead()) {
            log.info("Cannot read " + file.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }
        log.info("Compressing " + file.getName());
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

    /**
     * Determine whether a file is a ZIP File.
     */
    public static boolean isZipFile(File file) throws IOException {
        if(file.isDirectory()) {
            return false;
        }
        if(!file.canRead()) {
            throw new IOException("Cannot read file "+file.getAbsolutePath());
        }
        if(file.length() < 4) {
            return false;
        }
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        int test = in.readInt();
        in.close();
        return test == 0x504b0304;
    }

    /*
     * From -> http://stackoverflow.com/questions/10103861/adding-files-to-zip-file
     */
    public static void packZip(File output, List<File> sources) throws IOException {
        log.info("Packaging to " + output.getName());
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

    private static void mkdirs(File outdir, String path) {
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

    public static void copyDirs(String sourcePath, String destPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(destPath);
            Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE, new CopyDirectory(source, target));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static class CopyDirectory extends SimpleFileVisitor<Path> {

        private Path source;
        private Path target;

        public CopyDirectory(Path source, Path target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            Files.copy(file, target.resolve(source.relativize(file)));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path directory,
                                                 BasicFileAttributes attributes) throws IOException {
            Path targetDirectory = target.resolve(source.relativize(directory));
            try {
                Files.copy(directory, targetDirectory);
            } catch (FileAlreadyExistsException e) {
                if (!Files.isDirectory(targetDirectory)) {
                    throw e;
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
