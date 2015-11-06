package org.votingsystem.web.currency.util;

import org.votingsystem.util.TimePeriod;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReportFiles {

    private File baseDir;
    private File receiptFile;
    private File jsonFile;

    public ReportFiles() {}

    public ReportFiles(TimePeriod timePeriod, String baseDirPath, String subPath) {
        String reportsBasePath = baseDirPath + "/backup/weekReports";
        DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom());
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo());
        reportsBasePath = reportsBasePath + "/" + dateFromPathPart + "_" +dateToPathPart + "/";
        if(subPath != null) reportsBasePath = reportsBasePath + subPath;
        baseDir = new File(reportsBasePath);
        baseDir.mkdirs();
        jsonFile = new File(reportsBasePath + "/balances.json");
        receiptFile = new File(reportsBasePath + "/weekReportReceipt.p7s");
    }

    public static ReportFiles CURRENCY_PERIOD(TimePeriod timePeriod, String baseDirPath) {
        ReportFiles result = new ReportFiles();
        DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom());
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo());
        String reportsBasePath = baseDirPath + "/backup/currency/" + dateFromPathPart + "_" + dateToPathPart;
        result.baseDir = new File(reportsBasePath);
        result.baseDir.mkdirs();
        result.jsonFile = new File(reportsBasePath + "/currencyMetaInf.json");
        result.receiptFile = new File(reportsBasePath + "/currencyReportReceipt.p7s");
        return result;
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