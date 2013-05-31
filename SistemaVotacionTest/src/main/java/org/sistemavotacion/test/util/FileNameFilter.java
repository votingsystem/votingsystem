package org.sistemavotacion.test.util;

import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.sistemavotacion.test.ContextoPruebas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class FileNameFilter implements IOFileFilter {
    
    private static Logger logger = LoggerFactory.getLogger(FileNameFilter.class);

    private String fileName = null;
    
    public FileNameFilter(String fileName) {
        this.fileName = fileName;
    }


    @Override
    public boolean accept(File file) {
        if(file.getName().contains(fileName)) return true;
        else return false;
    }

    @Override
    public boolean accept(File file, String string) {
        if(string.contains(fileName)) return true;
        else return false;
    }
    
    public static Collection<File> getFilesFromDirectoryTree(File file, String fileName) {
        Collection<File> archivos = FileUtils.listFiles(file,
            new FileNameFilter(fileName), TrueFileFilter.INSTANCE);
        return archivos;
    }
    
    public static void main(String[] args) {
        Collection<File> solicitudes = FileNameFilter.getFilesFromDirectoryTree(
                new File(ContextoPruebas.APPDIR), ContextoPruebas.ANULACION_FILE);
        logger.debug("Encontradas '" + solicitudes.size() + "' solicitudes");
    }
    
}
