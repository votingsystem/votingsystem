package org.votingsystem.groovy.util

import org.votingsystem.util.DateUtils

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class ReportFiles {

    private static final DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    File baseDir, receiptFile, jsonFile

    public ReportFiles(DateUtils.TimePeriod timePeriod, String baseDirPath, String subPath) {
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom())
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo())
        baseDirPath ="$baseDirPath/${dateFromPathPart}_${dateToPathPart}/"
        if(subPath) baseDirPath = "${baseDirPath}${subPath}"
        baseDir = new File(baseDirPath)
        baseDir.mkdirs()
        jsonFile = new File("${baseDirPath}/balances.json")
        receiptFile = new File("${baseDirPath}/weekReportReceipt.p7s")
    }

    public File getTagReceiptFile(String tagName) {
        return new File("${baseDir.getAbsolutePath()}/tag_${tagName}.p7s")
    }

}
