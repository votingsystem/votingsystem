package org.votingsystem.web.currency.util;

import org.votingsystem.util.TimePeriod;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReportFiles {

    private static final DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    private File baseDir;
    private File receiptFile;
    private File jsonFile;

    public ReportFiles(TimePeriod timePeriod, String baseDirPath, String subPath) {
        String reportsBasePath = baseDirPath + "/backup/weekReports";
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom());
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo());
        reportsBasePath = reportsBasePath + "/" + dateFromPathPart + "_" +dateToPathPart + "/";
        if(subPath != null) reportsBasePath = reportsBasePath + subPath;;
        setBaseDir(new File(reportsBasePath));
        getBaseDir().mkdirs();
        setJsonFile(new File(reportsBasePath + "/balances.json"));
        setReceiptFile(new File(reportsBasePath + "/weekReportReceipt.p7s"));
    }

    public File getTagReceiptFile(String tagName) {
        return new File(getBaseDir().getAbsolutePath() + "/tag_" + tagName + ".p7s");
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public File getReceiptFile() {
        return receiptFile;
    }

    public void setReceiptFile(File receiptFile) {
        this.receiptFile = receiptFile;
    }

    public File getJsonFile() {
        return jsonFile;
    }

    public void setJsonFile(File jsonFile) {
        this.jsonFile = jsonFile;
    }
}