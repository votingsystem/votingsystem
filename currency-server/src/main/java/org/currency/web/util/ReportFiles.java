package org.currency.web.util;

import org.votingsystem.util.Interval;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReportFiles {

    private File baseDir;
    private File receiptFile;
    private File reportFile;

    public ReportFiles() {}

    public ReportFiles(Interval timePeriod, String baseDirPath, String subPath) {
        String reportsBasePath = baseDirPath + "/backup/weekReports";
        DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom());
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo());
        reportsBasePath = reportsBasePath + "/" + dateFromPathPart + "_" +dateToPathPart + "/";
        if(subPath != null) reportsBasePath = reportsBasePath + subPath;
        baseDir = new File(reportsBasePath);
        baseDir.mkdirs();
        reportFile = new File(reportsBasePath + "/balances.xml");
        receiptFile = new File(reportsBasePath + "/weekReportReceipt.p7s");
    }

    public static ReportFiles CURRENCY_PERIOD(Interval timePeriod, String baseDirPath) {
        ReportFiles result = new ReportFiles();
        DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom());
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo());
        String reportsBasePath = baseDirPath + "/backup/currency/" + dateFromPathPart + "_" + dateToPathPart;
        result.baseDir = new File(reportsBasePath);
        result.baseDir.mkdirs();
        result.reportFile = new File(reportsBasePath + "/currencyMetaInf.xml");
        result.receiptFile = new File(reportsBasePath + "/currencyReportReceipt.p7s");
        return result;
    }

    public File getTagReceiptFile(String tagName) {
        return new File(getBaseDir().getAbsolutePath() + "/tag_" + tagName + ".xml");
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

    public File getReportFile() {
        return reportFile;
    }

    public void setReportFile(File reportFile) {
        this.reportFile = reportFile;
    }
}