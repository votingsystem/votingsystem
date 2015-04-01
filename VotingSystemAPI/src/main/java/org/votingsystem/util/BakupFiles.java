package org.votingsystem.util;


import org.votingsystem.model.EventVS;
import org.votingsystem.throwable.ExceptionVS;

import java.io.File;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BakupFiles {

    private File metaInfFile;
    private File reportFile;
    private File zipResult;
    private File baseDir;
    private File filesDir;

    public BakupFiles(EventVS eventVS, TypeVS type, String errorsBasePath, String backupBasePath) throws ExceptionVS {
        new File(errorsBasePath).mkdirs();
        new File(backupBasePath).mkdirs();
        String datePartPath = DateUtils.getDateStr(eventVS.getDateFinish(), "yyyy_MM_dd");
        String baseDirPath = backupBasePath + "/" + datePartPath + "/EventVS_" + eventVS.getId();
        baseDir = new File(baseDirPath);
        baseDir.mkdirs();
        String filesDirPath = baseDirPath + "/" + type.toString();
        filesDir = new File(filesDirPath);
        switch(type) {
            case REPRESENTATIVE_DATA:
                metaInfFile = new File(filesDirPath + "/meta.inf");
                reportFile = new File(baseDirPath + "/" + type.toString() + "_REPORTS.csv");
                zipResult = new File(baseDirPath + "/" + type.toString() + "_EventVS_" + eventVS.getId() + ".zip");
                break;
            case VOTING_EVENT:
                metaInfFile = new File(baseDirPath + "/meta.inf");
                zipResult = new File(baseDirPath + "/" + type.toString() + "_" + eventVS.getId() + ".zip");
                break;
            default: throw new ExceptionVS("unprocessed type:" + type);
        }
    }

    public File getMetaInfFile() {
        return metaInfFile;
    }

    public File getReportFile() {
        return reportFile;
    }

    public File getZipResult() {
        return zipResult;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public File getFilesDir() {
        return filesDir;
    }
}
