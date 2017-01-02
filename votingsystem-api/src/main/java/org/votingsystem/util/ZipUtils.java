package org.votingsystem.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
 */
public class ZipUtils {

    private static Logger log = Logger.getLogger(ZipUtils.class.getName());

    private List<String> fileList;
    private String sourceFolder;

    /**
     * @param sourceFolder input dir location
     */
    public ZipUtils(String sourceFolder) {
        this.sourceFolder = sourceFolder;
        fileList = new ArrayList<String>();
        generateFileList(new File(sourceFolder));
    }

    /**
     * @param sourceFolder input dir
     */
    public ZipUtils(File sourceFolder) {
        this.sourceFolder = sourceFolder.getAbsolutePath();
        fileList = new ArrayList<String>();
        generateFileList(sourceFolder);
    }

    /**
     * @param zipFile output ZIP file
     */
    public void zipIt(File zipFile) throws IOException {
        log.log(Level.FINE, "Output to Zip : " + zipFile.getAbsolutePath());
        zipIt(new FileOutputStream(zipFile));
    }

    private void zipIt(FileOutputStream fos) throws IOException {
        byte[] buffer = new byte[4096];
        ZipOutputStream zos = new ZipOutputStream(fos);
        for(String file : fileList){
            log.log(Level.FINE, "File Added : " + file);
            ZipEntry ze= new ZipEntry(file);
            zos.putNextEntry(ze);
            FileInputStream in = new FileInputStream(sourceFolder + File.separator + file);
            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            in.close();
        }
        zos.closeEntry();
        //remember close it
        zos.close();
        log.log(Level.FINE, "Done");
    }

    /**
     * @param zipFile output ZIP file location
     */
    public void zipIt(String zipFile) throws IOException {
        log.log(Level.FINE, "Output to Zip : " + zipFile);
        zipIt(new FileOutputStream(zipFile));
    }

    /**
     * Traverse a directory and get all files,
     * and add the file into fileList
     * @param node file or directory
     */
    public void generateFileList(File node){
        //add file only
        if(node.isFile()){
            fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
        }
        if(node.isDirectory()){
            String[] subNote = node.list();
            for(String filename : subNote){
                generateFileList(new File(node, filename));
            }
        }
    }

    /**
     * Format the file path for zip
     * @param file file path
     * @return Formatted file path
     */
    private String generateZipEntry(String file){
        return file.substring(sourceFolder.length()+1, file.length());
    }

}
