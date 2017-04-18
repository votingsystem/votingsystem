package org.votingsystem.util;


import org.votingsystem.model.voting.Election;
import org.votingsystem.throwable.ExceptionBase;

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

    public BakupFiles(Election election, String serverBasePath) throws ExceptionBase {
        String backupBasePath = serverBasePath + "/backup";
        new File(serverBasePath + "/error").mkdirs();
        new File(backupBasePath).mkdirs();
        String datePartPath = DateUtils.getDateStr(election.getDateFinish(), "yyyy_MM_dd");
        String baseDirPath = backupBasePath + "/" + datePartPath + "/Election_" + election.getUUID();
        baseDir = new File(baseDirPath);
        baseDir.mkdirs();
        filesDir = new File(baseDirPath + "/files");
        metaInfFile = new File(baseDirPath + "/meta.inf");
        zipResult = new File(baseDirPath + "/Election_" + election.getUUID() + ".zip");
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
