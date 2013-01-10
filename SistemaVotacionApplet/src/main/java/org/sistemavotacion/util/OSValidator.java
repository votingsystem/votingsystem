package org.sistemavotacion.util;

import org.sistemavotacion.Contexto;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class OSValidator {

    private static Logger logger = LoggerFactory.getLogger(OSValidator.class);

    public static String LIB_PATH_BASE_DIR =  "lib/";
    public static String LINUX_LIB         =  "libpkcs11wrapper.so";
    public static String MACOSX_LIB        =  "libpkcs11wrapper.jnilib";
    public static String SOLARIS_LIB       =  "libpkcs11wrapper.so";
    public static String WIN32_LIB         =  "pkcs11wrapper.dll";
    public static String WIN64_LIB         =  "PKCS11Wrapper.dll";
    public static String LINUX_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/linux/release/";
    public static String LINUX_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/linux/release/libpkcs11wrapper.so";
    public static String LINUX_x64_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/linux_x64/release/";
    public static String LINUX_x64_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/linux_x64/release/libpkcs11wrapper.so";
    public static String MACOSX_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/macosx_universal/release/";
    public static String MACOSX_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/macosx_universal/release/libpkcs11wrapper.jnilib";
    public static String SOLARIS_SPARC_SYSTEM_IAIK_CLASSPATH =  "iaik_native/platforms/solaris_sparc/release/";
    public static String SOLARIS_SPARC_SYSTEM_IAIK_LIB_PATH =  "iaik_native/platforms/solaris_sparc/release/libpkcs11wrapper.so";
    public static String SOLARIS_SPARCV9_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/solaris_sparcv9/release/";
    public static String SOLARIS_SPARCV9_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/solaris_sparcv9/release/libpkcs11wrapper.so";
    public static String SOLARIS_SPARCV9_JDK11_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/solaris_sparcv9/release/_jdk11";
    public static String SOLARIS_SPARCV9_JDK11_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/solaris_sparcv9/release/_jdk11/libpkcs11wrapper.so";
    public static String WIN32_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/win32/release/";
    public static String WIN32_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/win32/release/pkcs11wrapper.dll";
    public static String WIN32_JDK11_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/win32/release/_JDK11";
    public static String WIN32_JDK11_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/win32/release/_JDK11/pkcs11wrapper.dll";
    public static String WIN64_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/win64/release/";
    public static String WIN64_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/win64/release/PKCS11Wrapper.dll";
    public static String WIN64_JDK11_IAIK_SYSTEM_CLASSPATH =  "iaik_native/platforms/win64/release/_JDK11";
    public static String WIN64_JDK11_IAIK_SYSTEM_LIB_PATH =  "iaik_native/platforms/win64/release/_JDK11/PKCS11Wrapper.dll";

    public static boolean isWindows(){
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf( "win" ) >= 0);//windows
    }

    public static boolean isMac(){
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf( "mac" ) >= 0);//Mac
    }

    public static boolean isUnix(){
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);//linux or unix
    }

    
    public static void initClassPath () throws Exception {
        FileUtils.deleteDir(new File(FileUtils.APPDIR + LIB_PATH_BASE_DIR));
        String dirAddedToSystemClasspath = null;
        String pkcs11LibraryWrapperPath = null;
        String libName = LINUX_LIB;
        if (isWindows()) {
            if (System.getProperty("os.arch").indexOf("64") >= 0) {
            	dirAddedToSystemClasspath = WIN64_IAIK_SYSTEM_CLASSPATH;
            	pkcs11LibraryWrapperPath = WIN64_IAIK_SYSTEM_LIB_PATH;
                libName = WIN64_LIB;
            } else {
            	dirAddedToSystemClasspath = WIN32_IAIK_SYSTEM_CLASSPATH;
            	pkcs11LibraryWrapperPath = WIN32_IAIK_SYSTEM_LIB_PATH;
                libName = WIN32_LIB;
            } 
        }
        else if(isUnix()) {
            //pkcs11LibraryPath = "/usr/local/lib/opensc-pkcs11.so";
            //pkcs11LibraryPath = "/lib/opensc-pkcs11.so";
            libName = LINUX_LIB;
            if (System.getProperty("os.arch").indexOf("64") >= 0) {
            	dirAddedToSystemClasspath = LINUX_x64_IAIK_SYSTEM_CLASSPATH;
            	pkcs11LibraryWrapperPath = LINUX_x64_IAIK_SYSTEM_LIB_PATH;
                
             }
            else {
            	dirAddedToSystemClasspath = LINUX_IAIK_SYSTEM_CLASSPATH;
            	pkcs11LibraryWrapperPath = LINUX_IAIK_SYSTEM_LIB_PATH;
            } 
        }
        else if(isMac()) {
            libName = MACOSX_LIB;
            dirAddedToSystemClasspath = MACOSX_IAIK_SYSTEM_CLASSPATH;
            pkcs11LibraryWrapperPath = MACOSX_IAIK_SYSTEM_LIB_PATH;
        }
        File directorioBaseDeAplicacion = Contexto.getApplicactionBaseDirFile();
        File directorioDestinolibrerias = new File(FileUtils.APPDIR + LIB_PATH_BASE_DIR +
                dirAddedToSystemClasspath + UUID.randomUUID().toString() + File.separator);
        directorioDestinolibrerias.mkdirs();
        /*if (directorioBaseDeAplicacion.isFile()) { //se esta ejecutando desde jar
            JarFile jarFile = new JarFile(directorioBaseDeAplicacion);
            ZipEntry zipEntry = jarFile.getEntry(pkcs11LibraryWrapperPath);
        	InputStream in = new BufferedInputStream(jarFile.getInputStream(zipEntry));
        	File outputFile = new File(FileUtils.APPDIR + zipEntry.getName());
        	outputFile.createNewFile();
        	FileUtils.copyStreamToFile(in, outputFile);
        } else*/ 
        if (directorioBaseDeAplicacion.isDirectory()){//se esta ejecutando desde directorio classes
            File archivoLibreria = new File(Contexto.getApplicactionBaseDir() + 
            		dirAddedToSystemClasspath).listFiles()[0];
            File copiaArchivoLibreria = new File(directorioDestinolibrerias.
                    getAbsolutePath() + File.separator + archivoLibreria.getName());
            FileUtils.copy(archivoLibreria, copiaArchivoLibreria);
        }  else {//JWS
        	File outputFile = new File(directorioDestinolibrerias.
                        getAbsolutePath()  + File.separator+ libName);
        	outputFile.createNewFile();
                InputStream in =  Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(pkcs11LibraryWrapperPath);
                if (in == null) {
                    throw new FileNotFoundException(
                            "resource " + pkcs11LibraryWrapperPath);
                }
                FileUtils.copyStreamToFile(in, outputFile);
        }
        FileUtils.addDirToSystemClasspath(directorioDestinolibrerias.getAbsolutePath());
    }

    
    public static String getPKCS11ModulePath () throws Exception {
        String pkcs11LibraryPath = null;
        if (isWindows()) {
            if (System.getProperty("os.arch").indexOf("64") >= 0) {
            	pkcs11LibraryPath = "C:\\Windows\\SysWOW64\\UsrPkcs11.dll";
            } else {
                pkcs11LibraryPath = "C:\\WINDOWS\\system32\\UsrPkcs11.dll";
            }
        }
        else if(isUnix()) {
            pkcs11LibraryPath = getPKCS11LibPath();
            //pkcs11LibraryPath = "/usr/local/lib/opensc-pkcs11.so";
            //pkcs11LibraryPath = "/lib/opensc-pkcs11.so";
        }
        else if(isMac()) {
            pkcs11LibraryPath = "/Library/OpenSC/lib/opensc-pkcs11.so";
        }
        return pkcs11LibraryPath;
    }

    
    public static String getPKCS11LibPath() {
        String[] systemLibraryList = { "/usr/lib/opensc-pkcs11.so",
            "/usr/local/lib/opensc-pkcs11.so",
            "/lib/opensc-pkcs11.so", "C:\\WINDOWS\\system32\\UsrPkcs11.dll",
            "C:\\Windows\\SysWOW64\\UsrPkcs11.dll"};

        for (String file : systemLibraryList) {
            File f = new File(file);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }
}
