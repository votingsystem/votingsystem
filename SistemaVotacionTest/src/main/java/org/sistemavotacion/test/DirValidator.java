package org.sistemavotacion.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.herramientavalidacion.modelo.SignedFile;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DirValidator {
    
    private static Logger logger = LoggerFactory.getLogger(DirValidator.class);  
    
    List<File> fileList = new ArrayList<File>();
    
    private void validateDirFiles (File dir) throws Exception {
        logger.debug("validateDirFiles - dir: " + dir.getAbsolutePath());
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {return true;}
        };
       
        File[] archivos = dir.listFiles(fileFilter);
        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.isDirectory()) validateDirFiles(archivo);
                else fileList.add(archivo);
            }
            
        }
        for(File file:fileList) {
            InputStream inputStream = new FileInputStream(file);
            //byte[] fileBytes = FileUtils.getBytesFromInputStream(inputStream);

            byte[] fileBytes = FileUtils.getBytesFromFile(file);
            //System.out.println(new String(fileBytes));

            SignedFile signedFile = new SignedFile(fileBytes, "votes.p7m");
            logger.debug("file:" + file.getAbsolutePath() + " - isValidSignature: " + signedFile.isValidSignature());
        }
    }
    
    public static void main(String[] args) throws Exception {
        Contexto.INSTANCE.init(null);
        
        File dir = new File(ContextoPruebas.DEFAULTS.ERROR_DIR);
        DirValidator validateDirFiles = new DirValidator();
        validateDirFiles.validateDirFiles(dir);
    }
    
}
