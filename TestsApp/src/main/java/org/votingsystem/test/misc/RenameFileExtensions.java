package org.votingsystem.test.misc;


import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class RenameFileExtensions {

    private static Logger log = Logger.getLogger(RenameFileExtensions.class.getSimpleName());

    private static final String basePath = "/home/jgzornoza/temp/cryptocurrency_webapp";
    private static final String old_extension = ".jsp";
    private static final String new_extension = ".vsp";

    public static void main(String[] args) {
        File baseDir =  new File(basePath);
        RenameFileExtensions rf = new RenameFileExtensions();
        try {
            rf.renameRecursively(baseDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renameRecursively(File file) throws IOException {
        log.info("renameRecursively file: " + file.getAbsolutePath() + " - new_extension: " + new_extension);
        if(file.isDirectory()) {
            if(file.list().length != 0){
                String files[] = file.list();
                for (String temp : files) {
                    File fileToRename = new File(file, temp);
                    renameRecursively(fileToRename);
                }
            } else log.info("empty dir");
        } else {
            int index = file.getName().indexOf(".");
            if(index > 0) {
                String fileExtension = file.getName().substring(index);
                log.info("old_extension: " + fileExtension);
                if(old_extension.equals(fileExtension)) {
                    String newName = file.getParent() + "/" + file.getName().substring(0, index) + new_extension;
                    File newFile = new File(newName);
                    file.renameTo(newFile);
                    log.info("renameRecursively newName: " + newFile.getAbsolutePath());
                }
            }
        }
    }

}
